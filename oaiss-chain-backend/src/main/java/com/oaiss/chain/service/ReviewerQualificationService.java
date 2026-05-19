package com.oaiss.chain.service;

import com.oaiss.chain.annotation.DistributedLock;
import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.entity.ReviewerQualification;
import com.oaiss.chain.entity.User;
import com.oaiss.chain.enums.QualificationStatusEnum;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.repository.ReviewerQualificationRepository;
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

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 审核员资格证 Service
 *
 * @author OAISS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewerQualificationService {

    private final ReviewerQualificationRepository reviewerQualificationRepository;
    private final ReviewerRepository reviewerRepository;
    private final UserRepository userRepository;

    private static final int MAX_CERT_NO_RETRIES = 3;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 签发审核员资格证
     *
     * @param reviewerId 审核员ID
     * @return 签发的资格证
     */
    @DistributedLock(key = "'cert:reviewer:' + #reviewerId")
    @Transactional
    public ReviewerQualification issueCertificate(Long reviewerId) {
        // 0. Verify reviewer exists (WR-01)
        userRepository.findById(reviewerId)
                .orElseThrow(() -> BusinessException.notFound("error.user.notFound"));

        // 1. Check duplicate ACTIVE
        List<ReviewerQualification> active = reviewerQualificationRepository
                .findByReviewerIdAndStatusAndDeletedFalse(reviewerId, QualificationStatusEnum.ACTIVE.getCode());
        if (!active.isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "error.qualification.alreadyActive");
        }

        // 2. Generate certificateNo with collision retry
        String certNo = generateUniqueCertificateNo();

        // 3. Build and save
        ReviewerQualification qualification = ReviewerQualification.builder()
                .reviewerId(reviewerId)
                .qualificationType("碳排放审核资质")
                .certificateNo(certNo)
                .issuingAuthority("OAISS管理中心")
                .issuedDate(LocalDate.now())
                .status(QualificationStatusEnum.ACTIVE.getCode())
                .build();
        qualification = reviewerQualificationRepository.save(qualification);
        log.info("ReviewerQualification issued: {} for reviewer {}", certNo, reviewerId);
        return qualification;
    }

    /**
     * 吊销审核员资格证
     *
     * @param reviewerId 审核员ID
     */
    @Transactional
    public void revokeCertificate(Long reviewerId) {
        // Find ACTIVE certificate
        ReviewerQualification qualification = reviewerQualificationRepository
                .findByReviewerIdAndStatusAndDeletedFalse(reviewerId, 1)
                .stream().findFirst()
                .orElseThrow(() -> BusinessException.notFound("error.qualification.notFound"));

        qualification.setStatus(QualificationStatusEnum.REVOKED.getCode());
        reviewerQualificationRepository.save(qualification);
        log.info("ReviewerQualification revoked for reviewer {}", reviewerId);
    }

    /**
     * 查询资格证列表（分页，可选状态筛选）
     *
     * @param status 状态筛选（可选）
     * @param page   页码（从1开始）
     * @param size   每页数量
     * @return 分页结果
     */
    public Page<ReviewerQualification> listCertificates(Integer status, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return reviewerQualificationRepository.findByStatusAndDeletedFalse(status, pageable);
        }
        return reviewerQualificationRepository.findByDeletedFalse(pageable);
    }

    /**
     * 查询审核员自身的资格证
     *
     * @param reviewerId 审核员ID
     * @return 该审核员的所有资格证
     */
    public List<ReviewerQualification> getMyCertificate(Long reviewerId) {
        return reviewerQualificationRepository.findByReviewerIdAndDeletedFalse(reviewerId);
    }

    /**
     * 生成唯一证书编号: RQ-{yyyyMMdd}-{6位随机数字}
     */
    private String generateUniqueCertificateNo() {
        for (int i = 0; i < MAX_CERT_NO_RETRIES; i++) {
            String certNo = String.format("RQ-%s-%06d",
                    DATE_FORMAT.format(LocalDate.now()),
                    SECURE_RANDOM.nextInt(1000000));
            if (!reviewerQualificationRepository.existsByCertificateNoAndDeletedFalse(certNo)) {
                return certNo;
            }
        }
        throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "error.qualification.certNoGenerationFailed");
    }
}
