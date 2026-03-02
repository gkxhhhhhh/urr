package com.urr.domain.action;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_urr_action_def")
public class ActionDefEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long behaviorId;
    private Long categoryId;
    private Long subCategoryId;
    private String actionCode;
    private String actionName;
    private Integer status;
    private String actionKind;
    private Integer baseDurationMs;
    private Integer baseEnergyCost;
    private Integer minPlayerLevel;
    private Integer minSkillLevel;
    private String unlockConditionJson;
    private String durationScaleRule;
    private String rewardScaleRule;
    private String paramsJson;

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
