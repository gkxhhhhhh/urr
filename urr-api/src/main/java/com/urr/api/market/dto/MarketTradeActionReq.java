package com.urr.api.market.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 市场成交请求。
 */
@Data
public class MarketTradeActionReq {

    /**
     * 角色ID。
     */
    @NotNull(message = "playerId不能为空")
    private Long playerId;

    /**
     * 成交数量。
     */
    @NotNull(message = "qty不能为空")
    @Min(value = 1, message = "qty不能小于1")
    private Long qty;
}