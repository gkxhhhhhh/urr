package com.urr.api.game.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * 市场成交写请求。
 */
@Data
public class MarketTradeRequest {

    /**
     * 玩家ID。
     */
    @NotNull(message = "playerId不能为空")
    @Positive(message = "playerId必须大于0")
    private Long playerId;

    /**
     * 数量。
     */
    @NotNull(message = "qty不能为空")
    @Positive(message = "qty必须大于0")
    private Long qty;
}
