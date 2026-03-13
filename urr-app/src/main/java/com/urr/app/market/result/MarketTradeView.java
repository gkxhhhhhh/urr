package com.urr.app.market.result;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 市场成交视图。
 */
@Data
public class MarketTradeView {

    /**
     * 成交ID。
     */
    private Long tradeId;

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 卖家ID。
     */
    private Long sellerId;

    /**
     * 卖家昵称。
     */
    private String sellerName;

    /**
     * 买家ID。
     */
    private Long buyerId;

    /**
     * 买家昵称。
     */
    private String buyerName;

    /**
     * 物品ID。
     */
    private Long itemId;

    /**
     * 物品名称。
     */
    private String itemName;

    /**
     * 数量。
     */
    private Long qty;

    /**
     * 单价。
     */
    private Long priceEach;

    /**
     * 总额。
     */
    private Long totalAmount;

    /**
     * 币种。
     */
    private String currencyCode;

    /**
     * 成交时间。
     */
    private LocalDateTime tradeTime;
}