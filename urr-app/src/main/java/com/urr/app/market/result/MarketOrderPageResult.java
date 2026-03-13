package com.urr.app.market.result;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 市场订单分页结果。
 */
@Data
public class MarketOrderPageResult {

    /**
     * 总数。
     */
    private Long total = 0L;

    /**
     * 列表。
     */
    private List<MarketOrderView> list = new ArrayList<>();
}