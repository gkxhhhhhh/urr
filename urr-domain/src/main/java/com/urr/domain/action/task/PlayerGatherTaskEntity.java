package com.urr.domain.action.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集任务扩展实体，映射 t_urr_player_gather_task。
 *
 * 说明：
 * 1. 一条采集任务，对应一条动作任务根记录。
 * 2. taskId = t_urr_player_activity.id。
 * 3. 采集专属真相字段全部落在这里，避免污染通用根表。
 */
@Data
@TableName("t_urr_player_gather_task")
public class PlayerGatherTaskEntity {

    /**
     * 对应动作任务根表ID。
     */
    @TableId(value = "task_id", type = IdType.INPUT)
    private Long taskId;

    /**
     * 目标轮次。
     * -1 表示无限次数。
     */
    private Long targetCount;

    /**
     * 已完成轮次。
     *
     * 语义：
     * 1. 已逻辑完成。
     * 2. 奖励逻辑上已归属玩家。
     * 3. 但不一定已经正式入库。
     */
    private Long completedCount;

    /**
     * 已正式刷入正式库存的轮次。
     */
    private Long flushedCount;

    /**
     * 当前分段开始轮次（含）。
     */
    private Long currentSegmentStart;

    /**
     * 当前分段结束轮次（含）。
     */
    private Long currentSegmentEnd;

    /**
     * 分段大小。
     */
    private Integer segmentSize;

    /**
     * 采集任务启动时锁定的属性快照。
     * JSON 字符串。
     */
    private String statSnapshot;

    /**
     * 当前分段锁定奖励计划。
     * JSON 字符串。
     */
    private String currentSegmentRewardPlanJson;

    /**
     * 已完成但未正式入库的收益池。
     * JSON 字符串。
     */
    private String pendingRewardPoolJson;

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
     * 修改人。
     */
    private String updateUser;

    /**
     * 修改时间。
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer deleteFlag;
}