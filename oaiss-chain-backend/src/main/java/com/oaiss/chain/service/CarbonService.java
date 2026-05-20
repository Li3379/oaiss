package com.oaiss.chain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.dto.CarbonReportRequest;
import com.oaiss.chain.dto.CarbonReportResponse;
import com.oaiss.chain.dto.ReviewRequest;
import com.oaiss.chain.entity.CarbonReport;
import com.oaiss.chain.entity.Enterprise;
import com.oaiss.chain.entity.User;
import com.oaiss.chain.enums.ReportStatusEnum;
import com.oaiss.chain.exception.CarbonException;
import com.oaiss.chain.repository.CarbonReportRepository;
import com.oaiss.chain.repository.EnterpriseRepository;
import com.oaiss.chain.repository.UserRepository;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 碳核算服务
 * 
 * @author OAISS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarbonService {

    private final CarbonReportRepository carbonReportRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final CreditScoreService creditScoreService;
    private final EmissionRatingService emissionRatingService;
    private final BlockchainServicePort blockchainService;

    /**
     * 创建碳报告（草稿）
     */
    @Transactional
    public CarbonReportResponse createReport(JwtUserDetails currentUser, 
            CarbonReportRequest request) {
        Enterprise enterprise = enterpriseRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> CarbonException.submitFailed("未找到关联企业信息"));

        // Parse emission data JSON to calculate totals
        BigDecimal[] totals = parseEmissionTotals(request.getEmissionData());
        BigDecimal scope1 = totals[0];
        BigDecimal scope2 = totals[1];
        BigDecimal scope3 = totals[2];
        BigDecimal totalEmission = totals[3];

        CarbonReport report = CarbonReport.builder()
                .reportNo(CommonUtils.generateReportId())
                .enterpriseId(enterprise.getId())
                .submitterId(currentUser.getUserId())
                .accountingPeriod(request.getAccountingPeriod())
                .title(CommonUtils.sanitizeHtml(CommonUtils.sanitizeInput(request.getTitle())))
                .reportType(request.getReportType())
                .emissionData(request.getEmissionData())
                .totalEmission(totalEmission)
                .scope1Emission(scope1)
                .scope2Emission(scope2)
                .scope3Emission(scope3)
                .calculationMethod(request.getCalculationMethod())
                .status(ReportStatusEnum.DRAFT.getCode())
                .signatureData(request.getSignatureData())
                .attachments(request.getAttachments())
                .build();

        report = carbonReportRepository.save(report);
        log.info("Carbon report created: {} by user {}", report.getReportNo(), currentUser.getUsername());

        return toResponse(report);
    }

    /**
     * 提交碳报告
     */
    @Transactional
    public CarbonReportResponse submitReport(JwtUserDetails currentUser, Long reportId) {
        CarbonReport report = carbonReportRepository.findById(reportId)
                .orElseThrow(() -> CarbonException.reportNotFound(reportId));

        // 验证报告所有权
        Enterprise enterprise = enterpriseRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> CarbonException.submitFailed("未找到关联企业信息"));
        if (!report.getEnterpriseId().equals(enterprise.getId())) {
            throw CarbonException.submitFailed("无权操作此报告");
        }

        // 验证状态
        ReportStatusEnum status = ReportStatusEnum.fromCode(report.getStatus());
        if (!status.isSubmittable()) {
            throw CarbonException.reportAlreadySubmitted(reportId);
        }

        // 计算碳排放量
        calculateEmissions(report);

        // 更新状态
        report.setStatus(ReportStatusEnum.SUBMITTED.getCode());
        report = carbonReportRepository.save(report);

        log.info("Carbon report submitted: {}", report.getReportNo());
        return toResponse(report);
    }

    /**
     * 审核碳报告
     */
    @Transactional
    public CarbonReportResponse reviewReport(JwtUserDetails currentUser, ReviewRequest request) {
        CarbonReport report = carbonReportRepository.findById(request.getReportId())
                .orElseThrow(() -> CarbonException.reportNotFound(request.getReportId()));

        // 验证状态
        ReportStatusEnum status = ReportStatusEnum.fromCode(report.getStatus());
        if (!status.isReviewable()) {
            throw CarbonException.submitFailed("报告状态不允许审核");
        }

        // 更新审核信息
        report.setReviewerId(currentUser.getUserId());
        report.setReviewComment(CommonUtils.sanitizeHtml(CommonUtils.sanitizeInput(request.getReviewComment())));
        report.setReviewedAt(LocalDateTime.now());
        report.setStatus(request.getReviewResult()); // 3-通过, 4-拒绝

        // Cascading side effects for approved reports (D-01/D-02/D-03/D-05)
        if (request.getReviewResult() == ReportStatusEnum.APPROVED.getCode()) {
            Long enterpriseId = report.getEnterpriseId();

            // 1. Credit score bonus (+5 points)
            creditScoreService.addBonusPoints(enterpriseId, 5,
                "碳报告审核通过奖励", currentUser.getUserId());

            // 2. Emission rating calculation
            // Extract year from accountingPeriod (e.g., "2024-Q1" -> "2024", "2024" -> "2024")
            String ratingYear = report.getAccountingPeriod();
            if (ratingYear == null || ratingYear.isEmpty()) {
                ratingYear = String.valueOf(LocalDateTime.now().getYear());
            } else if (ratingYear.length() > 4) {
                ratingYear = ratingYear.substring(0, 4);
            }
            emissionRatingService.rateEnterprise(enterpriseId,
                ratingYear,
                report.getTotalEmission(),
                null,
                currentUser.getUserId());

            // 3. Blockchain mock record
            String txHash = blockchainService.commitReportToChain(
                report.getId(), report.getEmissionData());
            report.setBlockchainTxHash(txHash);
            report.setOnChainAt(LocalDateTime.now());

            // 4. Transition to ON_CHAIN(5) per D-05
            report.setStatus(ReportStatusEnum.ON_CHAIN.getCode());
        }

        report = carbonReportRepository.save(report);

        log.info("Carbon report reviewed: {} -> status={}", report.getReportNo(), request.getReviewResult());
        return toResponse(report);
    }

    /**
     * 获取报告详情
     */
    @Transactional(readOnly = true)
    public CarbonReportResponse getReport(Long reportId) {
        CarbonReport report = carbonReportRepository.findById(reportId)
                .orElseThrow(() -> CarbonException.reportNotFound(reportId));
        return toResponse(report);
    }

    /**
     * 分页查询报告
     */
    public Page<CarbonReportResponse> listReports(Long enterpriseId, Integer status, 
            String keyword, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CarbonReport> reports = carbonReportRepository.search(enterpriseId, status, keyword, pageable);
        return reports.map(this::toResponse);
    }

    /**
     * 获取企业的报告列表
     */
    public Page<CarbonReportResponse> listMyReports(JwtUserDetails currentUser, 
            Integer status, Integer page, Integer size) {
        Enterprise enterprise = enterpriseRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> CarbonException.submitFailed("未找到关联企业信息"));
        
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CarbonReport> reports;
        if (status != null) {
            reports = carbonReportRepository.findByEnterpriseIdAndStatusAndDeletedFalse(
                    enterprise.getId(), status, pageable);
        } else {
            reports = carbonReportRepository.findByEnterpriseIdAndDeletedFalse(
                    enterprise.getId(), pageable);
        }
        return reports.map(this::toResponse);
    }

    /**
     * 删除碳报告（仅草稿可删除）
     */
    @Transactional
    public void deleteReport(JwtUserDetails currentUser, Long reportId) {
        CarbonReport report = carbonReportRepository.findById(reportId)
                .orElseThrow(() -> CarbonException.reportNotFound(reportId));

        if (ReportStatusEnum.fromCode(report.getStatus()) != ReportStatusEnum.DRAFT) {
            throw CarbonException.reportAlreadySubmitted(reportId);
        }

        report.setDeleted(true);
        carbonReportRepository.save(report);
        log.info("Carbon report deleted: {}", report.getReportNo());
    }

    // ==================== 私有方法 ====================

    /**
     * 计算碳排放量
     * 根据排放因子和活动数据计算
     */
    private void calculateEmissions(CarbonReport report) {
        BigDecimal[] totals = parseEmissionTotals(report.getEmissionData());
        report.setScope1Emission(totals[0]);
        report.setScope2Emission(totals[1]);
        report.setScope3Emission(totals[2]);
        report.setTotalEmission(totals[3]);
    }

    /**
     * 解析排放数据JSON，计算各范围排放量
     * @return [scope1, scope2, scope3, total]
     */
    private BigDecimal[] parseEmissionTotals(String emissionData) {
        BigDecimal scope1 = BigDecimal.ZERO;
        BigDecimal scope2 = BigDecimal.ZERO;
        BigDecimal scope3 = BigDecimal.ZERO;
        try {
            if (emissionData != null) {
                JsonNode data = objectMapper.readTree(emissionData);

                // 范围1: 直接排放
                if (data.has("scope1")) {
                    for (JsonNode item : data.get("scope1")) {
                        BigDecimal activity = new BigDecimal(item.get("activity_data").asText("0"));
                        BigDecimal factor = new BigDecimal(item.get("emission_factor").asText("0"));
                        scope1 = scope1.add(activity.multiply(factor));
                    }
                }

                // 范围2: 间接排放（电力等）
                if (data.has("scope2")) {
                    for (JsonNode item : data.get("scope2")) {
                        BigDecimal activity = new BigDecimal(item.get("activity_data").asText("0"));
                        BigDecimal factor = new BigDecimal(item.get("emission_factor").asText("0"));
                        scope2 = scope2.add(activity.multiply(factor));
                    }
                }

                // 范围3: 其他间接排放
                if (data.has("scope3")) {
                    for (JsonNode item : data.get("scope3")) {
                        BigDecimal activity = new BigDecimal(item.get("activity_data").asText("0"));
                        BigDecimal factor = new BigDecimal(item.get("emission_factor").asText("0"));
                        scope3 = scope3.add(activity.multiply(factor));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse emission data JSON for total calculation: {}", e.getMessage());
        }
        return new BigDecimal[]{scope1, scope2, scope3, scope1.add(scope2).add(scope3)};
    }

    /**
     * Entity转Response
     */
    private CarbonReportResponse toResponse(CarbonReport report) {
        String enterpriseName = null;
        String reviewerName = null;

        if (report.getEnterpriseId() != null) {
            enterpriseName = enterpriseRepository.findById(report.getEnterpriseId())
                    .map(Enterprise::getEnterpriseName).orElse(null);
        }
        if (report.getReviewerId() != null) {
            reviewerName = userRepository.findById(report.getReviewerId())
                    .map(User::getRealName).orElse(null);
        }

        String statusText = null;
        if (report.getStatus() != null) {
            statusText = ReportStatusEnum.fromCode(report.getStatus()).getDescription();
        }

        return CarbonReportResponse.builder()
                .id(report.getId())
                .reportNo(report.getReportNo())
                .enterpriseId(report.getEnterpriseId())
                .enterpriseName(enterpriseName)
                .accountingPeriod(report.getAccountingPeriod())
                .title(report.getTitle())
                .reportType(report.getReportType())
                .emissionData(report.getEmissionData())
                .totalEmission(report.getTotalEmission())
                .scope1Emission(report.getScope1Emission())
                .scope2Emission(report.getScope2Emission())
                .scope3Emission(report.getScope3Emission())
                .calculationMethod(report.getCalculationMethod())
                .status(report.getStatus())
                .statusText(statusText)
                .reviewerId(report.getReviewerId())
                .reviewerName(reviewerName)
                .reviewComment(report.getReviewComment())
                .reviewedAt(report.getReviewedAt())
                .signatureData(report.getSignatureData())
                .blockchainTxHash(report.getBlockchainTxHash())
                .onChainAt(report.getOnChainAt())
                .attachments(report.getAttachments())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
