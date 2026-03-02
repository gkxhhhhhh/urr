package com.urr.domain.battle;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_urr_player_dungeon_progress")
public class PlayerDungeonProgressEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;
    private Integer serverId;
    private Long dungeonId;
    private Integer bestStage;
    private Integer bestTimeMs;
    private Long totalRuns;

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
