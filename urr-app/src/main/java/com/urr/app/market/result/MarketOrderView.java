package com.urr.app.market.result;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 市场订单视图。
 */
@Data
public class MarketOrderView {

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 订单类型。
     */
    private String orderType;

    /**
     * 订单状态。
     */
    private String status;

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
     * 总数量。
     */
    private Long qtyTotal;

    /**
     * 剩余数量。
     */
    private Long qtyRemain;

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
     * 手续费基点。
     */
    private Integer feeRateBp;

    /**
     * 过期时间。
     */
    private LocalDateTime expireTime;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;
}