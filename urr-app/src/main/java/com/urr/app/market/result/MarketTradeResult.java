package com.urr.app.market.result;

import lombok.Data;

/**
 * 市场成交写接口结果。
 */
@Data
public class MarketTradeResult {

    /**
     * 成交ID。
     */
    private Long tradeId;

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 订单状态。
     */
    private String orderStatus;

    /**
     * 本次成交数量。
     */
    private Long tradeQty;

    /**
     * 成交总额。
     */
    private Long totalAmount;
}