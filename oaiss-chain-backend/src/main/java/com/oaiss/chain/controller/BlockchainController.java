package com.oaiss.chain.controller;

import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.service.BlockchainServicePort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 区块链控制器
 * 提供区块链状态查询、区块信息查询、交易记录查询
 *
 * @author OAISS Team
 */
@RestController
@RequestMapping("/blockchain")
@RequiredArgsConstructor
@Tag(name = "09. 区块链管理", description = "区块链状态查询、区块信息查询、链上交易记录查询")
public class BlockchainController {

    private final BlockchainServicePort blockchainService;

    @GetMapping("/status")
    @Operation(summary = "检查区块链连接状态", description = "检查与Hyperledger Fabric区块链网络的连接状态，返回网络名称、通道数量、节点状态等信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功获取区块链状态"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "区块链连接异常")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTERPRISE', 'THIRD_PARTY')")
    public ApiResponse<Map<String, Object>> checkStatus() {
        return ApiResponse.success(blockchainService.checkConnection());
    }

    @GetMapping("/block/{blockNumber}")
    @Operation(summary = "查询区块信息", description = "根据区块高度查询区块链上的区块详细信息，包括区块哈希、交易列表、时间戳等")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功获取区块信息"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "区块号无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "区块不存在")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @Parameter(name = "blockNumber", description = "区块高度/区块号", required = true, example = "1000")
    @PreAuthorize("hasAnyRole('ADMIN', 'THIRD_PARTY')")
    public ApiResponse<String> queryBlock(@PathVariable Long blockNumber) {
        return ApiResponse.success(blockchainService.queryBlock(blockNumber));
    }

    @GetMapping("/transaction/{txHash}")
    @Operation(summary = "查询链上交易", description = "根据交易哈希查询区块链上的交易详细信息，包括交易状态、交易时间、交易数据等")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功获取交易信息"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "交易哈希格式无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "交易不存在")
    })
    @Parameter(name = "txHash", description = "交易哈希值", required = true, example = "0x1234567890abcdef...")
    public ApiResponse<String> queryTransaction(@PathVariable String txHash) {
        return ApiResponse.success(blockchainService.queryTransaction(txHash));
    }

    @GetMapping("/transactions")
    @Operation(summary = "查询链上交易列表", description = "分页查询区块链上的交易记录列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功获取交易列表"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTERPRISE', 'THIRD_PARTY')")
    public ApiResponse<Page<Map<String, Object>>> listTransactions(
            @Parameter(description = "页码（从1开始）", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(blockchainService.listTransactions(page, size));
    }

    @GetMapping("/blocks/latest")
    @Operation(summary = "获取最新区块列表", description = "获取区块链上最新的区块列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功获取区块列表"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTERPRISE', 'THIRD_PARTY')")
    public ApiResponse<Page<Map<String, Object>>> listLatestBlocks(
            @Parameter(description = "页码（从1开始）", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(blockchainService.listLatestBlocks(page, size));
    }
}
