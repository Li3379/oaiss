package com.oaiss.chain.controller;

import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.dto.SearchResponse;
import com.oaiss.chain.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 搜索查询控制器
 * 提供历史信息搜索、全市场宏观数据统计
 *
 * @author OAISS Team
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "14. 搜索查询管理", description = "碳报告搜索、交易记录搜索、市场宏观数据统计")
@PreAuthorize("isAuthenticated()")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/reports")
    @Operation(summary = "搜索碳报告", description = "根据关键字、企业ID、状态搜索碳报告，支持模糊匹配和分页")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Page<SearchResponse.CarbonReportSummary>> searchReports(
            @Parameter(description = "搜索关键字", example = "年度报告") @RequestParam(required = false) String keyword,
            @Parameter(description = "企业ID", example = "1") @RequestParam(required = false) Long enterpriseId,
            @Parameter(description = "报告状态：0-草稿, 1-待审核, 2-已通过, 3-已驳回", example = "2") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码（从1开始）", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(searchService.searchCarbonReports(keyword, enterpriseId, status, page, size));
    }

    @GetMapping("/trades")
    @Operation(summary = "搜索交易记录", description = "根据条件搜索碳交易记录，支持按买卖双方、交易类型、状态筛选")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Page<SearchResponse.TradeSummary>> searchTrades(
            @Parameter(description = "卖方企业ID", example = "1") @RequestParam(required = false) Long sellerId,
            @Parameter(description = "买方企业ID", example = "2") @RequestParam(required = false) Long buyerId,
            @Parameter(description = "交易类型：1-P2P, 2-拍卖, 3-挂牌", example = "1") @RequestParam(required = false) Integer tradeType,
            @Parameter(description = "交易状态：0-待付款, 1-已完成, 2-已取消", example = "1") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码（从1开始）", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(searchService.searchTrades(sellerId, buyerId, tradeType, status, page, size));
    }

    @GetMapping("/market-overview")
    @Operation(summary = "市场概览", description = "获取全市场宏观数据统计，包括交易总量、平均价格、活跃企业数等")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<SearchResponse.MarketOverview> getMarketOverview() {
        return ApiResponse.success(searchService.getMarketOverview());
    }
}
