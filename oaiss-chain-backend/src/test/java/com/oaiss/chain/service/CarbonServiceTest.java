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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private CreditScoreService creditScoreService;

    @Mock
    private EmissionRatingService emissionRatingService;

    @Mock
    private BlockchainServicePort blockchainService;

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
        testReport.setCreatedAt(LocalDateTime.now());
        testReport.setUpdatedAt(LocalDateTime.now());

        reportRequest = new CarbonReportRequest();
        reportRequest.setAccountingPeriod("2024-Q1");
        reportRequest.setTitle("Test Report");
        reportRequest.setReportType(1);
        reportRequest.setEmissionData("{\"scope1\":[]}");
    }

    @Test
    @DisplayName("create report success")
    void testCreateReportSuccess() {
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));
        when(carbonReportRepository.save(any(CarbonReport.class))).thenReturn(testReport);

        CarbonReportResponse response = carbonService.createReport(currentUser, reportRequest);

        assertNotNull(response);
        verify(carbonReportRepository, times(1)).save(any(CarbonReport.class));
    }

    @Test
    @DisplayName("create report fails when enterprise missing")
    void testCreateReportFailEnterpriseNotFound() {
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(CarbonException.class, () -> carbonService.createReport(currentUser, reportRequest));
        verify(carbonReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("submit report success")
    void testSubmitReportSuccess() throws Exception {
        testReport.setStatus(ReportStatusEnum.DRAFT.getCode());
        testReport.setEmissionData("{\"scope1\":[],\"scope2\":[],\"scope3\":[]}");

        ObjectMapper realMapper = new ObjectMapper();
        var mockNode = realMapper.readTree("{\"scope1\":[],\"scope2\":[],\"scope3\":[]}");

        when(carbonReportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));
        when(objectMapper.readTree(any(String.class))).thenReturn(mockNode);
        when(carbonReportRepository.save(any(CarbonReport.class))).thenReturn(testReport);

        CarbonReportResponse response = carbonService.submitReport(currentUser, 1L);

        assertNotNull(response);
        verify(carbonReportRepository, times(1)).save(any(CarbonReport.class));
    }

    @Test
    @DisplayName("submit report fails when report missing")
    void testSubmitReportFailReportNotFound() {
        when(carbonReportRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CarbonException.class, () -> carbonService.submitReport(currentUser, 999L));
    }

    @Test
    @DisplayName("review approved report stops at approved status")
    void testReviewReportSuccess() {
        testReport.setStatus(ReportStatusEnum.SUBMITTED.getCode());
        ReviewRequest reviewRequest = new ReviewRequest(1L, ReportStatusEnum.APPROVED.getCode(), "Approved");

        when(carbonReportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(carbonReportRepository.save(any(CarbonReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CarbonReportResponse response = carbonService.reviewReport(currentUser, reviewRequest);

        assertNotNull(response);
        assertEquals(ReportStatusEnum.APPROVED.getCode(), response.getStatus());
        verify(blockchainService, never()).commitReportToChain(any(), any());
        verify(creditScoreService, never()).addBonusPoints(any(), any(), any(), any());
    }

    @Test
    @DisplayName("admin certification pushes approved report on-chain")
    void testCertifyReportSuccess() {
        testReport.setStatus(ReportStatusEnum.APPROVED.getCode());
        testReport.setTotalEmission(BigDecimal.TEN);
        ReviewRequest certificationRequest = new ReviewRequest(1L, ReportStatusEnum.ON_CHAIN.getCode(), "certified");

        when(carbonReportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(carbonReportRepository.save(any(CarbonReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(blockchainService.commitReportToChain(1L, testReport.getEmissionData())).thenReturn("tx-123");

        CarbonReportResponse response = carbonService.certifyReport(currentUser, certificationRequest);

        assertNotNull(response);
        assertEquals(ReportStatusEnum.ON_CHAIN.getCode(), response.getStatus());
        assertEquals("tx-123", response.getBlockchainTxHash());
        verify(blockchainService, times(1)).commitReportToChain(1L, testReport.getEmissionData());
        verify(creditScoreService, times(1)).addBonusPoints(eq(1L), eq(5), any(), eq(1L));
        verify(emissionRatingService, times(1)).rateEnterprise(eq(1L), eq("2024"), eq(BigDecimal.TEN), eq(null), eq(1L));
    }

    @Test
    @DisplayName("admin certification rejects non-approved report")
    void testCertifyReportFailsWhenStatusInvalid() {
        testReport.setStatus(ReportStatusEnum.SUBMITTED.getCode());
        ReviewRequest certificationRequest = new ReviewRequest(1L, ReportStatusEnum.ON_CHAIN.getCode(), "certified");

        when(carbonReportRepository.findById(1L)).thenReturn(Optional.of(testReport));

        assertThrows(CarbonException.class, () -> carbonService.certifyReport(currentUser, certificationRequest));
        verify(blockchainService, never()).commitReportToChain(any(), any());
    }

    @Test
    @DisplayName("get report success")
    void testGetReportSuccess() {
        when(carbonReportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(testEnterprise));

        CarbonReportResponse response = carbonService.getReport(1L);

        assertNotNull(response);
        assertEquals("CR20240101001", response.getReportNo());
    }

    @Test
    @DisplayName("get report fails when not found")
    void testGetReportFailNotFound() {
        when(carbonReportRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CarbonException.class, () -> carbonService.getReport(999L));
    }

    @Test
    @DisplayName("list reports")
    void testListReports() {
        Page<CarbonReport> page = new PageImpl<>(Arrays.asList(testReport));
        when(carbonReportRepository.search(any(), any(), any(), any(Pageable.class))).thenReturn(page);
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(testEnterprise));

        Page<CarbonReportResponse> result = carbonService.listReports(null, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("list my reports")
    void testListMyReports() {
        Page<CarbonReport> page = new PageImpl<>(Arrays.asList(testReport));
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));
        when(carbonReportRepository.searchMyReports(eq(1L), eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(page);
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(testEnterprise));

        Page<CarbonReportResponse> result = carbonService.listMyReports(currentUser, null, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("list my reports with title and accounting period filters")
    void testListMyReportsWithFilters() {
        Page<CarbonReport> page = new PageImpl<>(Arrays.asList(testReport));
        when(enterpriseRepository.findByUserId(1L)).thenReturn(Optional.of(testEnterprise));
        when(carbonReportRepository.searchMyReports(eq(1L), eq(ReportStatusEnum.DRAFT.getCode()), eq("Test"), eq("2024-Q1"), any(Pageable.class)))
                .thenReturn(page);
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(testEnterprise));

        Page<CarbonReportResponse> result = carbonService.listMyReports(
                currentUser,
                ReportStatusEnum.DRAFT.getCode(),
                "  Test  ",
                " 2024-Q1 ",
                1,
                10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(carbonReportRepository).searchMyReports(eq(1L), eq(ReportStatusEnum.DRAFT.getCode()), eq("Test"), eq("2024-Q1"), any(Pageable.class));
    }

    @Test
    @DisplayName("delete report success")
    void testDeleteReportSuccess() {
        testReport.setStatus(ReportStatusEnum.DRAFT.getCode());
        when(carbonReportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(carbonReportRepository.save(any(CarbonReport.class))).thenReturn(testReport);

        carbonService.deleteReport(currentUser, 1L);

        verify(carbonReportRepository, times(1)).save(any(CarbonReport.class));
    }

    @Test
    @DisplayName("delete report fails when not draft")
    void testDeleteReportFailNotDraft() {
        testReport.setStatus(ReportStatusEnum.SUBMITTED.getCode());
        when(carbonReportRepository.findById(1L)).thenReturn(Optional.of(testReport));

        assertThrows(CarbonException.class, () -> carbonService.deleteReport(currentUser, 1L));
        verify(carbonReportRepository, never()).save(any());
    }
}
