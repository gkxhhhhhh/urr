package com.urr.api.game.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 取消市场订单请求。
 */
@Data
public class CancelMarketOrderRequest {

    /**
     * 玩家ID。
     */
    @NotNull(message = "playerId不能为空")
    private Long playerId;
}