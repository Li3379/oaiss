package com.oaiss.chain.repository;

import com.oaiss.chain.entity.ReviewerQualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审核员资质 Repository
 *
 * @author OAISS Team
 */
@Repository
public interface ReviewerQualificationRepository extends JpaRepository<ReviewerQualification, Long> {

    List<ReviewerQualification> findByReviewerIdAndDeletedFalse(Long reviewerId);

    List<ReviewerQualification> findByReviewerIdAndStatusAndDeletedFalse(Long reviewerId, Integer status);

    boolean existsByCertificateNoAndDeletedFalse(String certificateNo);
}
