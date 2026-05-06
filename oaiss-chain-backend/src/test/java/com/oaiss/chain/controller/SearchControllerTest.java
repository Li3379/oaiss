package com.oaiss.chain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaiss.chain.dto.SearchResponse;
import com.oaiss.chain.security.JwtTokenProvider;
import com.oaiss.chain.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SearchController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
                org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
        })
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchService searchService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private SearchResponse.CarbonReportSummary reportSummary;
    private SearchResponse.TradeSummary tradeSummary;
    private SearchResponse.MarketOverview marketOverview;

    @BeforeEach
    void setUp() {
        reportSummary = SearchResponse.CarbonReportSummary.builder()
                .id(1L)
                .reportNo("RPT-2024-001")
                .enterpriseId(1L)
                .title("2024年度碳报告")
                .status(2)
                .build();

        tradeSummary = SearchResponse.TradeSummary.builder()
                .id(1L)
                .tradeNo("TRD-2024-001")
                .tradeType(1)
                .sellerId(1L)
                .buyerId(2L)
                .quantity(new BigDecimal("1000"))
                .unitPrice(new BigDecimal("100.00"))
                .build();

        marketOverview = SearchResponse.MarketOverview.builder()
                .totalEnterprises(50L)
                .totalCarbonReports(100L)
                .totalTransactions(200L)
                .build();
    }

    @Test
    @DisplayName("搜索碳报告成功")
    void testSearchReportsSuccess() throws Exception {
        Page<SearchResponse.CarbonReportSummary> page = new PageImpl<>(List.of(reportSummary));
        when(searchService.searchCarbonReports(eq(null), eq(null), eq(null), eq(1), eq(20))).thenReturn(page);

        mockMvc.perform(get("/search/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        verify(searchService).searchCarbonReports(null, null, null, 1, 20);
    }

    @Test
    @DisplayName("搜索碳报告-带筛选条件")
    void testSearchReportsWithFilters() throws Exception {
        Page<SearchResponse.CarbonReportSummary> page = new PageImpl<>(List.of(reportSummary));
        when(searchService.searchCarbonReports(eq("年度"), eq(1L), eq(2), eq(1), eq(10))).thenReturn(page);

        mockMvc.perform(get("/search/reports")
                        .param("keyword", "年度")
                        .param("enterpriseId", "1")
                        .param("status", "2")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(searchService).searchCarbonReports("年度", 1L, 2, 1, 10);
    }

    @Test
    @DisplayName("搜索交易记录成功")
    void testSearchTradesSuccess() throws Exception {
        Page<SearchResponse.TradeSummary> page = new PageImpl<>(List.of(tradeSummary));
        when(searchService.searchTrades(eq(null), eq(null), eq(null), eq(null), eq(1), eq(20))).thenReturn(page);

        mockMvc.perform(get("/search/trades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        verify(searchService).searchTrades(null, null, null, null, 1, 20);
    }

    @Test
    @DisplayName("搜索交易记录-带筛选条件")
    void testSearchTradesWithFilters() throws Exception {
        Page<SearchResponse.TradeSummary> page = new PageImpl<>(List.of(tradeSummary));
        when(searchService.searchTrades(eq(1L), eq(2L), eq(1), eq(1), eq(1), eq(10))).thenReturn(page);

        mockMvc.perform(get("/search/trades")
                        .param("sellerId", "1")
                        .param("buyerId", "2")
                        .param("tradeType", "1")
                        .param("status", "1")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(searchService).searchTrades(1L, 2L, 1, 1, 1, 10);
    }

    @Test
    @DisplayName("获取市场概览成功")
    void testGetMarketOverviewSuccess() throws Exception {
        when(searchService.getMarketOverview()).thenReturn(marketOverview);

        mockMvc.perform(get("/search/market-overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalEnterprises").value(50))
                .andExpect(jsonPath("$.data.totalCarbonReports").value(100))
                .andExpect(jsonPath("$.data.totalTransactions").value(200));

        verify(searchService).getMarketOverview();
    }
}