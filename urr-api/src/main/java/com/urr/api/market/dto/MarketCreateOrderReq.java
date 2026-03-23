package com.urr.api.market.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 创建市场订单请求。
 */
@Data
public class MarketCreateOrderReq {

    /**
     * 角色ID。
     */
    @NotNull(message = "playerId不能为空")
    private Long playerId;

    /**
     * 物品ID。
     */
    @NotNull(message = "itemId不能为空")
    private Long itemId;

    /**
     * 数量。
     */
    @NotNull(message = "qty不能为空")
    @Min(value = 1, message = "qty不能小于1")
    private Long qty;

    /**
     * 单价。
     */
    @NotNull(message = "priceEach不能为空")
    @Min(value = 1, message = "priceEach不能小于1")
    private Long priceEach;

    /**
     * 过期分钟数。
     */
    @Min(value = 1, message = "expireMinutes不能小于1")
    private Long expireMinutes;
}