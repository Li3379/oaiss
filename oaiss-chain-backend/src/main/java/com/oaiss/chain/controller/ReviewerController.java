package com.oaiss.chain.controller;

import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.entity.CarbonReport;
import com.oaiss.chain.entity.Reviewer;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.ReviewerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 审核员控制器
 * 提供审核员专属功能，包括待审核报告、审核历史等
 *
 * @author OAISS Team
 */
@RestController
@RequestMapping("/reviewer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('REVIEWER')")
@Tag(name = "10. 审核员管理", description = "审核员专属功能，包括待审核报告、审核历史等")
public class ReviewerController {

    private final ReviewerService reviewerService;

    @GetMapping("/info")
    @Operation(summary = "获取审核员信息", description = "获取当前登录审核员的详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非审核员用户")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Reviewer> getReviewerInfo(
            @AuthenticationPrincipal JwtUserDetails currentUser) {
        Reviewer reviewer = reviewerService.getReviewerInfo(currentUser.getUserId());
        return ApiResponse.success(reviewer);
    }

    @GetMapping("/reports/pending")
    @Operation(summary = "获取待审核报告", description = "分页查询待审核的碳报告列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非审核员用户")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Page<CarbonReport>> getPendingReports(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size) {
        Page<CarbonReport> reports = reviewerService.getPendingReports(currentUser.getUserId(), page, size);
        return ApiResponse.success(reports);
    }

    @GetMapping("/history")
    @Operation(summary = "获取审核历史", description = "分页查询已审核的报告历史记录")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非审核员用户")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Page<CarbonReport>> getReviewHistory(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer size) {
        Page<CarbonReport> history = reviewerService.getReviewHistory(currentUser.getUserId(), page, size);
        return ApiResponse.success(history);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取审核统计", description = "获取审核员的审核统计数据")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非审核员用户")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Map<String, Object>> getStatistics(
            @AuthenticationPrincipal JwtUserDetails currentUser) {
        Map<String, Object> stats = reviewerService.getStatistics(currentUser.getUserId());
        return ApiResponse.success(stats);
    }
}
