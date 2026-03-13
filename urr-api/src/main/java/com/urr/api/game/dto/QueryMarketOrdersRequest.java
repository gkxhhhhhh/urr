package com.urr.api.game.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 查询市场订单请求。
 */
@Data
public class QueryMarketOrdersRequest {

    /**
     * 玩家ID。
     */
    @NotNull(message = "playerId不能为空")
    private Long playerId;

    /**
     * 订单类型：SELL / BUY。
     */
    @NotBlank(message = "orderType不能为空")
    private String orderType;

    /**
     * 页码。
     */
    private Integer pageNo = 1;

    /**
     * 页大小。
     */
    private Integer pageSize = 20;
}