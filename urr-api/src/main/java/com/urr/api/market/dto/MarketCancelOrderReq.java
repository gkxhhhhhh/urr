package com.urr.api.market.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 取消市场订单请求。
 */
@Data
public class MarketCancelOrderReq {

    /**
     * 角色ID。
     */
    @NotNull(message = "playerId不能为空")
    private Long playerId;
}