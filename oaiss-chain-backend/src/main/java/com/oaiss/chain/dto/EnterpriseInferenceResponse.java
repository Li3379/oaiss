package com.oaiss.chain.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Enterprise inference response DTO
 *
 * @author OAISS Team
 */
@Data
@Builder
public class EnterpriseInferenceResponse {

    private Long enterpriseId;
    private String complianceStatus;
    private Double confidence;
    private Double anomalyScore;
    private Boolean isAnomaly;
    @Builder.Default
    private List<String> riskFactors = List.of();
    @Builder.Default
    private String modelVersion = "1.0.0";
}