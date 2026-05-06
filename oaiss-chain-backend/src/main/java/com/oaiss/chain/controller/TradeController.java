package com.oaiss.chain.controller;

import com.oaiss.chain.dto.*;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.TradeService;
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
 * 碳交易控制器
 * 
 * @author OAISS Team
 */
@RestController
@RequestMapping("/trade")
@RequiredArgsConstructor
@Tag(name = "04. 碳交易管理", description = "P2P交易、拍卖挂单、交易确认、交易查询等碳交易相关接口")
public class TradeController {

    private final TradeService tradeService;

    @PostMapping("/p2p")
    @Operation(
        summary = "创建P2P交易", 
        description = "创建点对点碳配额交易。卖方发起交易，指定买方和交易数量、价格。需要审核通过后生效。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "交易创建成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = TradeResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "请求参数错误或碳配额不足"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限，仅企业用户可创建"
        )
    })
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<TradeResponse> createP2PTrade(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "P2P交易请求", required = true)
            @Valid @RequestBody TradeRequest request) {
        return ApiResponse.success(tradeService.createP2PTrade(currentUser, request), "交易创建成功");
    }

    @PostMapping("/auction")
    @Operation(
        summary = "创建拍卖挂单", 
        description = "创建碳配额拍卖挂单。卖方挂出碳配额，买方竞价购买。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "挂单创建成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = TradeResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "请求参数错误或碳配额不足"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限，仅企业用户可创建"
        )
    })
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<TradeResponse> createAuctionOrder(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "拍卖挂单请求", required = true)
            @Valid @RequestBody TradeRequest request) {
        return ApiResponse.success(tradeService.createAuctionOrder(currentUser, request), "挂单创建成功");
    }

    @PostMapping("/{tradeId}/confirm")
    @Operation(
        summary = "确认交易", 
        description = "确认并完成碳交易。交易双方确认后，碳配额和资金完成交割。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "交易完成",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = TradeResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "交易状态不允许确认"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "交易不存在"
        )
    })
    @PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")
    public ApiResponse<TradeResponse> confirmTrade(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "交易ID", required = true, example = "1")
            @PathVariable Long tradeId) {
        return ApiResponse.success(tradeService.confirmTrade(tradeId, currentUser.getUserId()), "交易完成");
    }

    @PostMapping("/{tradeId}/cancel")
    @Operation(
        summary = "取消交易", 
        description = "取消待处理的碳交易。仅交易发起方可取消。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "交易已取消",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = TradeResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "交易状态不允许取消"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "无权限取消此交易"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "交易不存在"
        )
    })
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<TradeResponse> cancelTrade(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "交易ID", required = true, example = "1")
            @PathVariable Long tradeId) {
        return ApiResponse.success(tradeService.cancelTrade(tradeId, currentUser), "交易已取消");
    }

    @GetMapping("/{tradeId}")
    @Operation(
        summary = "获取交易详情", 
        description = "根据交易ID获取碳交易的完整信息，包括交易双方、配额数量、价格、状态等。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "获取成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = TradeResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "交易不存在"
        )
    })
    public ApiResponse<TradeResponse> getTrade(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "交易ID", required = true, example = "1")
            @PathVariable Long tradeId) {
        return ApiResponse.success(tradeService.getTrade(tradeId, currentUser));
    }

    @GetMapping("/list")
    @Operation(
        summary = "查询交易列表", 
        description = "分页查询碳交易记录。管理员、审核员、第三方机构可访问。支持多条件筛选。",
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
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'THIRD_PARTY')")
    public ApiResponse<Page<TradeResponse>> listTrades(
            @Parameter(description = "卖方企业ID筛选", example = "1")
            @RequestParam(required = false) Long sellerId,
            @Parameter(description = "买方企业ID筛选", example = "2")
            @RequestParam(required = false) Long buyerId,
            @Parameter(description = "交易类型筛选 (1-P2P, 2-拍卖)", example = "1")
            @RequestParam(required = false) Integer tradeType,
            @Parameter(description = "交易状态筛选 (0-待审核, 1-已完成, 2-已取消)", example = "1")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码，从1开始", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(tradeService.listTrades(sellerId, buyerId, tradeType, status, page, size));
    }

    @GetMapping("/my-trades")
    @Operation(
        summary = "查询我的交易", 
        description = "企业用户查询与自己相关的交易列表（作为买方或卖方）。",
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
    public ApiResponse<Page<TradeResponse>> listMyTrades(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "交易类型筛选", example = "1")
            @RequestParam(required = false) Integer tradeType,
            @Parameter(description = "交易状态筛选", example = "1")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(tradeService.listMyTrades(currentUser, tradeType, status, page, size));
    }
}
