package com.urr.domain.skill;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 玩家技能（t_urr_player_skill）。
 */
@Data
@TableName("t_urr_player_skill")
public class PlayerSkillEntity {

    /**
     * 主键ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 玩家ID。
     */
    private Long playerId;

    /**
     * 技能ID。
     */
    private Long skillId;

    /**
     * 技能等级。
     */
    private Integer skillLevel;

    /**
     * 技能经验。
     */
    private Long skillExp;

    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer deleteFlag;
}