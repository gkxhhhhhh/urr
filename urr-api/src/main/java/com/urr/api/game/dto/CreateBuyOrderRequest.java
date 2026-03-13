package com.urr.api.game.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 创建买单请求。
 */
@Data
public class CreateBuyOrderRequest {

    /**
     * 玩家ID。
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
    private Long qty;

    /**
     * 单价。
     */
    @NotNull(message = "priceEach不能为空")
    private Long priceEach;

    /**
     * 过期分钟数。
     */
    private Long expireMinutes;
}