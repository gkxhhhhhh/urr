package com.urr.app.market.result;

import lombok.Data;

/**
 * 取消订单结果。
 */
@Data
public class MarketCancelOrderResult {

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 状态。
     */
    private String status;

    /**
     * 退回物品数量。
     */
    private Long returnedItemQty;

    /**
     * 退回金额。
     */
    private Long returnedGoldAmount;
}