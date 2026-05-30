package com.oaiss.chain.service;

import com.oaiss.chain.entity.CarbonReport;
import com.oaiss.chain.entity.Enterprise;
import com.oaiss.chain.entity.Reviewer;
import com.oaiss.chain.entity.User;
import com.oaiss.chain.enums.ReportStatusEnum;
import com.oaiss.chain.repository.CarbonReportRepository;
import com.oaiss.chain.repository.EnterpriseRepository;
import com.oaiss.chain.repository.ReviewerRepository;
import com.oaiss.chain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审核员服务
 * 提供审核员信息查询、待审核报告管理等功能
 *
 * @author OAISS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewerService {

    private static final List<Integer> HISTORY_STATUSES = List.of(
            ReportStatusEnum.APPROVED.getCode(),
            ReportStatusEnum.REJECTED.getCode(),
            ReportStatusEnum.ON_CHAIN.getCode()
    );

    private final ReviewerRepository reviewerRepository;
    private final CarbonReportRepository carbonReportRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final UserRepository userRepository;

    /**
     * 获取当前审核员信息
     */
    @Transactional(readOnly = true)
    public Reviewer getReviewerInfo(Long userId) {
        Reviewer reviewer = reviewerRepository.findByUserIdAndDeletedFalse(userId)
                .filter(r -> !r.getDeleted())
                .orElseThrow(() -> new RuntimeException("审核员信息不存在"));

        userRepository.findByIdAndDeletedFalse(userId)
                .map(User::getRealName)
                .filter(name -> name != null && !name.isBlank())
                .ifPresent(reviewer::setName);
        return reviewer;
    }

    /**
     * 获取待审核报告列表
     */
    @Transactional(readOnly = true)
    public Page<CarbonReport> getPendingReports(Long userId, Integer page, Integer size) {
        getReviewerInfo(userId);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CarbonReport> reports = carbonReportRepository.findByStatusAndDeletedFalse(
                ReportStatusEnum.SUBMITTED.getCode(), pageable);
        reports.forEach(this::enrichReportDisplayFields);
        return reports;
    }

    /**
     * 获取审核历史
     */
    @Transactional(readOnly = true)
    public Page<CarbonReport> getReviewHistory(Long userId, Integer page, Integer size) {
        Reviewer reviewer = getReviewerInfo(userId);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<CarbonReport> history = carbonReportRepository.findByReviewerIdAndStatusInAndDeletedFalse(
                reviewer.getUserId(), HISTORY_STATUSES, pageable);
        history.forEach(this::enrichReportDisplayFields);
        return history;
    }

    /**
     * 获取审核统计数据
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics(Long userId) {
        Reviewer reviewer = getReviewerInfo(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("completedReviews", reviewer.getCompletedReviews());
        stats.put("level", reviewer.getLevel());
        stats.put("organization", reviewer.getOrganization());

        long pendingCount = carbonReportRepository.countByStatusAndDeletedFalse(ReportStatusEnum.SUBMITTED.getCode());
        stats.put("pendingCount", pendingCount);

        long passedCount = carbonReportRepository.countByStatusAndDeletedFalse(ReportStatusEnum.APPROVED.getCode())
                + carbonReportRepository.countByStatusAndDeletedFalse(ReportStatusEnum.ON_CHAIN.getCode());
        stats.put("passedCount", passedCount);

        long rejectedCount = carbonReportRepository.countByStatusAndDeletedFalse(ReportStatusEnum.REJECTED.getCode());
        stats.put("rejectedCount", rejectedCount);

        return stats;
    }

    /**
     * 更新审核员状态
     */
    @Transactional
    public void incrementCompletedReviews(Long userId) {
        Reviewer reviewer = getReviewerInfo(userId);
        reviewer.setCompletedReviews(reviewer.getCompletedReviews() + 1);
        reviewerRepository.save(reviewer);
        log.info("审核员完成审核数更新: userId={}, completedReviews={}", userId, reviewer.getCompletedReviews());
    }

    private void enrichReportDisplayFields(CarbonReport report) {
        enterpriseRepository.findById(report.getEnterpriseId())
                .map(Enterprise::getEnterpriseName)
                .ifPresent(report::setEnterpriseName);

        if (report.getStatus() != null) {
            report.setStatusText(ReportStatusEnum.fromCode(report.getStatus()).getDescription());
        }
    }
}
