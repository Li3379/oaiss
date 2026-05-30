package com.oaiss.chain.service;

import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.entity.Reviewer;
import com.oaiss.chain.entity.ReviewerQualification;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.repository.ReviewerQualificationRepository;
import com.oaiss.chain.repository.ReviewerRepository;
import com.oaiss.chain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewerQualificationServiceTest {

    @Mock
    private ReviewerQualificationRepository reviewerQualificationRepository;

    @Mock
    private ReviewerRepository reviewerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewerQualificationService reviewerQualificationService;

    private Reviewer testReviewer;
    private ReviewerQualification testQualification;

    @BeforeEach
    void setUp() {
        testReviewer = Reviewer.builder()
                .userId(10L)
                .qualificationNo("RQ-INIT-001")
                .level(1)
                .build();
        testReviewer.setId(1L);

        testQualification = ReviewerQualification.builder()
                .reviewerId(1L)
                .qualificationType("碳排放审核资质")
                .certificateNo("RQ-20260515-123456")
                .issuingAuthority("OAISS管理中心")
                .issuedDate(LocalDate.now())
                .status(1)
                .build();
        testQualification.setId(1L);
    }

    @Test
    @DisplayName("签发审核员资格证-成功")
    void testIssueCertificateSuccess() {
        when(reviewerRepository.findById(1L)).thenReturn(java.util.Optional.of(testReviewer));
        when(reviewerQualificationRepository.findByReviewerIdAndDeletedFalse(10L))
                .thenReturn(Collections.emptyList());
        when(reviewerQualificationRepository.findByReviewerIdAndStatusAndDeletedFalse(1L, 1))
                .thenReturn(Collections.emptyList());
        when(reviewerQualificationRepository.existsByCertificateNoAndDeletedFalse(anyString()))
                .thenReturn(false);
        when(reviewerQualificationRepository.save(any(ReviewerQualification.class)))
                .thenReturn(testQualification);

        ReviewerQualification result = reviewerQualificationService.issueCertificate(1L);

        assertNotNull(result);
        assertEquals(1L, result.getReviewerId());
        assertEquals(1, result.getStatus());
        assertNotNull(result.getCertificateNo());
        verify(reviewerQualificationRepository).findByReviewerIdAndStatusAndDeletedFalse(1L, 1);
        verify(reviewerQualificationRepository).save(any(ReviewerQualification.class));
    }

    @Test
    @DisplayName("签发审核员资格证-已有有效资质证时拒绝重复签发")
    void testIssueCertificateDuplicateActiveThrowsException() {
        when(reviewerRepository.findById(1L)).thenReturn(java.util.Optional.of(testReviewer));
        when(reviewerQualificationRepository.findByReviewerIdAndDeletedFalse(10L))
                .thenReturn(Collections.emptyList());
        when(reviewerQualificationRepository.findByReviewerIdAndStatusAndDeletedFalse(1L, 1))
                .thenReturn(List.of(testQualification));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewerQualificationService.issueCertificate(1L));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());

        verify(reviewerQualificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("吊销审核员资格证-成功")
    void testRevokeCertificateSuccess() {
        when(reviewerRepository.findById(1L)).thenReturn(java.util.Optional.of(testReviewer));
        when(reviewerQualificationRepository.findByReviewerIdAndDeletedFalse(10L))
                .thenReturn(Collections.emptyList());
        when(reviewerQualificationRepository
                .findFirstByReviewerIdAndStatusAndDeletedFalseOrderByIssuedDateDescCreatedAtDesc(1L, 1))
                .thenReturn(java.util.Optional.of(testQualification));
        when(reviewerQualificationRepository.save(any(ReviewerQualification.class)))
                .thenReturn(testQualification);

        reviewerQualificationService.revokeCertificate(1L);

        assertEquals(2, testQualification.getStatus());
        verify(reviewerQualificationRepository).save(testQualification);
    }

    @Test
    @DisplayName("吊销审核员资格证-无有效资质证时抛出异常")
    void testRevokeCertificateNoActiveCertThrowsException() {
        when(reviewerRepository.findById(1L)).thenReturn(java.util.Optional.of(testReviewer));
        when(reviewerQualificationRepository.findByReviewerIdAndDeletedFalse(10L))
                .thenReturn(Collections.emptyList());
        when(reviewerQualificationRepository
                .findFirstByReviewerIdAndStatusAndDeletedFalseOrderByIssuedDateDescCreatedAtDesc(1L, 1))
                .thenReturn(java.util.Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewerQualificationService.revokeCertificate(1L));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getCode());

        verify(reviewerQualificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("查询资格证列表-带状态筛选")
    void testListCertificatesWithStatusFilter() {
        Page<ReviewerQualification> page = new PageImpl<>(List.of(testQualification));
        when(reviewerQualificationRepository.findByStatusAndDeletedFalse(eq(1), any(Pageable.class)))
                .thenReturn(page);

        Page<ReviewerQualification> result = reviewerQualificationService.listCertificates(1, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(reviewerQualificationRepository).findByStatusAndDeletedFalse(eq(1), any(Pageable.class));
        verify(reviewerQualificationRepository, never()).findByDeletedFalse(any());
    }

    @Test
    @DisplayName("查询资格证列表-不带状态筛选")
    void testListCertificatesWithoutStatusFilter() {
        Page<ReviewerQualification> page = new PageImpl<>(List.of(testQualification));
        when(reviewerQualificationRepository.findByDeletedFalse(any(Pageable.class)))
                .thenReturn(page);

        Page<ReviewerQualification> result = reviewerQualificationService.listCertificates(null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(reviewerQualificationRepository).findByDeletedFalse(any(Pageable.class));
        verify(reviewerQualificationRepository, never()).findByStatusAndDeletedFalse(any(), any());
    }

    @Test
    @DisplayName("签发审核员资格证-审核员不存在时抛出异常")
    void testIssueCertificateReviewerNotFoundThrowsException() {
        when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewerQualificationService.issueCertificate(999L));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getCode());

        verify(reviewerQualificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("查询审核员自身资质证")
    void testGetMyCertificate() {
        when(reviewerRepository.findById(1L)).thenReturn(java.util.Optional.of(testReviewer));
        when(reviewerQualificationRepository.findByReviewerIdAndDeletedFalse(1L))
                .thenReturn(List.of(testQualification));
        when(reviewerQualificationRepository.findByReviewerIdAndDeletedFalse(10L))
                .thenReturn(Collections.emptyList());

        List<ReviewerQualification> result = reviewerQualificationService.getMyCertificate(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getReviewerId());
        verify(reviewerQualificationRepository).findByReviewerIdAndDeletedFalse(1L);
    }
}