package com.oaiss.chain.controller;

import com.oaiss.chain.dto.*;
import com.oaiss.chain.entity.CarbonCoinTransaction;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.CarbonCoinService;
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

/**
 * 碳币交易控制器
 * 提供碳币账户管理、充值、转账、交易流水查询
 * <p>
 * 设计文档要求（doc03）：
 * - 碳额度与碳币之间的交易市场
 *
 * @author OAISS Team
 */
@RestController
@RequestMapping("/carbon-coin")
@RequiredArgsConstructor
@Tag(name = "10. 碳币交易管理", description = "碳币账户管理、充值、转账、交易流水查询、碳配额购买/出售")
public class CarbonCoinController {

    private final CarbonCoinService carbonCoinService;

    @GetMapping("/account")
    @Operation(summary = "获取碳币账户", description = "获取碳币账户信息。不传userId时获取当前登录用户的账户，传userId时获取指定用户的账户（需管理员权限）。")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功获取账户信息"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "用户ID无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<CarbonCoinAccountResponse> getAccount(
            @Parameter(description = "用户ID（不传则获取当前用户）", example = "1")
            @RequestParam(required = false) Long userId,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser) {
        Long targetUserId = userId != null ? userId : currentUser.getUserId();
        if (userId != null && !userId.equals(currentUser.getUserId())
                && !currentUser.getRoles().contains("ADMIN")) {
            throw com.oaiss.chain.exception.AuthorizationException.permissionDenied("无权限查询他人账户");
        }
        return ApiResponse.success(carbonCoinService.getOrCreateAccount(targetUserId));
    }

    @PostMapping("/recharge")
    @Operation(summary = "充值碳币", description = "充值碳币到指定用户的账户，支持人民币兑换碳币。仅管理员可操作。")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "充值成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "充值金额无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户账户不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @Parameter(name = "userId", description = "用户ID", required = true, example = "1")
    public ApiResponse<CarbonCoinAccountResponse> recharge(
            @RequestParam Long userId,
            @Valid @RequestBody CarbonCoinRechargeRequest request) {
        return ApiResponse.success(carbonCoinService.recharge(userId, request));
    }

    @PostMapping("/transfer")
    @Operation(summary = "碳币转账", description = "向其他用户转账碳币，支持即时到账")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "转账成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "转账金额无效或余额不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "目标用户不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")
    public ApiResponse<CarbonCoinAccountResponse> transfer(
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            @Valid @RequestBody CarbonCoinTransferRequest request) {
        return ApiResponse.success(carbonCoinService.transfer(currentUser.getUserId(), request));
    }

    @GetMapping("/transactions")
    @Operation(summary = "交易流水", description = "查询碳币交易流水记录，支持按交易类型筛选，分页返回。不传userId时查询当前用户。")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功获取交易流水"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Page<CarbonCoinTransaction>> getTransactions(
            @Parameter(description = "用户ID（不传则查询当前用户）", example = "1") @RequestParam(required = false) Long userId,
            @Parameter(description = "交易类型：1-充值, 2-转账, 3-购买配额, 4-出售配额", example = "1") @RequestParam(required = false) Integer txType,
            @Parameter(description = "页码（从1开始）", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser) {
        Long targetUserId = userId != null ? userId : currentUser.getUserId();
        return ApiResponse.success(carbonCoinService.getTransactions(targetUserId, txType, page, size));
    }
}
