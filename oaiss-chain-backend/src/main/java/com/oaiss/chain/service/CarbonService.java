package com.oaiss.chain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarbonService {

    private static final String ENTERPRISE_NOT_FOUND = "Enterprise info not found";
    private static final String NO_PERMISSION = "No permission to operate this report";
    private static final String REVIEW_STATUS_INVALID = "Report status does not allow review";
    private static final String REVIEW_DECISION_INVALID = "Invalid reviewer decision";
    private static final String CERTIFY_STATUS_INVALID = "Only approved reports can be certified";
    private static final String CERTIFY_DECISION_INVALID = "Invalid certification decision";
    private static final String CERTIFICATION_COMMENT_PREFIX = "Certification Comment: ";

    private final CarbonReportRepository carbonReportRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final CreditScoreService creditScoreService;
    private final EmissionRatingService emissionRatingService;
    private final BlockchainServicePort blockchainService;

    @Transactional
    public CarbonReportResponse createReport(JwtUserDetails currentUser, CarbonReportRequest request) {
        Enterprise enterprise = enterpriseRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> CarbonException.submitFailed(ENTERPRISE_NOT_FOUND));

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

    @Transactional
    public CarbonReportResponse submitReport(JwtUserDetails currentUser, Long reportId) {
        CarbonReport report = carbonReportRepository.findById(reportId)
                .orElseThrow(() -> CarbonException.reportNotFound(reportId));

        Enterprise enterprise = enterpriseRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> CarbonException.submitFailed(ENTERPRISE_NOT_FOUND));
        if (!report.getEnterpriseId().equals(enterprise.getId())) {
            throw CarbonException.submitFailed(NO_PERMISSION);
        }

        ReportStatusEnum status = ReportStatusEnum.fromCode(report.getStatus());
        if (!status.isSubmittable()) {
            throw CarbonException.reportAlreadySubmitted(reportId);
        }

        calculateEmissions(report);
        report.setStatus(ReportStatusEnum.SUBMITTED.getCode());
        report = carbonReportRepository.save(report);

        log.info("Carbon report submitted: {}", report.getReportNo());
        return toResponse(report);
    }

    @Transactional
    public CarbonReportResponse reviewReport(JwtUserDetails currentUser, ReviewRequest request) {
        CarbonReport report = carbonReportRepository.findById(request.getReportId())
                .orElseThrow(() -> CarbonException.reportNotFound(request.getReportId()));

        ReportStatusEnum status = ReportStatusEnum.fromCode(report.getStatus());
        if (!status.isReviewable()) {
            throw CarbonException.submitFailed(REVIEW_STATUS_INVALID);
        }
        if (!isReviewerDecision(request.getReviewResult())) {
            throw CarbonException.submitFailed(REVIEW_DECISION_INVALID);
        }

        report.setReviewerId(currentUser.getUserId());
        report.setReviewComment(CommonUtils.sanitizeHtml(CommonUtils.sanitizeInput(request.getReviewComment())));
        report.setReviewedAt(LocalDateTime.now());
        report.setStatus(request.getReviewResult());

        report = carbonReportRepository.save(report);
        log.info("Carbon report reviewed: {} -> status={}", report.getReportNo(), report.getStatus());
        return toResponse(report);
    }

    @Transactional
    public CarbonReportResponse certifyReport(JwtUserDetails currentUser, ReviewRequest request) {
        CarbonReport report = carbonReportRepository.findById(request.getReportId())
                .orElseThrow(() -> CarbonException.reportNotFound(request.getReportId()));

        ReportStatusEnum status = ReportStatusEnum.fromCode(report.getStatus());
        if (status != ReportStatusEnum.APPROVED) {
            throw CarbonException.submitFailed(CERTIFY_STATUS_INVALID);
        }
        if (!isCertificationDecision(request.getReviewResult())) {
            throw CarbonException.submitFailed(CERTIFY_DECISION_INVALID);
        }

        if (Objects.equals(request.getReviewResult(), ReportStatusEnum.ON_CHAIN.getCode())) {
            applyApprovedReportSideEffects(report, currentUser.getUserId());
            report.setStatus(ReportStatusEnum.ON_CHAIN.getCode());
        } else {
            if (request.getReviewComment() != null && !request.getReviewComment().isBlank()) {
                String sanitizedComment = CommonUtils.sanitizeHtml(CommonUtils.sanitizeInput(request.getReviewComment()));
                report.setReviewComment(mergeCertificationComment(report.getReviewComment(), sanitizedComment));
            }
            report.setStatus(ReportStatusEnum.REJECTED.getCode());
            report.setBlockchainTxHash(null);
            report.setOnChainAt(null);
        }

        report = carbonReportRepository.save(report);
        log.info("Carbon report certified: {} -> status={}", report.getReportNo(), report.getStatus());
        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public CarbonReportResponse getReport(Long reportId) {
        CarbonReport report = carbonReportRepository.findById(reportId)
                .orElseThrow(() -> CarbonException.reportNotFound(reportId));
        return toResponse(report);
    }

    public Page<CarbonReportResponse> listReports(Long enterpriseId, Integer status, String keyword, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CarbonReport> reports = carbonReportRepository.search(enterpriseId, status, keyword, pageable);
        return reports.map(this::toResponse);
    }

    public Page<CarbonReportResponse> listMyReports(
            JwtUserDetails currentUser,
            Integer status,
            String title,
            String accountingPeriod,
            Integer page,
            Integer size) {
        Enterprise enterprise = enterpriseRepository.findByUserId(currentUser.getUserId())
                .orElseThrow(() -> CarbonException.submitFailed(ENTERPRISE_NOT_FOUND));

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String normalizedTitle = normalizeFilter(title);
        String normalizedAccountingPeriod = normalizeFilter(accountingPeriod);
        Page<CarbonReport> reports = carbonReportRepository.searchMyReports(
                enterprise.getId(),
                status,
                normalizedTitle,
                normalizedAccountingPeriod,
                pageable);
        return reports.map(this::toResponse);
    }

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

    private void calculateEmissions(CarbonReport report) {
        BigDecimal[] totals = parseEmissionTotals(report.getEmissionData());
        report.setScope1Emission(totals[0]);
        report.setScope2Emission(totals[1]);
        report.setScope3Emission(totals[2]);
        report.setTotalEmission(totals[3]);
    }

    private void applyApprovedReportSideEffects(CarbonReport report, Long operatorUserId) {
        Long enterpriseId = report.getEnterpriseId();

        creditScoreService.addBonusPoints(
                enterpriseId,
                5,
                "Carbon report certification reward",
                operatorUserId);

        String ratingYear = report.getAccountingPeriod();
        if (ratingYear == null || ratingYear.isEmpty()) {
            ratingYear = String.valueOf(LocalDateTime.now().getYear());
        } else if (ratingYear.length() > 4) {
            ratingYear = ratingYear.substring(0, 4);
        }
        emissionRatingService.rateEnterprise(
                enterpriseId,
                ratingYear,
                report.getTotalEmission(),
                null,
                operatorUserId);

        String txHash = blockchainService.commitReportToChain(report.getId(), report.getEmissionData());
        report.setBlockchainTxHash(txHash);
        report.setOnChainAt(LocalDateTime.now());
    }

    private boolean isReviewerDecision(Integer decision) {
        return decision != null
                && (decision.equals(ReportStatusEnum.APPROVED.getCode())
                || decision.equals(ReportStatusEnum.REJECTED.getCode()));
    }

    private boolean isCertificationDecision(Integer decision) {
        return decision != null
                && (decision.equals(ReportStatusEnum.ON_CHAIN.getCode())
                || decision.equals(ReportStatusEnum.REJECTED.getCode()));
    }

    private String mergeCertificationComment(String existingComment, String certificationComment) {
        if (existingComment == null || existingComment.isBlank()) {
            return CERTIFICATION_COMMENT_PREFIX + certificationComment;
        }
        return existingComment + System.lineSeparator() + CERTIFICATION_COMMENT_PREFIX + certificationComment;
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal[] parseEmissionTotals(String emissionData) {
        BigDecimal scope1 = BigDecimal.ZERO;
        BigDecimal scope2 = BigDecimal.ZERO;
        BigDecimal scope3 = BigDecimal.ZERO;
        try {
            if (emissionData != null) {
                JsonNode data = objectMapper.readTree(emissionData);

                if (data.has("scope1")) {
                    for (JsonNode item : data.get("scope1")) {
                        BigDecimal activity = new BigDecimal(item.get("activity_data").asText("0"));
                        BigDecimal factor = new BigDecimal(item.get("emission_factor").asText("0"));
                        scope1 = scope1.add(activity.multiply(factor));
                    }
                }

                if (data.has("scope2")) {
                    for (JsonNode item : data.get("scope2")) {
                        BigDecimal activity = new BigDecimal(item.get("activity_data").asText("0"));
                        BigDecimal factor = new BigDecimal(item.get("emission_factor").asText("0"));
                        scope2 = scope2.add(activity.multiply(factor));
                    }
                }

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

    private CarbonReportResponse toResponse(CarbonReport report) {
        String enterpriseName = null;
        String reviewerName = null;

        if (report.getEnterpriseId() != null) {
            enterpriseName = enterpriseRepository.findById(report.getEnterpriseId())
                    .map(Enterprise::getEnterpriseName)
                    .orElse(null);
        }
        if (report.getReviewerId() != null) {
            reviewerName = userRepository.findById(report.getReviewerId())
                    .map(User::getRealName)
                    .orElse(null);
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
