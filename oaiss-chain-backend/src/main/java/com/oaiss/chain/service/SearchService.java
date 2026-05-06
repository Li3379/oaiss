package com.oaiss.chain.service;

import com.oaiss.chain.dto.SearchResponse;
import com.oaiss.chain.entity.CarbonReport;
import com.oaiss.chain.entity.Transaction;
import com.oaiss.chain.repository.CarbonReportRepository;
import com.oaiss.chain.repository.TransactionRepository;
import com.oaiss.chain.repository.EnterpriseRepository;
import com.oaiss.chain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 搜索查询服务
 * 提供历史信息搜索、全市场宏观数据统计
 * <p>
 * 设计文档要求（doc01）：
 * - 搜索查询模块：历史信息查询、全市场宏观数据统计
 *
 * @author OAISS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final CarbonReportRepository carbonReportRepository;
    private final TransactionRepository transactionRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final UserRepository userRepository;

    /**
     * 搜索碳报告
     */
    public Page<SearchResponse.CarbonReportSummary> searchCarbonReports(
            String keyword, Long enterpriseId, Integer status,
            Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CarbonReport> reports = carbonReportRepository.search(enterpriseId, status, keyword, pageable);
        return reports.map(this::toReportSummary);
    }

    /**
     * 搜索交易记录
     */
    public Page<SearchResponse.TradeSummary> searchTrades(
            Long sellerId, Long buyerId, Integer tradeType, Integer status,
            Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transaction> trades = transactionRepository.search(sellerId, buyerId, tradeType, status, pageable);
        return trades.map(this::toTradeSummary);
    }

    /**
     * 获取全市场宏观数据
     */
    public SearchResponse.MarketOverview getMarketOverview() {
        long totalEnterprises = enterpriseRepository.count();
        long totalReports = carbonReportRepository.count();
        long totalTrades = transactionRepository.count();

        // 构建市场概览
        return SearchResponse.MarketOverview.builder()
                .totalEnterprises(totalEnterprises)
                .totalCarbonReports(totalReports)
                .totalTransactions(totalTrades)
                .queryTime(LocalDateTime.now())
                .build();
    }

    // ==================== 私有方法 ====================

    private SearchResponse.CarbonReportSummary toReportSummary(CarbonReport report) {
        return SearchResponse.CarbonReportSummary.builder()
                .id(report.getId())
                .reportNo(report.getReportNo())
                .enterpriseId(report.getEnterpriseId())
                .title(report.getTitle())
                .accountingPeriod(report.getAccountingPeriod())
                .totalEmission(report.getTotalEmission())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private SearchResponse.TradeSummary toTradeSummary(Transaction trade) {
        return SearchResponse.TradeSummary.builder()
                .id(trade.getId())
                .tradeNo(trade.getTradeNo())
                .tradeType(trade.getTradeType())
                .sellerId(trade.getSellerId())
                .buyerId(trade.getBuyerId())
                .quantity(trade.getQuantity())
                .unitPrice(trade.getUnitPrice())
                .totalAmount(trade.getTotalAmount())
                .status(trade.getStatus())
                .createdAt(trade.getCreatedAt())
                .build();
    }
}
