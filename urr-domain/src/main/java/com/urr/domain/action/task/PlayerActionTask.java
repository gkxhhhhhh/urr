package com.urr.domain.action.task;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色动作任务通用模型。
 *
 * 说明：
 * 1. 这是纯代码层运行态模型，本次不直接绑定数据库表。
 * 2. 它表达“一个角色当前/最近的一条动作任务”的通用信息。
 * 3. 采集、战斗、制造等具体任务，在此基础上再做增量扩展。
 */
@Data
public class PlayerActionTask {

    /**
     * 任务ID。
     * 当前阶段只是模型字段，后续落库时可直接复用。
     */
    private Long id;

    /**
     * 玩家ID。
     */
    private Long playerId;

    /**
     * 区服ID。
     * 现有仓库大多数玩家运行态相关表都有 serverId，这里先预留。
     */
    private Integer serverId;

    /**
     * 动作编码。
     * 对应 ActionDefEntity.actionCode。
     */
    private String actionCode;

    /**
     * 任务业务类型。
     * 例如：GATHER / BATTLE / CRAFT。
     */
    private ActionTaskTypeEnum taskType;

    /**
     * 任务开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 本次任务锁定的最后交互时间。
     * 后续可用于离线超时判断。
     */
    private LocalDateTime lastInteractTime;

    /**
     * 本次任务最近一次任务层结算时间。
     * 当前会话只建模，不实现结算逻辑。
     */
    private LocalDateTime lastSettleTime;

    /**
     * 本次任务锁定的离线截止时间。
     * 到达该时间后，任务可被视为离线过期。
     */
    private LocalDateTime offlineExpireAt;

    /**
     * 奖励随机种子。
     * 放在任务根上，便于日志、排障和后续预生成计划复用。
     */
    private Long rewardSeed;

    /**
     * 任务当前状态。
     */
    private ActionTaskStatusEnum status;

    /**
     * 任务停止/结束原因。
     * 任务未结束时可为空。
     */
    private ActionTaskStopReasonEnum stopReason;

    /**
     * 判断任务当前是否正在运行。
     *
     * @return true-运行中，false-不是运行中
     */
    public boolean isRunning() {
        return ActionTaskStatusEnum.RUNNING.equals(status);
    }

    /**
     * 判断任务当前是否已经进入终态。
     *
     * @return true-终态，false-非终态
     */
    public boolean isTerminalStatus() {
        return status != null && status.isTerminal();
    }

    /**
     * 判断在指定时间点，任务是否已经达到离线截止时间。
     *
     * @param currentTime 当前时间
     * @return true-已过期，false-未过期
     */
    public boolean isOfflineExpired(LocalDateTime currentTime) {
        if (currentTime == null || offlineExpireAt == null) {
            return false;
        }
        return !currentTime.isBefore(offlineExpireAt);
    }
}