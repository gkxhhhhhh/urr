package com.urr.domain.action.task;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家当前运行动作槽位缓存。
 *
 * 说明：
 * 1. 这个对象是“单角色当前运行中动作”的通用热态槽位。
 * 2. 它不只服务采集，后续战斗、制造也可以复用。
 * 3. 这个 key 的核心目的，是承接“一个角色同一时刻只能有一个运行中动作”的规则。
 */
@Data
public class PlayerRunningTaskCache {

    /**
     * 当前运行任务ID。
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
     * 任务业务类型。
     * 例如：GATHER / BATTLE / CRAFT。
     */
    private String taskType;

    /**
     * 当前任务状态。
     * 例如：QUEUED / RUNNING。
     */
    private String status;

    /**
     * 任务开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 最近一次交互时间。
     */
    private LocalDateTime lastInteractTime;

    /**
     * 本次任务锁定的离线截止时间。
     */
    private LocalDateTime offlineExpireAt;

    /**
     * 缓存最近更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 判断当前槽位是不是采集任务。
     *
     * @return true-采集任务，false-不是采集任务
     */
    public boolean isGatherTask() {
        if (taskType == null) {
            return false;
        }
        return ActionTaskTypeEnum.GATHER.getCode().equalsIgnoreCase(taskType);
    }
}