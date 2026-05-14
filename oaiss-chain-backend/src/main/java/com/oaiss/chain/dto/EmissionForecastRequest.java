package com.oaiss.chain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Emission forecast request DTO
 *
 * @author OAISS Team
 */
@Data
@Builder
public class EmissionForecastRequest {

    @NotNull(message = "Enterprise ID must not be null")
    @Positive(message = "Enterprise ID must be positive")
    private Long enterpriseId;

    @NotEmpty(message = "Historical dates must not be empty")
    private List<String> dates;

    @NotEmpty(message = "Historical emissions must not be empty")
    private List<Double> emissions;

    @Builder.Default
    private String sector = "power_generation";

    @NotNull(message = "Horizon days must not be null")
    @Builder.Default
    private Integer horizonDays = 180;
}