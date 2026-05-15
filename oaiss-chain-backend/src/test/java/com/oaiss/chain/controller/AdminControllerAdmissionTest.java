package com.oaiss.chain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaiss.chain.entity.EnterpriseAdmission;
import com.oaiss.chain.repository.AccountPermissionListRepository;
import com.oaiss.chain.repository.ReviewerRepository;
import com.oaiss.chain.repository.UserRepository;
import com.oaiss.chain.security.JwtTokenProvider;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.EnterpriseAdmissionService;
import com.oaiss.chain.service.ReviewerQualificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminController 企业准入证书端点单元测试
 *
 * Note: @WebMvcTest does not load SecurityConfig (which has @EnableMethodSecurity),
 * so @PreAuthorize annotations are not enforced in these tests. The /my endpoint
 * test verifies controller logic (null enterpriseId handling) rather than role enforcement.
 * Role enforcement is validated by the service-layer tests and integration tests.
 */
@WebMvcTest(value = AdminController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
                org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
        })
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerAdmissionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnterpriseAdmissionService enterpriseAdmissionService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AccountPermissionListRepository permissionRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private ReviewerQualificationService reviewerQualificationService;

    @MockBean
    private ReviewerRepository reviewerRepository;

    private EnterpriseAdmission testAdmission;
    private JwtUserDetails enterpriseUser;

    @BeforeEach
    void setUp() {
        testAdmission = EnterpriseAdmission.builder()
                .enterpriseId(1L)
                .certificateNo("EA-20260515-123456")
                .issuedDate(LocalDate.now())
                .status(1)
                .build();
        testAdmission.setId(1L);

        enterpriseUser = JwtUserDetails.builder()
                .userId(20L)
                .username("enterprise_user")
                .enterpriseId(1L)
                .roles(List.of("ENTERPRISE"))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("签发准入证书-管理员成功签发")
    void testIssueAdmission_asAdmin_returns200() throws Exception {
        when(enterpriseAdmissionService.issueCertificate(1L)).thenReturn(testAdmission);

        mockMvc.perform(post("/admin/enterprise-admission/1/issue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.certificateNo").value("EA-20260515-123456"));

        verify(enterpriseAdmissionService).issueCertificate(1L);
    }

    @Test
    @DisplayName("查看自身准入证书-企业用户成功")
    void testGetMyAdmission_asEnterprise_returns200() throws Exception {
        // Set SecurityContextHolder so @AuthenticationPrincipal can resolve
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(enterpriseUser, null, enterpriseUser.getAuthorities()));

        when(enterpriseAdmissionService.getMyCertificate(1L)).thenReturn(List.of(testAdmission));

        mockMvc.perform(get("/admin/enterprise-admission/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].certificateNo").value("EA-20260515-123456"));

        verify(enterpriseAdmissionService).getMyCertificate(1L);
    }

    @Test
    @DisplayName("查看自身准入证书-无企业ID时返回错误")
    void testGetMyAdmission_noEnterpriseId_returns400() throws Exception {
        // Admin user has no enterpriseId, so controller throws BusinessException
        JwtUserDetails adminUser = JwtUserDetails.builder()
                .userId(10L)
                .username("admin")
                .roles(List.of("ADMIN"))
                .enabled(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities()));

        mockMvc.perform(get("/admin/enterprise-admission/my"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));

        verify(enterpriseAdmissionService, never()).getMyCertificate(any());
    }
}
