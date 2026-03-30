package com.urr.domain.craft;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 制造配方定义。
 */
@Data
@TableName("t_urr_recipe_def")
public class RecipeDefEntity {

    /**
     * 主键ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配方编码。
     */
    private String recipeCode;

    /**
     * 中文名。
     */
    private String nameZh;

    /**
     * 英文名。
     */
    private String nameEn;

    /**
     * 制造技能ID。
     */
    private Long craftSkillId;

    /**
     * 技能等级需求。
     */
    private Integer craftLevelReq;

    /**
     * 制造耗时（毫秒）。
     */
    private Integer craftTimeMs;

    /**
     * 消耗 JSON。
     */
    private String costJson;

    /**
     * 产出 JSON。
     */
    private String outputJson;

    /**
     * 经验。
     */
    private Long expGain;

    /**
     * 扩展 JSON。
     */
    private String metaJson;

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
     * 逻辑删除。
     */
    @TableLogic
    private Integer deleteFlag;
}