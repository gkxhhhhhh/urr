package com.urr.domain.item;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家装备实例实体。
 */
@Data
@TableName("t_urr_player_equip")
public class PlayerEquipEntity {

    /**
     * 装备实例ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 玩家ID。
     */
    private Long playerId;

    /**
     * 区服ID。
     */
    private Integer serverId;

    /**
     * 装备物品定义ID。
     */
    private Long itemId;

    /**
     * 装备槽位。
     */
    private Integer equipSlot;

    /**
     * 等级需求。
     */
    private Integer levelReq;

    /**
     * 绑定类型。
     */
    private Integer bindType;

    /**
     * 绑定到的玩家ID。
     */
    private Long bindPlayerId;

    /**
     * 耐久。
     */
    private Integer durability;

    /**
     * 状态。
     */
    private Integer state;

    /**
     * 随机词条/强化/镶嵌等扩展JSON。
     */
    private String attrJson;

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
     * 更新人。
     */
    private String updateUser;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;

    /**
     * 删除标记。
     */
    @TableLogic
    private Integer deleteFlag;
}
