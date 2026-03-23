package com.urr.app.market.result;

import lombok.Data;

/**
 * 市场单商品详情结果。
 */
@Data
public class MarketItemDetailResult {

    /**
     * 商品基础信息。
     */
    private MarketItemCatalogView item;

    /**
     * 当前背包数量。
     */
    private Long bagQty = 0L;

    /**
     * 当前商品概要。
     */
    private MarketCatalogItemSummaryView summary = new MarketCatalogItemSummaryView();

    /**
     * 当前商品卖单列表。
     */
    private MarketOrderPageResult sellOrders = new MarketOrderPageResult();

    /**
     * 当前商品买单列表。
     */
    private MarketOrderPageResult buyOrders = new MarketOrderPageResult();
}