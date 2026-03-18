package com.urr.domain.skill;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 技能定义（t_urr_skill_def）。
 */
@Data
@TableName("t_urr_skill_def")
public class SkillDefEntity {

    /**
     * 主键ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 技能编码。
     */
    private String skillCode;

    /**
     * 技能名称（中文）。
     */
    private String nameZh;

    /**
     * 技能名称（英文）。
     */
    private String nameEn;

    /**
     * 技能类型。
     */
    private Integer skillType;

    /**
     * 最大等级。
     */
    private Integer maxLevel;

    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer deleteFlag;
}