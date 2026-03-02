package com.urr.domain.battle;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_urr_dungeon_run_log")
public class DungeonRunLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;
    private Integer serverId;
    private Long dungeonId;
    private Integer stage;
    private Integer result;
    private Integer energyCost;
    private Integer battleTimeMs;
    private String rewardJson;
    private Long seed;
    private LocalDateTime runTime;
    private String remarks;
    private String createUser;
    private LocalDateTime createTime;
    private String updateUser;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleteFlag;
}
