package com.oaiss.chain.controller;

import com.oaiss.chain.annotation.AuditLog;
import com.oaiss.chain.dto.*;
import com.oaiss.chain.enums.ReportStatusEnum;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.CarbonService;
import com.oaiss.chain.service.PowerGenerationFormulaService;
import com.oaiss.chain.service.PowerGridFormulaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 碳核算控制器
 * 
 * @author OAISS Team
 */
@RestController
@RequestMapping("/carbon")
@RequiredArgsConstructor
@Tag(name = "03. 碳核算管理", description = "碳报告提交、核算、审核、查询等碳核算相关接口")
public class CarbonController {

    private final CarbonService carbonService;
    private final PowerGridFormulaService powerGridFormulaService;
    private final PowerGenerationFormulaService powerGenerationFormulaService;

    @PostMapping("/reports")
    @Operation(
        summary = "创建碳报告", 
        description = "企业用户创建碳核算报告，初始状态为草稿。需要填写排放数据、核算周期等信息。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "报告创建成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = CarbonReportResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "请求参数错误"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限，仅企业用户可创建"
        )
    })
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<CarbonReportResponse> createReport(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "碳报告创建请求", required = true)
            @Valid @RequestBody CarbonReportRequest request) {
        return ApiResponse.success(carbonService.createReport(currentUser, request), "报告创建成功");
    }

    @PostMapping("/reports/{reportId}/submit")
    @Operation(
        summary = "提交碳报告", 
        description = "将草稿状态的碳报告提交至审核流程。提交后报告状态变更为待审核。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "报告提交成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = CarbonReportResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "报告状态不允许提交"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限提交此报告"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "报告不存在"
        )
    })
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<CarbonReportResponse> submitReport(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "报告ID", required = true, example = "1")
            @PathVariable Long reportId) {
        return ApiResponse.success(carbonService.submitReport(currentUser, reportId), "报告提交成功");
    }

    @GetMapping("/reports/{reportId}")
    @Operation(
        summary = "获取报告详情", 
        description = "根据报告ID获取碳报告的完整信息，包括排放数据、审核状态等。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "获取成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = CarbonReportResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "报告不存在"
        )
    })
    public ApiResponse<CarbonReportResponse> getReport(
            @Parameter(description = "报告ID", required = true, example = "1")
            @PathVariable Long reportId) {
        return ApiResponse.success(carbonService.getReport(reportId));
    }

    @GetMapping("/reports")
    @Operation(
        summary = "查询报告列表", 
        description = "分页查询碳报告列表。支持按企业、状态、关键词筛选。管理员、审核员、第三方可访问。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "查询成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限访问"
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'THIRD_PARTY', 'ENTERPRISE')")
    public ApiResponse<Page<CarbonReportResponse>> listReports(
            @Parameter(description = "企业ID筛选", example = "1")
            @RequestParam(required = false) Long enterpriseId,
            @Parameter(description = "报告状态筛选 (0-草稿, 1-待审核, 2-已通过, 3-已驳回)", example = "1")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "关键词搜索", example = "2024")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "页码，从1开始", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(carbonService.listReports(enterpriseId, status, keyword, page, size));
    }

    @GetMapping("/my-reports")
    @Operation(
        summary = "查询我的报告", 
        description = "企业用户查询自己创建的碳报告列表。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "查询成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限，仅企业用户可访问"
        )
    })
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<Page<CarbonReportResponse>> listMyReports(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "报告状态筛选", example = "1")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(carbonService.listMyReports(currentUser, status, page, size));
    }

    @DeleteMapping("/reports/{reportId}")
    @Operation(
        summary = "删除报告", 
        description = "删除草稿状态的碳报告。已提交的报告不可删除。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "删除成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "报告状态不允许删除"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限删除此报告"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "报告不存在"
        )
    })
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<Void> deleteReport(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "报告ID", required = true, example = "1")
            @PathVariable Long reportId) {
        carbonService.deleteReport(currentUser, reportId);
        return ApiResponse.success();
    }

    // ==================== 审核接口 ====================

    @PostMapping("/review")
    @Operation(
        summary = "审核碳报告", 
        description = "审核员对碳报告进行审核，可选择通过或驳回。需要填写审核意见。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "审核完成",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = CarbonReportResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "请求参数错误"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限，仅审核员可操作"
        )
    })
    @PreAuthorize("hasRole('REVIEWER')")
    public ApiResponse<CarbonReportResponse> reviewReport(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "审核请求", required = true)
            @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success(carbonService.reviewReport(currentUser, request), "审核完成");
    }

    // ==================== 碳排放计算接口 ====================

    @PostMapping("/calculate/power-grid")
    @Operation(
        summary = "电网碳排放计算",
        description = "基于 GB/T 32150-2015 标准，9参数电网碳排放计算。仅企业用户可调用。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PreAuthorize("hasRole('ENTERPRISE')")
    @AuditLog(module = "碳核算", action = "CALCULATE_POWER_GRID_EMISSION")
    public ApiResponse<PowerGridCalculationResponse> calculatePowerGrid(
            @Valid @RequestBody PowerGridCalculationRequest request) {
        return ApiResponse.success(powerGridFormulaService.calculate(request));
    }

    @PostMapping("/calculate/power-generation")
    @Operation(
        summary = "发电企业碳排放计算",
        description = "基于 GB/T 32150-2015 标准，25参数发电企业碳排放计算。仅企业用户可调用。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PreAuthorize("hasRole('ENTERPRISE')")
    @AuditLog(module = "碳核算", action = "CALCULATE_POWER_GENERATION_EMISSION")
    public ApiResponse<PowerGenerationCalculationResponse> calculatePowerGeneration(
            @Valid @RequestBody PowerGenerationCalculationRequest request) {
        return ApiResponse.success(powerGenerationFormulaService.calculate(request));
    }
}
