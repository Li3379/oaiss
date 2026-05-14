package com.oaiss.chain.controller;

import com.oaiss.chain.annotation.RateLimit;
import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.dto.EnterpriseInferenceResponse;
import com.oaiss.chain.service.ml.EnterpriseInferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Enterprise inference controller for compliance risk assessment
 * and emission trend inference.
 *
 * @author OAISS Team
 */
@RestController
@RequestMapping("/api/v1/predict/enterprise")
@RequiredArgsConstructor
@Tag(name = "Enterprise Inference", description = "Enterprise compliance risk assessment")
public class EnterpriseInferenceController {

    private final EnterpriseInferenceService enterpriseInferenceService;

    @GetMapping("/{enterpriseId}/inference")
    @Operation(
            summary = "Enterprise compliance inference",
            description = "Generate enterprise compliance risk assessment "
                    + "using IsolationForest anomaly detection and XGBoost classification.",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")
    )
    @PreAuthorize("hasAnyRole('ENTERPRISE', 'REVIEWER', 'THIRD_PARTY', 'ADMIN')")
    @RateLimit(key = "enterprise-inference", limit = 10, period = 60)
    public ApiResponse<EnterpriseInferenceResponse> inferEnterprise(
            @Parameter(description = "Enterprise ID", required = true, example = "1")
            @PathVariable Long enterpriseId) {
        return ApiResponse.success(
                enterpriseInferenceService.inferEnterprise(enterpriseId));
    }
}