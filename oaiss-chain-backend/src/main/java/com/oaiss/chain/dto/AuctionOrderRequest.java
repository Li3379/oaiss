package com.oaiss.chain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 拍卖挂单请求DTO
 *
 * @author OAISS Team
 */
@Data
public class AuctionOrderRequest {

    /**
     * 方向（1-买入, 2-卖出）
     */
    private Integer direction;

    /**
     * 碳配额数量（吨CO2当量）
     */
    private BigDecimal quantity;

    /**
     * 期望单价（元/吨）
     */
    private BigDecimal price;
}
