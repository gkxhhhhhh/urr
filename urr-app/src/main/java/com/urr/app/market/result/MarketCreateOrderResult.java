package com.urr.app.market.result;

import lombok.Data;

/**
 * 创建市场订单结果。
 */
@Data
public class MarketCreateOrderResult {

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 订单类型。
     */
    private String orderType;

    /**
     * 状态。
     */
    private String status;

    /**
     * 总数量。
     */
    private Long qtyTotal;

    /**
     * 剩余数量。
     */
    private Long qtyRemain;
}