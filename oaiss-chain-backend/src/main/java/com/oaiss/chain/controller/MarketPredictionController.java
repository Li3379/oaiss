package com.oaiss.chain.controller;

import com.oaiss.chain.annotation.RateLimit;
import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.dto.MarketForecastResponse;
import com.oaiss.chain.service.ml.MarketPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for carbon market prediction endpoints.
 *
 * <p>Provides AI-powered market trend, carbon price, and supply/demand
 * forecasting via the ML microservice.</p>
 *
 * @author OAISS Team
 */
@RestController
@RequestMapping("/api/v1/ai/market")
@RequiredArgsConstructor
@Validated
@Tag(name = "AI Market Prediction", description = "Carbon market trend and price forecasting")
public class MarketPredictionController {

    private static final int DEFAULT_HORIZON_DAYS = 30;
    private static final int MAX_HORIZON_DAYS = 365;

    private final MarketPredictionService marketPredictionService;

    @PostMapping("/trend")
    @PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")
    @RateLimit(key = "market_trend", limit = 10, period = 60)
    @Operation(
            summary = "Predict market trend",
            description = "Generate carbon market trend forecast using Prophet time-series model"
    )
    public ResponseEntity<ApiResponse<MarketForecastResponse>> predictTrend(
            @RequestParam(defaultValue = "30")
            @Min(1) @Max(MAX_HORIZON_DAYS) Integer horizonDays) {
        MarketForecastResponse result = marketPredictionService.predictMarketTrend(horizonDays);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/price")
    @PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")
    @RateLimit(key = "market_price", limit = 10, period = 60)
    @Operation(
            summary = "Predict carbon price",
            description = "Generate carbon price forecast with confidence intervals"
    )
    public ResponseEntity<ApiResponse<MarketForecastResponse>> predictPrice(
            @RequestParam(defaultValue = "30")
            @Min(1) @Max(MAX_HORIZON_DAYS) Integer horizonDays) {
        MarketForecastResponse result = marketPredictionService.predictCarbonPrice(horizonDays);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/supply-demand")
    @PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")
    @RateLimit(key = "market_supply_demand", limit = 10, period = 60)
    @Operation(
            summary = "Predict supply and demand",
            description = "Generate supply/demand volume forecast using XGBoost"
    )
    public ResponseEntity<ApiResponse<MarketForecastResponse>> predictSupplyDemand(
            @RequestParam(defaultValue = "30")
            @Min(1) @Max(MAX_HORIZON_DAYS) Integer horizonDays) {
        MarketForecastResponse result = marketPredictionService.predictSupplyDemand(horizonDays);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}