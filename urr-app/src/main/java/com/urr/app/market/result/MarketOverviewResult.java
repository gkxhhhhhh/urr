package com.urr.app.market.result;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 市场首页总览结果。
 */
@Data
public class MarketOverviewResult {

    /**
     * 商品目录。
     */
    private MarketItemCatalogResult catalog = new MarketItemCatalogResult();

    /**
     * 可交易库存。
     */
    private MarketInventoryResult inventory = new MarketInventoryResult();

    /**
     * 商品概要列表。
     */
    private List<MarketCatalogItemSummaryView> summaryList = new ArrayList<>();
}