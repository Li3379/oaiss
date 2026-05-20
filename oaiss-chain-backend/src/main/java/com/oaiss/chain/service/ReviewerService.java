package com.oaiss.chain.service;

import com.oaiss.chain.entity.CarbonReport;
import com.oaiss.chain.entity.Reviewer;
import com.oaiss.chain.repository.CarbonReportRepository;
import com.oaiss.chain.repository.ReviewerRepository;
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

    private final ReviewerRepository reviewerRepository;
    private final CarbonReportRepository carbonReportRepository;

    /**
     * 获取当前审核员信息
     */
    @Transactional(readOnly = true)
    public Reviewer getReviewerInfo(Long userId) {
        return reviewerRepository.findByUserIdAndDeletedFalse(userId)
                .filter(r -> !r.getDeleted())
                .orElseThrow(() -> new RuntimeException("审核员信息不存在"));
    }

    /**
     * 获取待审核报告列表
     */
    @Transactional(readOnly = true)
    public Page<CarbonReport> getPendingReports(Long userId, Integer page, Integer size) {
        // 验证审核员身份
        Reviewer reviewer = getReviewerInfo(userId);

        // 查询待审核报告（状态为1-待审核）
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return carbonReportRepository.findByStatusAndDeletedFalse(1, pageable);
    }

    /**
     * 获取审核历史
     */
    @Transactional(readOnly = true)
    public Page<CarbonReport> getReviewHistory(Long userId, Integer page, Integer size) {
        // 验证审核员身份
        Reviewer reviewer = getReviewerInfo(userId);

        // 查询已审核的报告（状态为2-已通过或3-已驳回）
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        return carbonReportRepository.findByStatusInAndDeletedFalse(List.of(2, 3), pageable);
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

        // 统计待审核数量
        long pendingCount = carbonReportRepository.countByStatusAndDeletedFalse(1);
        stats.put("pendingCount", pendingCount);

        // 统计已通过数量
        long passedCount = carbonReportRepository.countByStatusAndDeletedFalse(2);
        stats.put("passedCount", passedCount);

        // 统计已驳回数量
        long rejectedCount = carbonReportRepository.countByStatusAndDeletedFalse(3);
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
}
