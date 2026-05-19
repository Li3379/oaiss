package com.oaiss.chain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaiss.chain.dto.CarbonNeutralProjectRequest;
import com.oaiss.chain.dto.CarbonNeutralProjectResponse;
import com.oaiss.chain.dto.ProjectVerificationRequest;
import com.oaiss.chain.entity.CarbonNeutralProject;
import com.oaiss.chain.entity.Enterprise;
import com.oaiss.chain.entity.User;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.repository.CarbonNeutralProjectRepository;
import com.oaiss.chain.repository.EnterpriseRepository;
import com.oaiss.chain.repository.UserRepository;
import com.oaiss.chain.security.JwtUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CarbonNeutralProjectServiceTest {

    @Mock
    private CarbonNeutralProjectRepository projectRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CarbonNeutralProjectService service;

    private JwtUserDetails testUser;
    private Enterprise testEnterprise;
    private CarbonNeutralProject testProject;
    private CarbonNeutralProjectRequest projectRequest;

    @BeforeEach
    void setUp() {
        testUser = JwtUserDetails.builder()
                .userId(1L)
                .username("testuser")
                .password("password")
                .userType(1)
                .roles(List.of("ENTERPRISE"))
                .enterpriseId(1L)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        
        testEnterprise = new Enterprise();
        testEnterprise.setId(1L);
        testEnterprise.setUserId(1L);
        testEnterprise.setEnterpriseName("Test Enterprise");
        testEnterprise.setCreditCode("123456789012345678");

        testProject = new CarbonNeutralProject();
        testProject.setProjectNo("CNP202401010001");
        testProject.setProjectName("Test Project");
        testProject.setProjectType(1);
        testProject.setOwnerId(1L);
        testProject.setDescription("Test Description");
        testProject.setExpectedReduction(BigDecimal.valueOf(1000));
        testProject.setStatus(CarbonNeutralProjectService.STATUS_DRAFT);
        testProject.setCertStatus(CarbonNeutralProjectService.CERT_STATUS_NONE);
        testProject.setVerificationStatus(CarbonNeutralProjectService.VERIFY_STATUS_NONE);
        testProject.setIssuedCredits(BigDecimal.ZERO);
        testProject.setUsedCredits(BigDecimal.ZERO);

        projectRequest = new CarbonNeutralProjectRequest();
        projectRequest.setProjectName("New Project");
        projectRequest.setProjectType(1);
        projectRequest.setDescription("Description");
        projectRequest.setExpectedReduction(BigDecimal.valueOf(500));
    }

    @Test
    @DisplayName("创建项目成功")
    void testCreateProjectSuccess() {
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));
        when(projectRepository.save(any())).thenReturn(testProject);

        CarbonNeutralProjectResponse response = service.createProject(testUser, projectRequest);

        assertNotNull(response);
        assertEquals("Test Project", response.getProjectName());
        verify(projectRepository).save(any());
    }

    @Test
    @DisplayName("创建项目失败-未找到企业")
    void testCreateProjectFailNoEnterprise() {
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.createProject(testUser, projectRequest));
    }

    @Test
    @DisplayName("更新项目成功")
    void testUpdateProjectSuccess() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));
        when(projectRepository.save(any())).thenReturn(testProject);

        CarbonNeutralProjectResponse response = service.updateProject(testUser, 1L, projectRequest);

        assertNotNull(response);
        verify(projectRepository).save(any());
    }

    @Test
    @DisplayName("更新项目失败-项目不存在")
    void testUpdateProjectFailProjectNotFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.updateProject(testUser, 1L, projectRequest));
    }

    @Test
    @DisplayName("更新项目失败-无权限")
    void testUpdateProjectFailNoPermission() {
        testProject.setOwnerId(2L);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));

        assertThrows(BusinessException.class, () -> service.updateProject(testUser, 1L, projectRequest));
    }

    @Test
    @DisplayName("更新项目失败-状态不允许")
    void testUpdateProjectFailInvalidStatus() {
        testProject.setStatus(CarbonNeutralProjectService.STATUS_PENDING);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));

        assertThrows(BusinessException.class, () -> service.updateProject(testUser, 1L, projectRequest));
    }

    @Test
    @DisplayName("提交审核成功")
    void testSubmitForReviewSuccess() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));
        when(projectRepository.save(any())).thenReturn(testProject);

        CarbonNeutralProjectResponse response = service.submitForReview(testUser, 1L);

        assertNotNull(response);
        verify(projectRepository).save(any());
    }

    @Test
    @DisplayName("审核项目通过")
    void testReviewProjectApproved() {
        testProject.setStatus(CarbonNeutralProjectService.STATUS_PENDING);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any())).thenReturn(testProject);

        CarbonNeutralProjectResponse response = service.reviewProject(testUser, 1L, true, "Approved");

        assertNotNull(response);
        verify(projectRepository).save(any());
    }

    @Test
    @DisplayName("审核项目拒绝")
    void testReviewProjectRejected() {
        testProject.setStatus(CarbonNeutralProjectService.STATUS_PENDING);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any())).thenReturn(testProject);

        CarbonNeutralProjectResponse response = service.reviewProject(testUser, 1L, false, "Rejected");

        assertNotNull(response);
    }

    @Test
    @DisplayName("审核项目失败-不在待审核状态")
    void testReviewProjectFailInvalidStatus() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        assertThrows(BusinessException.class, () -> service.reviewProject(testUser, 1L, true, "Approved"));
    }

    @Test
    @DisplayName("启动实施成功")
    void testStartImplementationSuccess() {
        testProject.setStatus(CarbonNeutralProjectService.STATUS_APPROVED);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));
        when(projectRepository.save(any())).thenReturn(testProject);

        CarbonNeutralProjectResponse response = service.startImplementation(testUser, 1L);

        assertNotNull(response);
    }

    @Test
    @DisplayName("提交核证申请成功")
    void testSubmitForVerificationSuccess() {
        testProject.setStatus(CarbonNeutralProjectService.STATUS_IMPLEMENTING);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));
        when(projectRepository.save(any())).thenReturn(testProject);

        CarbonNeutralProjectResponse response = service.submitForVerification(testUser, 1L, 2L);

        assertNotNull(response);
    }

    @Test
    @DisplayName("核证项目成功")
    void testVerifyProjectSuccess() {
        testProject.setVerificationStatus(CarbonNeutralProjectService.VERIFY_STATUS_PENDING);
        testProject.setStatus(CarbonNeutralProjectService.STATUS_IMPLEMENTING);
        
        ProjectVerificationRequest request = new ProjectVerificationRequest();
        request.setProjectId(1L);
        request.setVerifiedReduction(BigDecimal.valueOf(800));
        request.setVerificationReport("Report");
        request.setMonitoringData("Data");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any())).thenReturn(testProject);

        CarbonNeutralProjectResponse response = service.verifyProject(testUser, request);

        assertNotNull(response);
    }

    @Test
    @DisplayName("核证项目失败-不在待核证状态")
    void testVerifyProjectFailInvalidStatus() {
        ProjectVerificationRequest request = new ProjectVerificationRequest();
        request.setProjectId(1L);
        request.setVerifiedReduction(BigDecimal.valueOf(800));

        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        assertThrows(BusinessException.class, () -> service.verifyProject(testUser, request));
    }

    @Test
    @DisplayName("消耗碳信用成功")
    void testUseCreditsSuccess() {
        testProject.setIssuedCredits(BigDecimal.valueOf(1000));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));
        when(projectRepository.save(any())).thenReturn(testProject);

        CarbonNeutralProjectResponse response = service.useCredits(testUser, 1L, BigDecimal.valueOf(100));

        assertNotNull(response);
    }

    @Test
    @DisplayName("消耗碳信用失败-余额不足")
    void testUseCreditsFailInsufficient() {
        testProject.setIssuedCredits(BigDecimal.valueOf(100));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));

        assertThrows(BusinessException.class, () -> service.useCredits(testUser, 1L, BigDecimal.valueOf(200)));
    }

    @Test
    @DisplayName("更新监测数据成功")
    void testUpdateMonitoringSuccess() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));
        when(projectRepository.save(any())).thenReturn(testProject);

        CarbonNeutralProjectResponse response = service.updateMonitoring(testUser, 1L, "New Data");

        assertNotNull(response);
    }

    @Test
    @DisplayName("申请认证成功")
    void testApplyForCertificationSuccess() {
        testProject.setVerificationStatus(CarbonNeutralProjectService.VERIFY_STATUS_VERIFIED);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));
        when(projectRepository.save(any())).thenReturn(testProject);

        CarbonNeutralProjectResponse response = service.applyForCertification(testUser, 1L, "CERT_ORG");

        assertNotNull(response);
    }

    @Test
    @DisplayName("完成认证成功")
    void testCompleteCertificationSuccess() {
        testProject.setCertStatus(CarbonNeutralProjectService.CERT_STATUS_PENDING);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any())).thenReturn(testProject);

        CarbonNeutralProjectResponse response = service.completeCertification(1L, "CERT-001");

        assertNotNull(response);
    }

    @Test
    @DisplayName("终止项目成功")
    void testTerminateProjectSuccess() {
        testProject.setStatus(CarbonNeutralProjectService.STATUS_IMPLEMENTING);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));
        when(projectRepository.save(any())).thenReturn(testProject);

        CarbonNeutralProjectResponse response = service.terminateProject(testUser, 1L, "Reason");

        assertNotNull(response);
    }

    @Test
    @DisplayName("获取项目详情成功")
    void testGetProjectSuccess() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(testEnterprise));

        CarbonNeutralProjectResponse response = service.getProject(1L);

        assertNotNull(response);
        assertEquals("Test Project", response.getProjectName());
    }

    @Test
    @DisplayName("获取项目详情失败-项目不存在")
    void testGetProjectFailNotFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.getProject(1L));
    }

    @Test
    @DisplayName("搜索项目成功")
    void testSearchProjectsSuccess() {
        List<CarbonNeutralProject> projects = List.of(testProject);
        Page<CarbonNeutralProject> page = new PageImpl<>(projects);
        when(projectRepository.search(any(), any(), any(), any(Pageable.class))).thenReturn(page);
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(testEnterprise));

        Page<CarbonNeutralProjectResponse> response = service.searchProjects(1, 0, null, 1, 10);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    @DisplayName("获取我的项目列表成功")
    void testGetMyProjectsSuccess() {
        List<CarbonNeutralProject> projects = List.of(testProject);
        Page<CarbonNeutralProject> page = new PageImpl<>(projects);
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));
        when(projectRepository.findByOwnerIdAndDeletedFalse(anyLong(), any(Pageable.class))).thenReturn(page);
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(testEnterprise));

        Page<CarbonNeutralProjectResponse> response = service.getMyProjects(testUser, null, 1, 10);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    @DisplayName("获取待核证项目列表成功")
    void testGetPendingVerificationProjectsSuccess() {
        List<CarbonNeutralProject> projects = List.of(testProject);
        Page<CarbonNeutralProject> page = new PageImpl<>(projects);
        when(projectRepository.findByVerifierIdAndVerificationStatusAndDeletedFalse(anyLong(), anyInt(), any(Pageable.class))).thenReturn(page);
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(testEnterprise));

        Page<CarbonNeutralProjectResponse> response = service.getPendingVerificationProjects(1L, 1, 10);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    @DisplayName("提交审核失败-缺少项目名称")
    void testSubmitForReviewFailMissingName() {
        testProject.setProjectName(null);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));

        assertThrows(BusinessException.class, () -> service.submitForReview(testUser, 1L));
    }

    @Test
    @DisplayName("提交审核失败-缺少项目类型")
    void testSubmitForReviewFailMissingType() {
        testProject.setProjectType(null);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));

        assertThrows(BusinessException.class, () -> service.submitForReview(testUser, 1L));
    }

    @Test
    @DisplayName("提交审核失败-预计减排量无效")
    void testSubmitForReviewFailInvalidReduction() {
        testProject.setExpectedReduction(BigDecimal.ZERO);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(enterpriseRepository.findByUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(testEnterprise));

        assertThrows(BusinessException.class, () -> service.submitForReview(testUser, 1L));
    }
}