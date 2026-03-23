package com.urr.api.market.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 市场成交记录分页查询请求。
 */
@Data
public class MarketTradePageQueryReq {

    /**
     * 角色ID。
     */
    @NotNull(message = "playerId不能为空")
    private Long playerId;

    /**
     * 页码。
     */
    @Min(value = 1, message = "pageNo不能小于1")
    private Integer pageNo;

    /**
     * 页大小。
     */
    @Min(value = 1, message = "pageSize不能小于1")
    private Integer pageSize;
}