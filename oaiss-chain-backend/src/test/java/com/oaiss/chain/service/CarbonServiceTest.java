package com.oaiss.chain.service;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CarbonService 单元测试
 * CarbonService Unit Tests
 */
@ExtendWith(MockitoExtension.class)
class CarbonServiceTest {

    @Mock
    private CarbonReportRepository carbonReportRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CarbonService carbonService;

    private JwtUserDetails currentUser;
    private Enterprise testEnterprise;
    private CarbonReport testReport;
    private CarbonReportRequest reportRequest;

    @BeforeEach
    void setUp() {
        currentUser = new JwtUserDetails();
        currentUser.setUserId(1L);
        currentUser.setUsername("testuser");

        testEnterprise = Enterprise.builder()
                .userId(1L)
                .enterpriseName("Test Enterprise")
                .build();
        testEnterprise.setId(1L);

        testReport = CarbonReport.builder()
                .reportNo("CR20240101001")
                .enterpriseId(1L)
                .submitterId(1L)
                .accountingPeriod("2024-Q1")
                .title("Test Carbon Report")
                .reportType(1)
                .emissionData("{\"scope1\":[]}")
                .status(ReportStatusEnum.DRAFT.getCode())
                .totalEmission(BigDecimal.ZERO)
                .build();
        testReport.setId(1L);

        reportRequest = new CarbonReportRequest();
        reportRequest.setAccountingPeriod("2024-Q1");
        reportRequest.setTitle("Test Report");
        reportRequest.setReportType(1);
        reportRequest.setEmissionData("{\"scope1\":[]}");
    }

    @Test
    @DisplayName("创建碳报告成功")
    void testCreateReportSuccess() {
        // Given
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));
        when(carbonReportRepository.save(any(CarbonReport.class))).thenReturn(testReport);

        // When
        CarbonReportResponse response = carbonService.createReport(currentUser, reportRequest);

        // Then
        assertNotNull(response);
        verify(carbonReportRepository, times(1)).save(any(CarbonReport.class));
    }

    @Test
    @DisplayName("创建碳报告失败-企业不存在")
    void testCreateReportFailEnterpriseNotFound() {
        // Given
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CarbonException.class, () -> carbonService.createReport(currentUser, reportRequest));
        verify(carbonReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("提交碳报告成功")
    void testSubmitReportSuccess() throws Exception {
        // Given
        testReport.setStatus(ReportStatusEnum.DRAFT.getCode());
        testReport.setEmissionData("{\"scope1\":[],\"scope2\":[],\"scope3\":[]}");
        
        // Create a real ObjectMapper for this test
        ObjectMapper realMapper = new ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode mockNode = realMapper.readTree("{\"scope1\":[],\"scope2\":[],\"scope3\":[]}");
        
        when(carbonReportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));
        when(objectMapper.readTree(any(String.class))).thenReturn(mockNode);
        when(carbonReportRepository.save(any(CarbonReport.class))).thenReturn(testReport);

        // When
        CarbonReportResponse response = carbonService.submitReport(currentUser, 1L);

        // Then
        assertNotNull(response);
        verify(carbonReportRepository, times(1)).save(any(CarbonReport.class));
    }

    @Test
    @DisplayName("提交碳报告失败-报告不存在")
    void testSubmitReportFailReportNotFound() {
        // Given
        when(carbonReportRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CarbonException.class, () -> carbonService.submitReport(currentUser, 999L));
    }

    @Test
    @DisplayName("审核碳报告成功")
    void testReviewReportSuccess() {
        // Given
        testReport.setStatus(ReportStatusEnum.SUBMITTED.getCode());
        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setReportId(1L);
        reviewRequest.setReviewResult(ReportStatusEnum.APPROVED.getCode());
        reviewRequest.setReviewComment("Approved");

        when(carbonReportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(carbonReportRepository.save(any(CarbonReport.class))).thenReturn(testReport);

        // When
        CarbonReportResponse response = carbonService.reviewReport(currentUser, reviewRequest);

        // Then
        assertNotNull(response);
        verify(carbonReportRepository, times(1)).save(any(CarbonReport.class));
    }

    @Test
    @DisplayName("获取报告详情成功")
    void testGetReportSuccess() {
        // Given
        when(carbonReportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(testEnterprise));

        // When
        CarbonReportResponse response = carbonService.getReport(1L);

        // Then
        assertNotNull(response);
        assertEquals("CR20240101001", response.getReportNo());
    }

    @Test
    @DisplayName("获取报告详情失败-不存在")
    void testGetReportFailNotFound() {
        // Given
        when(carbonReportRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CarbonException.class, () -> carbonService.getReport(999L));
    }

    @Test
    @DisplayName("分页查询报告")
    void testListReports() {
        // Given
        Page<CarbonReport> page = new PageImpl<>(Arrays.asList(testReport));
        when(carbonReportRepository.search(any(), any(), any(), any(Pageable.class))).thenReturn(page);
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(testEnterprise));

        // When
        Page<CarbonReportResponse> result = carbonService.listReports(null, null, null, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("查询我的报告")
    void testListMyReports() {
        // Given
        Page<CarbonReport> page = new PageImpl<>(Arrays.asList(testReport));
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));
        when(carbonReportRepository.findByEnterpriseIdAndDeletedFalse(eq(1L), any(Pageable.class))).thenReturn(page);
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(testEnterprise));

        // When
        Page<CarbonReportResponse> result = carbonService.listMyReports(currentUser, null, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("删除碳报告成功")
    void testDeleteReportSuccess() {
        // Given
        testReport.setStatus(ReportStatusEnum.DRAFT.getCode());
        when(carbonReportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(carbonReportRepository.save(any(CarbonReport.class))).thenReturn(testReport);

        // When
        carbonService.deleteReport(currentUser, 1L);

        // Then
        verify(carbonReportRepository, times(1)).save(any(CarbonReport.class));
    }

    @Test
    @DisplayName("删除碳报告失败-非草稿状态")
    void testDeleteReportFailNotDraft() {
        // Given
        testReport.setStatus(ReportStatusEnum.SUBMITTED.getCode());
        when(carbonReportRepository.findById(1L)).thenReturn(Optional.of(testReport));

        // When & Then
        assertThrows(CarbonException.class, () -> carbonService.deleteReport(currentUser, 1L));
        verify(carbonReportRepository, never()).save(any());
    }
}
