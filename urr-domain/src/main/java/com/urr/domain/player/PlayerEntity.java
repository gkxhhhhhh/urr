package com.urr.domain.player;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家角色（t_urr_player）。
 *
 * 说明：
 * 1. 当前项目先用 MyBatis-Plus Entity 直接映射表。
 * 2. 本次补充 lastInteractTime，用于表达角色最近一次交互时间。
 */
@Data
@TableName("t_urr_player")
public class PlayerEntity {

    /**
     * 玩家ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账号ID。
     */
    private Long accountId;

    /**
     * 区服ID。
     */
    private Integer serverId;

    /**
     * 角色类型。
     */
    private Integer type;

    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 头像。
     */
    private String avatar;

    /**
     * 等级。
     */
    private Integer level;

    /**
     * 经验值。
     */
    private Long exp;

    /**
     * 战力。
     */
    private Long power;

    /**
     * 体力。
     */
    private Integer energy;

    /**
     * 体力最后刷新时间。
     */
    private LocalDateTime energyUpdateTime;

    /**
     * 最近在线时间。
     */
    private LocalDateTime lastOnlineTime;

    /**
     * 最近一次交互时间。
     */
    private LocalDateTime lastInteractTime;

    /**
     * 最近一次结算时间。
     */
    private LocalDateTime lastSettleTime;

    /**
     * 乐观锁版本号。
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