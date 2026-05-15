package com.oaiss.chain.controller;

import com.oaiss.chain.annotation.RateLimit;
import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.dto.CarbonPredictionRequest;
import com.oaiss.chain.dto.CarbonPredictionResponse;
import com.oaiss.chain.dto.EmissionRatingRequest;
import com.oaiss.chain.entity.EmissionRating;
import com.oaiss.chain.service.CarbonPredictionService;
import com.oaiss.chain.service.EmissionRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 碳排放评级与AI预测控制器
 * 提供企业碳排放评级、行业排名、AI碳排放预测功能
 *
 * @author OAISS Team
 */
@RestController
@RequestMapping("/emission")
@RequiredArgsConstructor
@Tag(name = "13. 碳排放评级管理", description = "企业碳排放评级、行业排名、AI碳排放趋势预测")
@Validated
public class EmissionController {

    private final EmissionRatingService ratingService;
    private final CarbonPredictionService predictionService;

    @GetMapping("/ratings/{enterpriseId}")
    @Operation(summary = "企业评级历史", description = "获取指定企业历年碳排放评级记录，包括评级等级、碳排放量、排名变化等")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "企业ID无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "企业不存在或无评级记录")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @Parameter(name = "enterpriseId", description = "企业ID", required = true, example = "1")
    public ApiResponse<List<EmissionRating>> getRatings(@PathVariable Long enterpriseId) {
        return ApiResponse.success(ratingService.getEnterpriseRatings(enterpriseId));
    }

    @PostMapping("/ratings")
    @Operation(summary = "生成评级", description = "根据企业年度碳排放数据生成碳排放评级，自动计算评级等级")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "评级生成成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "评级参数无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<EmissionRating> createRating(@Valid @RequestBody EmissionRatingRequest request) {
        return ApiResponse.success(ratingService.rateEnterprise(
                request.getEnterpriseId(), request.getYear(),
                request.getTotalEmission(), request.getRevenue(),
                request.getRatedBy()));
    }

    @GetMapping("/rankings/{year}")
    @Operation(summary = "行业排名", description = "获取指定年度全行业碳排放排名，按碳排放强度排序")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "年份格式无效")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @Parameter(name = "year", description = "统计年份", required = true, example = "2025")
    public ApiResponse<List<EmissionRating>> getRankings(@PathVariable String year) {
        return ApiResponse.success(ratingService.getIndustryRanking(year));
    }

    @PostMapping("/predict")
    @PreAuthorize("hasAnyRole('ENTERPRISE', 'ADMIN')")
    @RateLimit(key = "emission_predict", limit = 10, period = 60)
    @Operation(summary = "AI碳排放预测", description = "基于企业历史碳排放数据，使用AI模型预测未来碳排放趋势")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "预测成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "预测参数无效或历史数据不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<CarbonPredictionResponse> predict(@Valid @RequestBody CarbonPredictionRequest request) {
        return ApiResponse.success(predictionService.predict(request));
    }
}
