package com.urr.app.market.result;

import lombok.Data;

/**
 * 市场商品目录项。
 */
@Data
public class MarketItemCatalogView {

    /**
     * 物品ID。
     */
    private Long itemId;

    /**
     * 物品编码。
     */
    private String itemCode;

    /**
     * 物品名称。
     */
    private String itemName;

    /**
     * 物品类型。
     */
    private Integer itemType;

    /**
     * 稀有度。
     */
    private Integer rarity;

    /**
     * 商店售卖价格。
     */
    private Long sellPrice;

    /**
     * 物品备注描述。
     */
    private String remarks;

    /**
     * 扩展元数据。
     */
    private String metaJson;
}