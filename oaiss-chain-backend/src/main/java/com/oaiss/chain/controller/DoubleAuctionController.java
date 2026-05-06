package com.oaiss.chain.controller;

import com.oaiss.chain.dto.AuctionOrderRequest;
import com.oaiss.chain.dto.AuctionOrderResponse;
import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.dto.MatchingResultResponse;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.DoubleAuctionService;
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

import java.util.List;

/**
 * 双向拍卖控制器
 * 提供碳配额双向拍卖功能，包括买入/卖出挂单、撮合匹配、结果查询
 *
 * @author OAISS Team
 */
@RestController
@RequestMapping("/auction")
@RequiredArgsConstructor
@Tag(name = "12. 双向拍卖管理", description = "碳配额双向拍卖挂单、撮合匹配、撮合结果查询")
public class DoubleAuctionController {

    private final DoubleAuctionService doubleAuctionService;

    @PostMapping("/buy")
    @Operation(summary = "提交买入挂单", description = "企业提交碳配额买入挂单，指定期望价格和数量，等待撮合匹配")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "挂单成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "挂单参数无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非企业用户无法挂单")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<AuctionOrderResponse> placeBuyOrder(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Valid @RequestBody AuctionOrderRequest request) {
        return ApiResponse.success(doubleAuctionService.placeBuyOrder(currentUser, request));
    }

    @PostMapping("/sell")
    @Operation(summary = "提交卖出挂单", description = "企业提交碳配额卖出挂单，指定期望价格和数量，等待撮合匹配")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "挂单成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "挂单参数无效或配额不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非企业用户无法挂单")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ENTERPRISE')")
    public ApiResponse<AuctionOrderResponse> placeSellOrder(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Valid @RequestBody AuctionOrderRequest request) {
        return ApiResponse.success(doubleAuctionService.placeSellOrder(currentUser, request));
    }

    @PostMapping("/match")
    @Operation(summary = "执行撮合", description = "执行双向拍卖撮合算法，按价格优先、时间优先原则匹配买卖挂单")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "撮合执行成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "仅管理员可执行撮合")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<MatchingResultResponse>> executeMatching() {
        return ApiResponse.success(doubleAuctionService.executeMatching());
    }

    @GetMapping("/orders")
    @Operation(summary = "查询挂单列表", description = "分页查询所有拍卖挂单，支持按方向和状态筛选")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'THIRD_PARTY')")
    public ApiResponse<Page<AuctionOrderResponse>> listOrders(
            @Parameter(description = "挂单方向：1-买入, 2-卖出", example = "1") @RequestParam(required = false) Integer direction,
            @Parameter(description = "挂单状态：0-待撮合, 1-已撮合, 2-已取消", example = "0") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码（从1开始）", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(doubleAuctionService.listOrders(direction, status, page, size));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "查询我的挂单", description = "查询当前用户提交的所有拍卖挂单")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Page<AuctionOrderResponse>> listMyOrders(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "挂单方向：1-买入, 2-卖出", example = "1") @RequestParam(required = false) Integer direction,
            @Parameter(description = "挂单状态：0-待撮合, 1-已撮合, 2-已取消", example = "0") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码（从1开始）", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(doubleAuctionService.listMyOrders(currentUser, direction, status, page, size));
    }

    @GetMapping("/results")
    @Operation(summary = "查询撮合结果", description = "查询当前用户的撮合成交结果")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Page<MatchingResultResponse>> listMatchingResults(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "页码（从1开始）", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(doubleAuctionService.listMatchingResults(currentUser, page, size));
    }
}
