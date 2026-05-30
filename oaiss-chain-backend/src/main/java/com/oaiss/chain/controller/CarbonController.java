package com.oaiss.chain.controller;

import com.oaiss.chain.annotation.AuditLog;
import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.dto.CarbonReportRequest;
import com.oaiss.chain.dto.CarbonReportResponse;
import com.oaiss.chain.dto.PowerGenerationCalculationRequest;
import com.oaiss.chain.dto.PowerGenerationCalculationResponse;
import com.oaiss.chain.dto.PowerGridCalculationRequest;
import com.oaiss.chain.dto.PowerGridCalculationResponse;
import com.oaiss.chain.dto.ReviewRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/carbon")
@RequiredArgsConstructor
@Tag(name = "03. Carbon Accounting", description = "Carbon report submission, review, certification, and formula calculations")
public class CarbonController {

    private final CarbonService carbonService;
    private final PowerGridFormulaService powerGridFormulaService;
    private final PowerGenerationFormulaService powerGenerationFormulaService;

    @PostMapping("/reports")
    @Operation(
            summary = "Create carbon report",
            description = "Enterprise users create draft carbon reports.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Report created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CarbonReportResponse.class))
            )
    })
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<CarbonReportResponse> createReport(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Valid @RequestBody CarbonReportRequest request) {
        return ApiResponse.success(carbonService.createReport(currentUser, request), "报告创建成功");
    }

    @PostMapping("/reports/{reportId}/submit")
    @Operation(
            summary = "Submit carbon report",
            description = "Submit a draft report for reviewer audit.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<CarbonReportResponse> submitReport(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @PathVariable Long reportId) {
        return ApiResponse.success(carbonService.submitReport(currentUser, reportId), "报告提交成功");
    }

    @GetMapping("/reports/{reportId}")
    @Operation(
            summary = "Get report detail",
            description = "Retrieve a carbon report by id.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ApiResponse<CarbonReportResponse> getReport(@PathVariable Long reportId) {
        return ApiResponse.success(carbonService.getReport(reportId));
    }

    @GetMapping("/reports")
    @Operation(
            summary = "List reports",
            description = "Paginated query for carbon reports.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'THIRD_PARTY', 'ENTERPRISE')")
    public ApiResponse<Page<CarbonReportResponse>> listReports(
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(carbonService.listReports(enterpriseId, status, keyword, page, size));
    }

    @GetMapping("/my-reports")
    @Operation(
            summary = "List my reports",
            description = "Enterprise users query their own reports.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<Page<CarbonReportResponse>> listMyReports(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String accountingPeriod,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(carbonService.listMyReports(currentUser, status, title, accountingPeriod, page, size));
    }

    @DeleteMapping("/reports/{reportId}")
    @Operation(
            summary = "Delete report",
            description = "Delete a draft report.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<Void> deleteReport(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @PathVariable Long reportId) {
        carbonService.deleteReport(currentUser, reportId);
        return ApiResponse.success();
    }

    @PostMapping("/review")
    @Operation(
            summary = "Review report",
            description = "Reviewer approves or rejects a submitted report.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PreAuthorize("hasRole('REVIEWER')")
    public ApiResponse<CarbonReportResponse> reviewReport(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success(carbonService.reviewReport(currentUser, request), "审核完成");
    }

    @PostMapping("/certify")
    @Operation(
            summary = "Certify report",
            description = "Admin certifies an approved report and pushes it on-chain or rejects certification.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CarbonReportResponse> certifyReport(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success(carbonService.certifyReport(currentUser, request), "认证完成");
    }

    @PostMapping("/calculate/power-grid")
    @Operation(
            summary = "Calculate power-grid emission",
            description = "Run the GB/T 32150-2015 power-grid formula.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PreAuthorize("hasRole('ENTERPRISE')")
    @AuditLog(module = "纰虫牳绠?", action = "CALCULATE_POWER_GRID_EMISSION")
    public ApiResponse<PowerGridCalculationResponse> calculatePowerGrid(
            @Valid @RequestBody PowerGridCalculationRequest request) {
        return ApiResponse.success(powerGridFormulaService.calculate(request));
    }

    @PostMapping("/calculate/power-generation")
    @Operation(
            summary = "Calculate power-generation emission",
            description = "Run the GB/T 32150-2015 power-generation formula.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PreAuthorize("hasRole('ENTERPRISE')")
    @AuditLog(module = "纰虫牳绠?", action = "CALCULATE_POWER_GENERATION_EMISSION")
    public ApiResponse<PowerGenerationCalculationResponse> calculatePowerGeneration(
            @Valid @RequestBody PowerGenerationCalculationRequest request) {
        return ApiResponse.success(powerGenerationFormulaService.calculate(request));
    }
}
