package com.urr.domain.player;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家角色（t_urr_player）
 * 说明：先用 MyBatis-Plus 直接映射，后续领域模型复杂后可再做 DO/Domain 分离。
 */
@Data
@TableName("t_urr_player")
public class PlayerEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long accountId;

    private Integer serverId;

    private Integer type;

    private String nickname;

    private String avatar;

    private Integer level;

    private Long exp;

    private Long power;

    private Integer energy;

    private LocalDateTime energyUpdateTime;

    private LocalDateTime lastOnlineTime;

    private LocalDateTime lastSettleTime;

    @Version
    private Integer version;

    private String remarks;

    private String createUser;
    private LocalDateTime createTime;

    private String updateUser;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleteFlag;
}
