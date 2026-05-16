package com.oaiss.chain.controller;

import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.constant.ErrorMessage;
import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.entity.AccountPermissionList;
import com.oaiss.chain.entity.EnterpriseAdmission;
import com.oaiss.chain.entity.ReviewerQualification;
import com.oaiss.chain.entity.User;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.repository.AccountPermissionListRepository;
import com.oaiss.chain.repository.UserRepository;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.EnterpriseAdmissionService;
import com.oaiss.chain.service.ReviewerQualificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台控制器
 * 
 * @author OAISS Team
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "16. 管理后台", description = "管理员后台管理接口，包括用户管理、系统监控、数据统计等")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AccountPermissionListRepository permissionRepository;
    private final EnterpriseAdmissionService enterpriseAdmissionService;
    private final ReviewerQualificationService reviewerQualificationService;

    @GetMapping("/users")
    @Operation(
        summary = "查询用户列表", 
        description = "分页查询系统用户列表。支持按用户类型、状态筛选。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "查询成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限，仅管理员可访问"
        )
    })
    public ApiResponse<Page<User>> listUsers(
            @Parameter(description = "用户类型筛选 (1-企业, 2-审核员, 3-第三方, 4-管理员)", example = "1")
            @RequestParam(required = false) Integer userType,
            @Parameter(description = "状态筛选 (0-禁用, 1-启用)", example = "1")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码，从1开始", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> users;
        if (userType != null) {
            users = userRepository.findByUserTypeAndDeletedFalse(userType, pageable);
        } else if (status != null) {
            users = userRepository.findByStatusAndDeletedFalse(status, pageable);
        } else {
            users = userRepository.findByDeletedFalse(pageable);
        }

        return ApiResponse.success(users);
    }

    @PutMapping("/users/{userId}/status")
    @Operation(
        summary = "更新用户状态", 
        description = "启用或禁用指定用户账号。禁用后用户将无法登录系统。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "更新成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "用户不存在"
        )
    })
    public ApiResponse<Void> updateUserStatus(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "目标状态 (0-禁用, 1-启用)", required = true, example = "1")
            @RequestParam Integer status,
            @AuthenticationPrincipal JwtUserDetails currentUser) {

        // 防止管理员禁用自己的账号
        if (currentUser != null && currentUser.getUserId().equals(userId) && status == 0) {
            throw new BusinessException(ErrorCode.CANNOT_DISABLE_SELF, ErrorMessage.CANNOT_DISABLE_SELF);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));

        user.setStatus(status);
        userRepository.save(user);

        return ApiResponse.success();
    }

    @GetMapping("/dashboard")
    @Operation(
        summary = "获取仪表板数据", 
        description = "获取管理后台仪表板的统计数据，包括用户数、活跃用户等。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "获取成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限"
        )
    })
    public ApiResponse<java.util.Map<String, Object>> getDashboard() {
        java.util.Map<String, Object> dashboard = new java.util.HashMap<>();
        dashboard.put("totalUsers", userRepository.count());
        dashboard.put("activeUsers", userRepository.findByDeletedFalse(
                PageRequest.of(0, 1)).getTotalElements());
        return ApiResponse.success(dashboard);
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "获取系统统计数据", 
        description = "获取系统运营统计数据，包括各类型用户数量、注册趋势等。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "获取成功"
        )
    })
    public ApiResponse<java.util.Map<String, Object>> getStatistics() {
        java.util.Map<String, Object> statistics = new java.util.HashMap<>();
        
        // 用户统计
        statistics.put("totalUsers", userRepository.count());
        statistics.put("enterpriseCount", userRepository.findByUserTypeAndDeletedFalse(1, PageRequest.of(0, 1)).getTotalElements());
        statistics.put("reviewerCount", userRepository.findByUserTypeAndDeletedFalse(2, PageRequest.of(0, 1)).getTotalElements());
        statistics.put("thirdPartyCount", userRepository.findByUserTypeAndDeletedFalse(3, PageRequest.of(0, 1)).getTotalElements());
        
        return ApiResponse.success(statistics);
    }

    @GetMapping("/config")
    @Operation(
        summary = "获取系统配置",
        description = "获取系统运行配置参数。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限")
    })
    public ApiResponse<java.util.Map<String, Object>> getConfig() {
        java.util.Map<String, Object> config = new java.util.HashMap<>();
        config.put("systemName", "OAISS CHAIN 双碳链动系统");
        config.put("version", "1.0.0");
        config.put("maxUploadSize", "10MB");
        config.put("sessionTimeout", 3600);
        config.put("enableCaptcha", true);
        config.put("enableBlockChain", true);
        return ApiResponse.success(config);
    }

    @GetMapping("/permissions")
    @Operation(
        summary = "获取权限列表",
        description = "获取系统所有权限定义列表。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限")
    })
    public ApiResponse<java.util.List<AccountPermissionList>> getPermissions() {
        java.util.List<AccountPermissionList> permissions = permissionRepository.findByDeletedFalseOrderBySortOrderAsc();
        return ApiResponse.success(permissions);
    }

    // ==================== 企业准入证书管理 ====================

    @PostMapping("/enterprise-admission/{enterpriseId}/issue")
    @Operation(
        summary = "签发企业准入证书",
        description = "为指定企业签发准入证书。同一企业已有有效证书时拒绝重复签发。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "签发成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "企业不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "已有有效证书")
    })
    public ApiResponse<EnterpriseAdmission> issueAdmission(
            @Parameter(description = "企业ID", required = true, example = "1")
            @PathVariable Long enterpriseId) {
        EnterpriseAdmission admission = enterpriseAdmissionService.issueCertificate(enterpriseId);
        return ApiResponse.success(admission);
    }

    @DeleteMapping("/enterprise-admission/{enterpriseId}")
    @Operation(
        summary = "吊销企业准入证书",
        description = "吊销指定企业的有效准入证书。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "吊销成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "无有效证书")
    })
    public ApiResponse<Void> revokeAdmission(
            @Parameter(description = "企业ID", required = true, example = "1")
            @PathVariable Long enterpriseId) {
        enterpriseAdmissionService.revokeCertificate(enterpriseId);
        return ApiResponse.success();
    }

    @GetMapping("/enterprise-admission")
    @Operation(
        summary = "查询准入证书列表",
        description = "分页查询企业准入证书列表。支持按状态筛选。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限")
    })
    public ApiResponse<Page<EnterpriseAdmission>> listAdmissions(
            @Parameter(description = "状态筛选 (1-有效, 2-已吊销)", example = "1")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码，从1开始", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        Page<EnterpriseAdmission> admissions = enterpriseAdmissionService.listCertificates(status, page, size);
        return ApiResponse.success(admissions);
    }

    // ==================== 审核员资格证管理 ====================

    @PostMapping("/reviewer-qualification/{reviewerId}/issue")
    @Operation(
        summary = "签发审核员资格证",
        description = "为指定审核员签发资格证。同一审核员已有有效资格证时拒绝重复签发。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "签发成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "已有有效资格证")
    })
    public ApiResponse<ReviewerQualification> issueQualification(
            @Parameter(description = "审核员ID", required = true, example = "1")
            @PathVariable Long reviewerId) {
        ReviewerQualification qualification = reviewerQualificationService.issueCertificate(reviewerId);
        return ApiResponse.success(qualification);
    }

    @DeleteMapping("/reviewer-qualification/{reviewerId}")
    @Operation(
        summary = "吊销审核员资格证",
        description = "吊销指定审核员的有效资格证。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "吊销成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "无有效资格证")
    })
    public ApiResponse<Void> revokeQualification(
            @Parameter(description = "审核员ID", required = true, example = "1")
            @PathVariable Long reviewerId) {
        reviewerQualificationService.revokeCertificate(reviewerId);
        return ApiResponse.success();
    }

    @GetMapping("/reviewer-qualification")
    @Operation(
        summary = "查询审核员资格证列表",
        description = "分页查询审核员资格证列表。支持按状态筛选。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限")
    })
    public ApiResponse<Page<ReviewerQualification>> listQualifications(
            @Parameter(description = "状态筛选 (1-有效, 2-已吊销)", example = "1")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码，从1开始", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        Page<ReviewerQualification> qualifications = reviewerQualificationService.listCertificates(status, page, size);
        return ApiResponse.success(qualifications);
    }
}
