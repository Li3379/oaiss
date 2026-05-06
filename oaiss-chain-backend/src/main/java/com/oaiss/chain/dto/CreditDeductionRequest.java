package com.oaiss.chain.dto;

import lombok.Data;

/**
 * 信誉扣分请求DTO
 *
 * @author OAISS Team
 */
@Data
public class CreditDeductionRequest {

    /**
     * 企业ID
     */
    private Long enterpriseId;

    /**
     * 事件类型（1-数据造假, 2-迟交报告, 3-轻微违规, 4-严重违规）
     */
    private Integer eventType;

    /**
     * 事件描述
     */
    private String description;

    /**
     * 关联碳报告ID（可选）
     */
    private Long relatedReportId;
}
