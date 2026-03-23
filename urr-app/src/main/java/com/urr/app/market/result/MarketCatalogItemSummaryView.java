package com.urr.app.market.result;

import lombok.Data;

/**
 * 商品目录概要视图。
 */
@Data
public class MarketCatalogItemSummaryView {

    /**
     * 物品ID。
     */
    private Long itemId;

    /**
     * 卖单数量。
     */
    private Long sellOrderCount = 0L;

    /**
     * 买单数量。
     */
    private Long buyOrderCount = 0L;

    /**
     * 卖单剩余总量。
     */
    private Long sellQty = 0L;

    /**
     * 买单剩余总量。
     */
    private Long buyQty = 0L;

    /**
     * 最低卖价。
     */
    private Long lowestSellPrice;

    /**
     * 最高买价。
     */
    private Long highestBuyPrice;
}