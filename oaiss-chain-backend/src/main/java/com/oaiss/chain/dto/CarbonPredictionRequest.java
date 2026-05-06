package com.oaiss.chain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 碳排放预测请求
 *
 * @author OAISS Team
 */
@Data
public class CarbonPredictionRequest {

    @NotNull(message = "企业ID不能为空")
    private Long enterpriseId;

    /**
     * 预测月数（默认6）
     */
    private Integer predictMonths = 6;
}
