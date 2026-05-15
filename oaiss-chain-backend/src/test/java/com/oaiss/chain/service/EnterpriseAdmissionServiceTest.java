package com.oaiss.chain.service;

import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.entity.Enterprise;
import com.oaiss.chain.entity.EnterpriseAdmission;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.repository.EnterpriseAdmissionRepository;
import com.oaiss.chain.repository.EnterpriseRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * EnterpriseAdmissionService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class EnterpriseAdmissionServiceTest {

    @Mock
    private EnterpriseAdmissionRepository enterpriseAdmissionRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @InjectMocks
    private EnterpriseAdmissionService enterpriseAdmissionService;

    private Enterprise testEnterprise;
    private EnterpriseAdmission testAdmission;

    @BeforeEach
    void setUp() {
        testEnterprise = Enterprise.builder()
                .userId(1L)
                .enterpriseName("Test Enterprise")
                .creditCode("91110000MA00000001")
                .build();
        testEnterprise.setId(1L);

        testAdmission = EnterpriseAdmission.builder()
                .enterpriseId(1L)
                .certificateNo("EA-20260515-123456")
                .issuedDate(LocalDate.now())
                .status(1)
                .build();
        testAdmission.setId(1L);
    }

    @Test
    @DisplayName("签发准入证书-成功")
    void testIssueCertificate_success() {
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(testEnterprise));
        when(enterpriseAdmissionRepository.findFirstByEnterpriseIdAndStatusAndDeletedFalse(1L, 1))
                .thenReturn(Optional.empty());
        when(enterpriseAdmissionRepository.existsByCertificateNoAndDeletedFalse(anyString())).thenReturn(false);
        when(enterpriseAdmissionRepository.save(any(EnterpriseAdmission.class))).thenReturn(testAdmission);

        EnterpriseAdmission result = enterpriseAdmissionService.issueCertificate(1L);

        assertNotNull(result);
        assertEquals(1L, result.getEnterpriseId());
        assertEquals(1, result.getStatus());
        assertNotNull(result.getCertificateNo());
        assertTrue(result.getCertificateNo().startsWith("EA-"));
        assertEquals(LocalDate.now(), result.getIssuedDate());

        verify(enterpriseRepository).findById(1L);
        verify(enterpriseAdmissionRepository).findFirstByEnterpriseIdAndStatusAndDeletedFalse(1L, 1);
        verify(enterpriseAdmissionRepository).save(any(EnterpriseAdmission.class));
    }

    @Test
    @DisplayName("签发准入证书-已有有效证书时拒绝重复签发")
    void testIssueCertificate_duplicateActive_throwsException() {
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(testEnterprise));
        when(enterpriseAdmissionRepository.findFirstByEnterpriseIdAndStatusAndDeletedFalse(1L, 1))
                .thenReturn(Optional.of(testAdmission));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> enterpriseAdmissionService.issueCertificate(1L));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());

        verify(enterpriseAdmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("签发准入证书-企业不存在时抛出异常")
    void testIssueCertificate_enterpriseNotFound_throwsException() {
        when(enterpriseRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> enterpriseAdmissionService.issueCertificate(999L));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getCode());

        verify(enterpriseAdmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("吊销准入证书-成功")
    void testRevokeCertificate_success() {
        when(enterpriseAdmissionRepository.findFirstByEnterpriseIdAndStatusAndDeletedFalse(1L, 1))
                .thenReturn(Optional.of(testAdmission));
        when(enterpriseAdmissionRepository.save(any(EnterpriseAdmission.class))).thenReturn(testAdmission);

        enterpriseAdmissionService.revokeCertificate(1L);

        assertEquals(2, testAdmission.getStatus());
        verify(enterpriseAdmissionRepository).save(testAdmission);
    }

    @Test
    @DisplayName("吊销准入证书-无有效证书时抛出异常")
    void testRevokeCertificate_noActiveCert_throwsException() {
        when(enterpriseAdmissionRepository.findFirstByEnterpriseIdAndStatusAndDeletedFalse(1L, 1))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> enterpriseAdmissionService.revokeCertificate(1L));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getCode());

        verify(enterpriseAdmissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("查询证书列表-带状态筛选")
    void testListCertificates_withStatusFilter() {
        Page<EnterpriseAdmission> page = new PageImpl<>(List.of(testAdmission));
        when(enterpriseAdmissionRepository.findByStatusAndDeletedFalse(eq(1), any(Pageable.class)))
                .thenReturn(page);

        Page<EnterpriseAdmission> result = enterpriseAdmissionService.listCertificates(1, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(enterpriseAdmissionRepository).findByStatusAndDeletedFalse(eq(1), any(Pageable.class));
        verify(enterpriseAdmissionRepository, never()).findByDeletedFalse(any());
    }

    @Test
    @DisplayName("查询证书列表-不带状态筛选")
    void testListCertificates_withoutStatusFilter() {
        Page<EnterpriseAdmission> page = new PageImpl<>(List.of(testAdmission));
        when(enterpriseAdmissionRepository.findByDeletedFalse(any(Pageable.class)))
                .thenReturn(page);

        Page<EnterpriseAdmission> result = enterpriseAdmissionService.listCertificates(null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(enterpriseAdmissionRepository).findByDeletedFalse(any(Pageable.class));
        verify(enterpriseAdmissionRepository, never()).findByStatusAndDeletedFalse(any(), any());
    }
}
