package com.urr.domain.action;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动作定义实体。
 */
@Data
@TableName("t_urr_action_def")
public class ActionDefEntity {

    /**
     * 主键ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 行为ID。
     */
    private Long behaviorId;

    /**
     * 分类ID。
     */
    private Long categoryId;

    /**
     * 子分类ID。
     */
    private Long subCategoryId;

    /**
     * 动作编码。
     */
    private String actionCode;

    /**
     * 动作名称。
     */
    private String actionName;

    /**
     * 动作排序。
     */
    private Integer sort;

    /**
     * 状态。
     */
    private Integer status;

    /**
     * 动作类型。
     */
    private String actionKind;

    /**
     * 基础时长（毫秒）。
     */
    private Integer baseDurationMs;

    /**
     * 基础能量消耗。
     */
    private Integer baseEnergyCost;

    /**
     * 最低玩家等级。
     */
    private Integer minPlayerLevel;

    /**
     * 最低技能等级。
     */
    private Integer minSkillLevel;

    /**
     * 解锁条件 JSON。
     */
    private String unlockConditionJson;

    /**
     * 时长缩放规则。
     */
    private String durationScaleRule;

    /**
     * 奖励缩放规则。
     */
    private String rewardScaleRule;

    /**
     * 参数 JSON。
     */
    private String paramsJson;

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