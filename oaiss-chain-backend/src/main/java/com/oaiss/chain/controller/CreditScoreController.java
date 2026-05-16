package com.oaiss.chain.controller;

import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.dto.CreditDeductionRequest;
import jakarta.validation.Valid;
import com.oaiss.chain.dto.CreditEventResponse;
import com.oaiss.chain.dto.CreditScoreResponse;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.CreditScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 信誉评分控制器
 *
 * @author OAISS Team
 */
@RestController
@RequestMapping("/credit")
@RequiredArgsConstructor
@Tag(name = "05. 信誉评分管理", description = "企业信誉评分查询、扣分、加分、等级评估等信誉管理接口")
public class CreditScoreController {

    private final CreditScoreService creditScoreService;

    @GetMapping("/my-score")
    @Operation(
        summary = "获取当前用户企业的信誉分",
        description = "企业用户获取自己的信誉评分信息，包括总分、等级、状态等。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "查询成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CreditScoreResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "无权限，仅企业用户可访问"
        )
    })
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<CreditScoreResponse> getMyScore(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser) {
        return ApiResponse.success(creditScoreService.getScoreByUserId(currentUser.getUserId()));
    }

    @GetMapping("/history")
    @Operation(
        summary = "获取当前用户企业的信誉历史",
        description = "企业用户获取自己的信誉分变动记录。",
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
    public ApiResponse<Page<CreditEventResponse>> getMyCreditHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "事件类型筛选", example = "1")
            @RequestParam(required = false) Integer eventType,
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(creditScoreService.getCreditHistoryByUserId(currentUser.getUserId(), eventType, page, size));
    }

    @GetMapping("/ranking")
    @Operation(
        summary = "获取信誉排名列表",
        description = "获取企业信誉评分排名列表，支持分页。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "查询成功"
        )
    })
    public ApiResponse<Page<CreditScoreResponse>> getScoreRanking(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(creditScoreService.getScoreRanking(page, size));
    }

    @GetMapping("/{enterpriseId}")
    @Operation(
        summary = "查询企业信誉分",
        description = "获取指定企业的信誉评分信息，包括总分、等级、状态等。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "查询成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = CreditScoreResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "企业不存在"
        )
    })
    public ApiResponse<CreditScoreResponse> getScore(
            @Parameter(description = "企业ID", required = true, example = "1")
            @PathVariable Long enterpriseId) {
        return ApiResponse.success(creditScoreService.getScore(enterpriseId));
    }

    @GetMapping("/{enterpriseId}/history")
    @Operation(
        summary = "查询信誉事件历史", 
        description = "分页查询企业的信誉分变动记录，包括加分、扣分事件。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "查询成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "企业不存在"
        )
    })
    public ApiResponse<Page<CreditEventResponse>> getCreditHistory(
            @Parameter(description = "企业ID", required = true, example = "1")
            @PathVariable Long enterpriseId,
            @Parameter(description = "事件类型筛选", example = "1")
            @RequestParam(required = false) Integer eventType,
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(creditScoreService.getCreditHistory(enterpriseId, eventType, page, size));
    }

    @PostMapping("/deduct")
    @Operation(
        summary = "扣除信誉分", 
        description = "审核员或管理员对违规企业扣除信誉分。扣分可能导致企业被限制交易或冻结。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "扣分成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = CreditScoreResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "请求参数错误"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限，仅审核员和管理员可操作"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "企业不存在"
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public ApiResponse<CreditScoreResponse> deductPoints(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "扣分请求", required = true)
            @Valid @RequestBody CreditDeductionRequest request) {
        return ApiResponse.success(creditScoreService.deductPoints(
                request.getEnterpriseId(),
                request.getEventType(),
                request.getDescription(),
                currentUser.getUserId(),
                request.getRelatedReportId()));
    }

    @PostMapping("/bonus")
    @Operation(
        summary = "添加信誉奖励分", 
        description = "管理员为企业添加信誉奖励分。用于奖励诚信行为或特殊情况补偿。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "加分成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = CreditScoreResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "请求参数错误"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限，仅管理员可操作"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "企业不存在"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CreditScoreResponse> addBonus(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "企业ID", required = true, example = "1")
            @RequestParam Long enterpriseId,
            @Parameter(description = "奖励分数", required = true, example = "10")
            @RequestParam Integer points,
            @Parameter(description = "奖励原因", required = true, example = "积极参与碳交易")
            @RequestParam String description) {
        return ApiResponse.success(creditScoreService.addBonusPoints(
                enterpriseId, points, description, currentUser.getUserId()));
    }

    @PostMapping("/evaluate/{enterpriseId}")
    @Operation(
        summary = "重新评估信誉等级", 
        description = "管理员手动触发企业信誉等级评估。根据当前信誉分重新计算等级。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "评估完成",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = CreditScoreResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限，仅管理员可操作"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "企业不存在"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CreditScoreResponse> evaluateLevel(
            @Parameter(description = "企业ID", required = true, example = "1")
            @PathVariable Long enterpriseId) {
        return ApiResponse.success(creditScoreService.evaluateLevel(enterpriseId));
    }

    @GetMapping("/restricted")
    @Operation(
        summary = "查询被限制交易的企业", 
        description = "获取所有因信誉分过低而被限制交易的企业列表。",
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
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<CreditScoreResponse>> getRestrictedEnterprises() {
        return ApiResponse.success(creditScoreService.getRestrictedEnterprises());
    }

    @GetMapping("/frozen")
    @Operation(
        summary = "查询被冻结的企业", 
        description = "获取所有因信誉分严重不足而被冻结的企业列表。",
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
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<CreditScoreResponse>> getFrozenEnterprises() {
        return ApiResponse.success(creditScoreService.getFrozenEnterprises());
    }

    @GetMapping("/check-permission/{enterpriseId}")
    @Operation(
        summary = "检查交易权限", 
        description = "检查企业是否有交易权限。信誉分低于阈值的企业将被限制交易。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "检查完成"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "企业不存在"
        )
    })
    public ApiResponse<Boolean> checkTradePermission(
            @Parameter(description = "企业ID", required = true, example = "1")
            @PathVariable Long enterpriseId) {
        return ApiResponse.success(creditScoreService.checkTradePermission(enterpriseId));
    }
}
