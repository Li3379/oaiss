package com.oaiss.chain.controller;

import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.dto.CarbonNeutralProjectRequest;
import com.oaiss.chain.dto.CarbonNeutralProjectResponse;
import com.oaiss.chain.dto.ProjectVerificationRequest;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.CarbonNeutralProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 碳中和项目控制器
 * 提供碳减排、碳汇、CCUS等碳中和项目的完整生命周期管理
 * <p>
 * 项目生命周期：
 * 1. 创建申请 → 2. 审核通过 → 3. 项目实施 → 4. 减排核证 → 5. 碳信用签发 → 6. 持续监测
 *
 * @author OAISS Team
 */
@RestController
@RequestMapping("/carbon-neutral")
@RequiredArgsConstructor
@Tag(name = "11. 碳中和项目管理", description = "碳减排、碳汇、CCUS等碳中和项目的创建、审核、认证、追踪")
public class CarbonNeutralProjectController {

    private final CarbonNeutralProjectService projectService;

    @PostMapping
    @Operation(summary = "创建项目", description = "创建新的碳中和项目（草稿状态），包括项目基本信息、类型、预计减排量等")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "项目创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "项目信息无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<CarbonNeutralProjectResponse> create(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Valid @RequestBody CarbonNeutralProjectRequest request) {
        return ApiResponse.success(projectService.createProject(currentUser, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新项目", description = "更新项目信息，仅草稿或被拒绝状态的项目可修改")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "项目状态不允许修改"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<CarbonNeutralProjectResponse> update(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody CarbonNeutralProjectRequest request) {
        return ApiResponse.success(projectService.updateProject(currentUser, id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取项目详情", description = "获取指定项目的详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<CarbonNeutralProjectResponse> get(
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable Long id) {
        return ApiResponse.success(projectService.getProject(id));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索项目", description = "根据项目类型、状态、关键字搜索碳中和项目，支持分页")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数无效")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Page<CarbonNeutralProjectResponse>> search(
            @Parameter(description = "项目类型：1-碳汇, 2-CCUS, 3-可再生能源, 4-节能改造, 5-其他", example = "1")
            @RequestParam(required = false) Integer projectType,
            @Parameter(description = "项目状态：0-筹备, 1-待审核, 2-审核通过, 3-实施中, 4-已完成, 5-已终止, 6-审核拒绝", example = "3")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键字（项目名称/编号）", example = "林业碳汇")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "页码（从1开始）", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(projectService.searchProjects(projectType, status, keyword, page, size));
    }

    @GetMapping("/projects")
    @Operation(summary = "获取项目列表", description = "获取碳中和项目列表（别名端点），支持分页")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数无效")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Page<CarbonNeutralProjectResponse>> listProjects(
            @Parameter(description = "项目类型", example = "1")
            @RequestParam(required = false) Integer projectType,
            @Parameter(description = "项目状态", example = "3")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键字", example = "林业碳汇")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "页码（从1开始）", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(projectService.searchProjects(projectType, status, keyword, page, size));
    }

    @GetMapping("/my")
    @Operation(summary = "我的项目", description = "获取当前用户企业的所有碳中和项目列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Page<CarbonNeutralProjectResponse>> getMyProjects(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "项目状态过滤") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码（从1开始）", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(projectService.getMyProjects(currentUser, status, page, size));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审核", description = "将项目提交给管理员审核")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "提交成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "项目状态不允许提交"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<CarbonNeutralProjectResponse> submitForReview(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable Long id) {
        return ApiResponse.success(projectService.submitForReview(currentUser, id));
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "审核项目", description = "管理员审核项目，通过或拒绝")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "审核成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "项目不在待审核状态"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN') or hasRole('REVIEWER')")
    public ApiResponse<CarbonNeutralProjectResponse> review(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String comment = (String) body.get("comment");
        return ApiResponse.success(projectService.reviewProject(currentUser, id, approved, comment));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "启动实施", description = "审核通过后启动项目实施")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "启动成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "项目状态不允许启动"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<CarbonNeutralProjectResponse> startImplementation(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable Long id) {
        return ApiResponse.success(projectService.startImplementation(currentUser, id));
    }

    @PostMapping("/{id}/submit-verification")
    @Operation(summary = "申请核证", description = "提交项目给第三方核证机构进行减排量核证")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "申请成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "项目状态不允许核证"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<CarbonNeutralProjectResponse> submitForVerification(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        Long verifierId = body.get("verifierId");
        return ApiResponse.success(projectService.submitForVerification(currentUser, id, verifierId));
    }

    @PostMapping("/verify")
    @Operation(summary = "核证项目", description = "第三方核证机构对项目减排量进行核证")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "核证成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "项目不在待核证状态"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('REVIEWER') or hasRole('ADMIN')")
    public ApiResponse<CarbonNeutralProjectResponse> verify(
            @AuthenticationPrincipal JwtUserDetails verifier,
            @Valid @RequestBody ProjectVerificationRequest request) {
        return ApiResponse.success(projectService.verifyProject(verifier, request));
    }

    @PostMapping("/{id}/use-credits")
    @Operation(summary = "使用碳信用", description = "消耗项目已签发的碳信用")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "使用成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "可用碳信用不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<CarbonNeutralProjectResponse> useCredits(
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable Long id,
            @RequestBody Map<String, java.math.BigDecimal> body) {
        java.math.BigDecimal amount = body.get("amount");
        return ApiResponse.success(projectService.useCredits(id, amount));
    }

    @PutMapping("/{id}/monitoring")
    @Operation(summary = "更新监测数据", description = "更新项目监测数据")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<CarbonNeutralProjectResponse> updateMonitoring(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String monitoringData = body.get("monitoringData");
        return ApiResponse.success(projectService.updateMonitoring(currentUser, id, monitoringData));
    }

    @PostMapping("/{id}/apply-certification")
    @Operation(summary = "申请认证", description = "向认证机构申请项目认证")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "申请成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "项目需先完成核证"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<CarbonNeutralProjectResponse> applyForCertification(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String certOrg = body.get("certOrg");
        return ApiResponse.success(projectService.applyForCertification(currentUser, id, certOrg));
    }

    @PostMapping("/{id}/certify")
    @Operation(summary = "完成认证", description = "认证机构完成项目认证")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "认证成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "项目不在待认证状态"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('REVIEWER') or hasRole('ADMIN')")
    public ApiResponse<CarbonNeutralProjectResponse> completeCertification(
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String certNo = body.get("certNo");
        return ApiResponse.success(projectService.completeCertification(id, certNo));
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "终止项目", description = "终止项目实施")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "终止成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "项目状态不允许终止"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "项目不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<CarbonNeutralProjectResponse> terminate(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "项目ID", required = true, example = "1") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        return ApiResponse.success(projectService.terminateProject(currentUser, id, reason));
    }

    @GetMapping("/pending-verification")
    @Operation(summary = "待核证项目", description = "获取分配给当前核证机构的待核证项目列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('REVIEWER') or hasRole('ADMIN')")
    public ApiResponse<Page<CarbonNeutralProjectResponse>> getPendingVerification(
            @AuthenticationPrincipal JwtUserDetails verifier,
            @Parameter(description = "页码（从1开始）", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(projectService.getPendingVerificationProjects(verifier.getUserId(), page, size));
    }
}
