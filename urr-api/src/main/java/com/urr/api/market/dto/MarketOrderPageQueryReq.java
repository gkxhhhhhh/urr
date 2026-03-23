package com.urr.api.market.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 市场订单分页查询请求。
 */
@Data
public class MarketOrderPageQueryReq {

    /**
     * 角色ID。
     */
    @NotNull(message = "playerId不能为空")
    private Long playerId;

    /**
     * 订单类型。
     */
    @NotBlank(message = "orderType不能为空")
    private String orderType;

    /**
     * 物品ID。
     *
     * 说明：
     * 市场列表查询时可传，用于按单个物品筛选。
     * 我的订单查询时允许为空。
     */
    private Long itemId;

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