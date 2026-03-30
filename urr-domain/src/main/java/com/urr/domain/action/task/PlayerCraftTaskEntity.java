package com.urr.domain.action.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 玩家制造任务明细实体。
 */
@Data
@TableName("t_urr_player_craft_task")
public class PlayerCraftTaskEntity {

    /**
     * 主键ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 根任务ID。
     */
    private Long taskId;

    /**
     * 玩家ID。
     */
    private Long playerId;

    /**
     * 区服ID。
     */
    private Integer serverId;

    /**
     * 动作编码。
     */
    private String actionCode;

    /**
     * 目标次数。
     * 0 表示无限次数。
     */
    private Long targetCount;

    /**
     * 已完成次数。
     */
    private Long completedCount;

    /**
     * 配方快照 JSON。
     */
    private String recipeSnapshotJson;

    /**
     * 下一轮完成时间戳（毫秒）。
     */
    private Long nextRoundFinishTime;

    /**
     * 明细状态。
     */
    private String status;

    /**
     * 创建时间戳（毫秒）。
     */
    private Long createTime;

    /**
     * 更新时间戳（毫秒）。
     */
    private Long updateTime;
}