package com.oaiss.chain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.entity.User;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.repository.UserRepository;
import com.oaiss.chain.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminController Unit Tests
 * 管理后台控制器单元测试
 */
@WebMvcTest(value = AdminController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
                org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
        })
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        testUser1 = User.builder()
                .username("enterprise_user")
                .realName("企业用户")
                .phone("13800138001")
                .email("enterprise@example.com")
                .userType(1)
                .status(1)
                .build();
        testUser1.setId(1L);

        testUser2 = User.builder()
                .username("reviewer_user")
                .realName("审核员")
                .phone("13800138002")
                .email("reviewer@example.com")
                .userType(2)
                .status(1)
                .build();
        testUser2.setId(2L);

        testUser3 = User.builder()
                .username("disabled_user")
                .realName("禁用用户")
                .phone("13800138003")
                .email("disabled@example.com")
                .userType(1)
                .status(0)
                .build();
        testUser3.setId(3L);
    }

    // ==================== List Users Tests ====================

    @Test
    @DisplayName("查询用户列表-成功返回所有用户")
    void testListUsersSuccess() throws Exception {
        // Given
        List<User> users = List.of(testUser1, testUser2, testUser3);
        Page<User> userPage = new PageImpl<>(users);
        when(userRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/admin/users")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].username").value("enterprise_user"))
                .andExpect(jsonPath("$.data.content[1].username").value("reviewer_user"))
                .andExpect(jsonPath("$.data.content[2].username").value("disabled_user"));

        verify(userRepository, times(1)).findByDeletedFalse(any(Pageable.class));
    }

    @Test
    @DisplayName("查询用户列表-按用户类型筛选")
    void testListUsersFilterByUserType() throws Exception {
        // Given
        List<User> enterpriseUsers = List.of(testUser1, testUser3);
        Page<User> userPage = new PageImpl<>(enterpriseUsers);
        when(userRepository.findByUserTypeAndDeletedFalse(eq(1), any(Pageable.class))).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/admin/users")
                        .param("userType", "1")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));

        verify(userRepository, times(1)).findByUserTypeAndDeletedFalse(eq(1), any(Pageable.class));
        verify(userRepository, never()).findByStatusAndDeletedFalse(any(), any());
        verify(userRepository, never()).findByDeletedFalse(any());
    }

    @Test
    @DisplayName("查询用户列表-按状态筛选")
    void testListUsersFilterByStatus() throws Exception {
        // Given
        List<User> activeUsers = List.of(testUser1, testUser2);
        Page<User> userPage = new PageImpl<>(activeUsers);
        when(userRepository.findByStatusAndDeletedFalse(eq(1), any(Pageable.class))).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/admin/users")
                        .param("status", "1")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));

        verify(userRepository, times(1)).findByStatusAndDeletedFalse(eq(1), any(Pageable.class));
        verify(userRepository, never()).findByUserTypeAndDeletedFalse(any(), any());
        verify(userRepository, never()).findByDeletedFalse(any());
    }

    @Test
    @DisplayName("查询用户列表-分页功能正常")
    void testListUsersPagination() throws Exception {
        // Given
        List<User> users = List.of(testUser1);
        Page<User> userPage = new PageImpl<>(users);
        when(userRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(userPage);

        // When & Then - 验证分页参数
        mockMvc.perform(get("/admin/users")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray());

        verify(userRepository, times(1)).findByDeletedFalse(any(Pageable.class));
    }

    @Test
    @DisplayName("查询用户列表-空列表")
    void testListUsersEmptyList() throws Exception {
        // Given
        Page<User> emptyPage = new PageImpl<>(List.of());
        when(userRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/admin/users")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        verify(userRepository, times(1)).findByDeletedFalse(any(Pageable.class));
    }

    @Test
    @DisplayName("查询用户列表-默认分页参数")
    void testListUsersDefaultPagination() throws Exception {
        // Given
        List<User> users = List.of(testUser1, testUser2);
        Page<User> userPage = new PageImpl<>(users);
        when(userRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(userPage);

        // When & Then - 不传分页参数，使用默认值
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray());

        verify(userRepository, times(1)).findByDeletedFalse(any(Pageable.class));
    }

    // ==================== Update User Status Tests ====================

    @Test
    @DisplayName("更新用户状态-成功启用用户")
    void testUpdateUserStatusSuccessEnable() throws Exception {
        // Given
        User userToEnable = User.builder()
                .username("disabled_user")
                .status(0)
                .build();
        userToEnable.setId(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(userToEnable));
        when(userRepository.save(any(User.class))).thenReturn(userToEnable);

        // When & Then
        mockMvc.perform(put("/admin/users/3/status")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userRepository, times(1)).findById(3L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("更新用户状态-成功禁用用户")
    void testUpdateUserStatusSuccessDisable() throws Exception {
        // Given
        User userToDisable = User.builder()
                .username("active_user")
                .status(1)
                .build();
        userToDisable.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(userToDisable));
        when(userRepository.save(any(User.class))).thenReturn(userToDisable);

        // When & Then
        mockMvc.perform(put("/admin/users/1/status")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("更新用户状态-用户不存在")
    void testUpdateUserStatusUserNotFound() throws Exception {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then - GlobalExceptionHandler returns 404 for RESOURCE_NOT_FOUND
        mockMvc.perform(put("/admin/users/999/status")
                        .param("status", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND));

        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any());
    }

    // ==================== Dashboard Tests ====================

    @Test
    @DisplayName("获取仪表板数据-成功")
    void testGetDashboardSuccess() throws Exception {
        // Given
        when(userRepository.count()).thenReturn(100L);
        Page<User> activePage = new PageImpl<>(List.of(testUser1, testUser2));
        when(userRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(activePage);

        // When & Then
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalUsers").value(100))
                .andExpect(jsonPath("$.data.activeUsers").value(2));

        verify(userRepository, times(1)).count();
        verify(userRepository, times(1)).findByDeletedFalse(any(Pageable.class));
    }

    @Test
    @DisplayName("获取仪表板数据-无用户")
    void testGetDashboardNoUsers() throws Exception {
        // Given
        when(userRepository.count()).thenReturn(0L);
        Page<User> emptyPage = new PageImpl<>(List.of());
        when(userRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalUsers").value(0))
                .andExpect(jsonPath("$.data.activeUsers").value(0));

        verify(userRepository, times(1)).count();
        verify(userRepository, times(1)).findByDeletedFalse(any(Pageable.class));
    }

    // ==================== Statistics Tests ====================

    @Test
    @DisplayName("获取系统统计数据-成功")
    void testGetStatisticsSuccess() throws Exception {
        // Given
        when(userRepository.count()).thenReturn(100L);

        // 企业用户
        Page<User> enterprisePage = new PageImpl<>(List.of(testUser1));
        when(userRepository.findByUserTypeAndDeletedFalse(eq(1), any(Pageable.class))).thenReturn(enterprisePage);

        // 审核员
        Page<User> reviewerPage = new PageImpl<>(List.of(testUser2));
        when(userRepository.findByUserTypeAndDeletedFalse(eq(2), any(Pageable.class))).thenReturn(reviewerPage);

        // 第三方
        Page<User> thirdPartyPage = new PageImpl<>(List.of());
        when(userRepository.findByUserTypeAndDeletedFalse(eq(3), any(Pageable.class))).thenReturn(thirdPartyPage);

        // When & Then
        mockMvc.perform(get("/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalUsers").value(100))
                .andExpect(jsonPath("$.data.enterpriseCount").value(1))
                .andExpect(jsonPath("$.data.reviewerCount").value(1))
                .andExpect(jsonPath("$.data.thirdPartyCount").value(0));

        verify(userRepository, times(1)).count();
        verify(userRepository, times(1)).findByUserTypeAndDeletedFalse(eq(1), any(Pageable.class));
        verify(userRepository, times(1)).findByUserTypeAndDeletedFalse(eq(2), any(Pageable.class));
        verify(userRepository, times(1)).findByUserTypeAndDeletedFalse(eq(3), any(Pageable.class));
    }

    @Test
    @DisplayName("获取系统统计数据-无用户数据")
    void testGetStatisticsNoUsers() throws Exception {
        // Given
        when(userRepository.count()).thenReturn(0L);

        Page<User> emptyPage = new PageImpl<>(List.of());
        when(userRepository.findByUserTypeAndDeletedFalse(any(Integer.class), any(Pageable.class))).thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalUsers").value(0))
                .andExpect(jsonPath("$.data.enterpriseCount").value(0))
                .andExpect(jsonPath("$.data.reviewerCount").value(0))
                .andExpect(jsonPath("$.data.thirdPartyCount").value(0));

        verify(userRepository, times(1)).count();
    }

    @Test
    @DisplayName("获取系统统计数据-大量用户")
    void testGetStatisticsLargeNumbers() throws Exception {
        // Given
        when(userRepository.count()).thenReturn(10000L);

        // 创建包含多个用户的页面
        List<User> manyUsers = List.of(testUser1, testUser2, testUser3);
        Page<User> userPage = new PageImpl<>(manyUsers);
        when(userRepository.findByUserTypeAndDeletedFalse(any(Integer.class), any(Pageable.class))).thenReturn(userPage);

        // When & Then
        mockMvc.perform(get("/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalUsers").value(10000));

        verify(userRepository, times(1)).count();
    }
}
