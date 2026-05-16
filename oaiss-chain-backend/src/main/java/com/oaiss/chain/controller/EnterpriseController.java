package com.oaiss.chain.controller;

import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.entity.Enterprise;
import com.oaiss.chain.entity.EnterpriseAdmission;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.EnterpriseAdmissionService;
import com.oaiss.chain.service.EnterpriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 企业用户控制器
 * 提供企业信息查询、配额管理等企业专属功能
 *
 * @author OAISS Team
 */
@RestController
@RequestMapping("/enterprise")
@RequiredArgsConstructor
@Tag(name = "17. 企业用户管理", description = "企业用户专属功能，包括企业信息、配额管理等")
public class EnterpriseController {

    private final EnterpriseService enterpriseService;
    private final EnterpriseAdmissionService enterpriseAdmissionService;

    @GetMapping("/admission/my")
    @PreAuthorize("hasRole('ENTERPRISE')")
    @Operation(summary = "查看自身准入证书", description = "企业用户查看自身的准入证书列表。")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限，仅企业用户可访问")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<List<EnterpriseAdmission>> getMyAdmission(
            @AuthenticationPrincipal JwtUserDetails currentUser) {
        Long enterpriseId = currentUser.getEnterpriseId();
        if (enterpriseId == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "error.user.notEnterprise");
        }
        List<EnterpriseAdmission> admissions = enterpriseAdmissionService.getMyCertificate(enterpriseId);
        return ApiResponse.success(admissions);
    }

    @GetMapping("/info")
    @PreAuthorize("hasRole('ENTERPRISE')")
    @Operation(summary = "获取企业信息", description = "获取当前登录企业的详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非企业用户")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Enterprise> getEnterpriseInfo(
            @AuthenticationPrincipal JwtUserDetails currentUser) {
        Enterprise enterprise = enterpriseService.getEnterpriseInfo(currentUser.getUserId());
        return ApiResponse.success(enterprise);
    }

    @GetMapping("/quota")
    @PreAuthorize("hasRole('ENTERPRISE')")
    @Operation(summary = "获取碳配额信息", description = "获取企业的碳配额详情，包括总额度、已用额度、可交易额度等")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非企业用户")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Map<String, Object>> getQuotaInfo(
            @AuthenticationPrincipal JwtUserDetails currentUser) {
        Map<String, Object> quotaInfo = enterpriseService.getQuotaInfo(currentUser.getUserId());
        return ApiResponse.success(quotaInfo);
    }

    @PutMapping("/contact")
    @PreAuthorize("hasRole('ENTERPRISE')")
    @Operation(summary = "更新联系方式", description = "更新企业的联系人和联系电话")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非企业用户")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Void> updateContact(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "联系人姓名", example = "张三") @RequestParam(required = false) String contactPerson,
            @Parameter(description = "联系电话", example = "13800138000") @RequestParam(required = false) String contactPhone) {
        enterpriseService.updateContact(currentUser.getUserId(), contactPerson, contactPhone);
        return ApiResponse.success(null, "联系方式更新成功");
    }

    @GetMapping("/{enterpriseId}")
    @Operation(summary = "获取指定企业信息", description = "根据企业ID获取企业公开信息（需要权限）")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "企业不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Enterprise> getEnterpriseById(
            @Parameter(description = "企业ID", required = true, example = "1") @PathVariable Long enterpriseId) {
        Enterprise enterprise = enterpriseService.getEnterpriseById(enterpriseId);
        return ApiResponse.success(enterprise);
    }
}
