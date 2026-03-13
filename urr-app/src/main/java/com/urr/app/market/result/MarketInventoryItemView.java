package com.urr.app.market.result;

import lombok.Data;

/**
 * 可挂牌库存项。
 */
@Data
public class MarketInventoryItemView {

    /**
     * 物品ID。
     */
    private Long itemId;

    /**
     * 物品名称。
     */
    private String itemName;

    /**
     * 背包数量。
     */
    private Long bagQty;
}