package com.urr.domain.market;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 市场订单实体。
 */
@Data
@TableName("t_urr_market_order")
public class MarketOrderEntity {

    /**
     * 订单ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 区服ID。
     */
    private Integer serverId;

    /**
     * 订单类型：1卖单 2买单。
     */
    private Integer orderType;

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
     * 总数量。
     */
    private Long qtyTotal;

    /**
     * 剩余数量。
     */
    private Long qtyRemain;

    /**
     * 单价。
     */
    private Long priceEach;

    /**
     * 币种。
     */
    private String currencyCode;

    /**
     * 手续费基点。
     */
    private Integer feeRateBp;

    /**
     * 状态。
     */
    private Integer status;

    /**
     * 过期时间。
     */
    private LocalDateTime expireTime;

    /**
     * 乐观锁版本。
     */
    @Version
    private Integer version;

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