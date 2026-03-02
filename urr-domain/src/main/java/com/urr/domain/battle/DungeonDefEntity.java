package com.urr.domain.battle;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_urr_dungeon_def")
public class DungeonDefEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String dungeonCode;
    private String nameZh;
    private String nameEn;
    private Integer minLevel;
    private Integer energyCost;
    private Integer battleTimeMs;
    private String dropTableJson;
    private String metaJson;
    private String remarks;
    private String createUser;
    private LocalDateTime createTime;
    private String updateUser;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleteFlag;
}
