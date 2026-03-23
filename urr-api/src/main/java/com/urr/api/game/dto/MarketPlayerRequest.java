package com.urr.api.game.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * 仅携带玩家ID的市场查询请求。
 */
@Data
public class MarketPlayerRequest {

    /**
     * 玩家ID。
     */
    @NotNull(message = "playerId不能为空")
    @Positive(message = "playerId必须大于0")
    private Long playerId;
}