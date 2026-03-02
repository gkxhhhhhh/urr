package com.urr.domain.battle;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_urr_encounter_wave")
public class EncounterWaveEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long encounterId;
    private Integer waveNo;
    private String monstersJson;
    private String rewardJson;
    private String metaJson;

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
