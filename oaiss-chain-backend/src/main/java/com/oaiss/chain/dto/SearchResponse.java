package com.oaiss.chain.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 搜索查询响应DTO
 *
 * @author OAISS Team
 */
public class SearchResponse {

    /**
     * 碳报告摘要
     */
    @Data
    @Builder
    public static class CarbonReportSummary {
        private Long id;
        private String reportNo;
        private Long enterpriseId;
        private String title;
        private String accountingPeriod;
        private BigDecimal totalEmission;
        private Integer status;
        private LocalDateTime createdAt;
    }

    /**
     * 交易摘要
     */
    @Data
    @Builder
    public static class TradeSummary {
        private Long id;
        private String tradeNo;
        private Integer tradeType;
        private Long sellerId;
        private Long buyerId;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalAmount;
        private Integer status;
        private LocalDateTime createdAt;
    }

    /**
     * 市场概览数据
     */
    @Data
    @Builder
    public static class MarketOverview {
        private Long totalEnterprises;
        private Long totalCarbonReports;
        private Long totalTransactions;
        private LocalDateTime queryTime;
    }
}
