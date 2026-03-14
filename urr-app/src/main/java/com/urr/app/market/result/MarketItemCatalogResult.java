package com.urr.app.market.result;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 市场商品目录结果。
 */
@Data
public class MarketItemCatalogResult {

    /**
     * 总数。
     */
    private Long count = 0L;

    /**
     * 列表。
     */
    private List<MarketItemCatalogView> list = Collections.emptyList();
}
