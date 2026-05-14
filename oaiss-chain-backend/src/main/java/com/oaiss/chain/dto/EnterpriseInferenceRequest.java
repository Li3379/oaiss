package com.oaiss.chain.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

/**
 * Enterprise inference request DTO
 *
 * @author OAISS Team
 */
@Data
@Builder
public class EnterpriseInferenceRequest {

    @NotNull(message = "Enterprise ID must not be null")
    @Positive(message = "Enterprise ID must be positive")
    private Long enterpriseId;

    @Builder.Default
    private Integer reportCount = 0;

    @Builder.Default
    private Double totalEmissions = 0.0;

    @DecimalMin(value = "0.0", message = "Credit score must be at least 0.0")
    @DecimalMax(value = "100.0", message = "Credit score must be at most 100.0")
    @Builder.Default
    private Double creditScore = 0.0;

    @Builder.Default
    private Double emissionRating = 0.0;

    @Builder.Default
    private Double transactionVolume = 0.0;

    @Builder.Default
    private Integer complianceFlags = 0;

    @Builder.Default
    private Double avgEmissionPerReport = 0.0;

    @Builder.Default
    private Integer daysSinceLastReport = 0;
}