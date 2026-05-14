package com.oaiss.chain.controller;

import com.oaiss.chain.dto.MarketForecastResponse;
import com.oaiss.chain.service.ml.MarketPredictionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketPredictionController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MarketPredictionController unit tests")
class MarketPredictionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketPredictionService marketPredictionService;

    private final MarketForecastResponse sampleResponse = MarketForecastResponse.builder()
            .forecastDates(List.of("2025-02-01", "2025-02-02", "2025-02-03"))
            .forecastPrices(List.of(55.0, 56.0, 57.0))
            .lowerBound(List.of(52.0, 53.0, 54.0))
            .upperBound(List.of(58.0, 59.0, 60.0))
            .trend("up")
            .modelVersion("1.0.0")
            .build();

    @Nested
    @DisplayName("POST /api/v1/ai/market/trend")
    class PredictTrendTests {

        @Test
        @DisplayName("should return 200 with forecast for ENTERPRISE role")
        @WithMockUser(roles = "ENTERPRISE")
        void shouldReturnForecastForEnterprise() throws Exception {
            when(marketPredictionService.predictMarketTrend(anyInt()))
                    .thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/ai/market/trend")
                            .with(csrf())
                            .param("horizonDays", "30")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.trend").value("up"))
                    .andExpect(jsonPath("$.data.forecastPrices").isArray())
                    .andExpect(jsonPath("$.data.modelVersion").value("1.0.0"));
        }

        @Test
        @DisplayName("should return 403 for unauthenticated user")
        void shouldReturn403ForUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/ai/market/trend")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ai/market/price")
    class PredictPriceTests {

        @Test
        @DisplayName("should return 200 with price forecast for ADMIN role")
        @WithMockUser(roles = "ADMIN")
        void shouldReturnPriceForecastForAdmin() throws Exception {
            when(marketPredictionService.predictCarbonPrice(anyInt()))
                    .thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/ai/market/price")
                            .with(csrf())
                            .param("horizonDays", "7")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.forecastPrices").isArray())
                    .andExpect(jsonPath("$.data.lowerBound").isArray())
                    .andExpect(jsonPath("$.data.upperBound").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ai/market/supply-demand")
    class PredictSupplyDemandTests {

        @Test
        @DisplayName("should return 200 with supply/demand forecast")
        @WithMockUser(roles = "ENTERPRISE")
        void shouldReturnSupplyDemandForecast() throws Exception {
            when(marketPredictionService.predictSupplyDemand(anyInt()))
                    .thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/ai/market/supply-demand")
                            .with(csrf())
                            .param("horizonDays", "14")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.trend").value("up"));
        }

        @Test
        @DisplayName("should reject horizonDays exceeding 365")
        @WithMockUser(roles = "ENTERPRISE")
        void shouldRejectExcessiveHorizonDays() throws Exception {
            mockMvc.perform(post("/api/v1/ai/market/supply-demand")
                            .with(csrf())
                            .param("horizonDays", "400")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Role-based access control")
    class RbacTests {

        @Test
        @DisplayName("should deny access for REVIEWER role")
        @WithMockUser(roles = "REVIEWER")
        void shouldDenyReviewer() throws Exception {
            mockMvc.perform(post("/api/v1/ai/market/trend")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should deny access for THIRD_PARTY role")
        @WithMockUser(roles = "THIRD_PARTY")
        void shouldDenyThirdParty() throws Exception {
            mockMvc.perform(post("/api/v1/ai/market/trend")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }
}