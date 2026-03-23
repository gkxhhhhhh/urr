package com.urr.app.market.result;

import lombok.Data;

/**
 * 我的市场总览结果。
 */
@Data
public class MarketMyOverviewResult {

    /**
     * 我的卖单。
     */
    private MarketOrderPageResult mySellOrders = new MarketOrderPageResult();

    /**
     * 我的买单。
     */
    private MarketOrderPageResult myBuyOrders = new MarketOrderPageResult();

    /**
     * 我的成交记录。
     */
    private MarketTradePageResult myTrades = new MarketTradePageResult();
}