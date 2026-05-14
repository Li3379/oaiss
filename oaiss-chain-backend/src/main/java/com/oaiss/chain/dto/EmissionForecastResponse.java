package com.oaiss.chain.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Emission forecast response DTO
 *
 * @author OAISS Team
 */
@Data
@Builder
public class EmissionForecastResponse {

    private Long enterpriseId;
    private List<String> forecastDates;
    private List<Double> forecastEmissions;
    private List<Double> lowerBound;
    private List<Double> upperBound;
    private String trend;
    private Double confidence;
    @Builder.Default
    private String modelVersion = "1.0.0";
}