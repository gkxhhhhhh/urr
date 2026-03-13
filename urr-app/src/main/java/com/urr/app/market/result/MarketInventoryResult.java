package com.urr.app.market.result;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 可挂牌库存结果。
 */
@Data
public class MarketInventoryResult {

    /**
     * 数量。
     */
    private Integer count = 0;

    /**
     * 列表。
     */
    private List<MarketInventoryItemView> list = new ArrayList<>();
}