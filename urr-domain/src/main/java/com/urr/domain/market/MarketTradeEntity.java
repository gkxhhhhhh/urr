package com.urr.domain.market;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 市场成交记录实体。
 */
@Data
@TableName("t_urr_market_trade")
public class MarketTradeEntity {

    /**
     * 成交ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 区服ID。
     */
    private Integer serverId;

    /**
     * 订单ID。
     */
    private Long orderId;

    /**
     * 卖家ID。
     */
    private Long sellerId;

    /**
     * 买家ID。
     */
    private Long buyerId;

    /**
     * 物品ID。
     */
    private Long itemId;

    /**
     * 装备实例ID。
     */
    private Long equipInstanceId;

    /**
     * 成交数量。
     */
    private Long qty;

    /**
     * 单价。
     */
    private Long priceEach;

    /**
     * 币种。
     */
    private String currencyCode;

    /**
     * 手续费金额。
     */
    private Long feeAmount;

    /**
     * 成交总额。
     */
    private Long totalAmount;

    /**
     * 成交时间。
     */
    private LocalDateTime tradeTime;

    /**
     * 随机种子。
     */
    private Long seed;

    /**
     * 备注。
     */
    private String remarks;

    /**
     * 创建人。
     */
    private String createUser;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 修改人。
     */
    private String updateUser;

    /**
     * 修改时间。
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer deleteFlag;
}