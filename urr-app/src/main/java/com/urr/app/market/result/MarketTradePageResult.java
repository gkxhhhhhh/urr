package com.urr.app.market.result;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 市场成交分页结果。
 */
@Data
public class MarketTradePageResult {

    /**
     * 总数。
     */
    private Long total = 0L;

    /**
     * 列表。
     */
    private List<MarketTradeView> list = new ArrayList<>();
}