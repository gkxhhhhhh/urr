package com.urr.app.action.task;

import com.urr.domain.action.ActionDefEntity;
import com.urr.domain.action.task.ActionTaskConstants;
import com.urr.domain.action.task.ActionTaskStatusEnum;
import com.urr.domain.action.task.ActionTaskTypeEnum;
import com.urr.domain.action.task.GatherTaskStatSnapshot;
import com.urr.domain.action.task.PlayerGatherTask;
import com.urr.domain.player.PlayerEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 采集任务工厂。
 *
 * 说明：
 * 1. 这里只做“启动时的一次性初始化组装”。
 * 2. 不负责数据库落库，不负责 Redis 写入。
 * 3. 本次把动作对应的掉落配置和经验配置一并锁进快照，避免任务中途受配置变更影响。
 */
@Component
@RequiredArgsConstructor
public class GatherTaskFactory {

    /**
     * 采集奖励生成器。
     */
    private final GatherTaskRewardGenerator gatherTaskRewardGenerator;

    /**
     * 组装一条“立即运行”的采集任务。
     *
     * @param player 玩家
     * @param action 动作定义
     * @param targetCount 目标轮次
     * @param operateTime 本次操作时间
     * @return 采集任务
     */
    public PlayerGatherTask createRunningTask(PlayerEntity player,
                                              ActionDefEntity action,
                                              Long targetCount,
                                              LocalDateTime operateTime) {
        validateInput(player, action, operateTime);

        long normalizedTargetCount = normalizeTargetCount(targetCount);
        long rewardSeed = generateRewardSeed();
        int offlineMinutesLimit = resolveOfflineMinutesLimit();
        int segmentSize = ActionTaskConstants.DEFAULT_SEGMENT_SIZE;

        PlayerGatherTask task = new PlayerGatherTask();
        task.setPlayerId(player.getId());
        task.setServerId(player.getServerId());
        task.setActionCode(action.getActionCode());
        task.setTaskType(ActionTaskTypeEnum.GATHER);
        task.setStartTime(operateTime);
        task.setLastInteractTime(operateTime);
        task.setLastSettleTime(operateTime);
        task.setOfflineExpireAt(operateTime.plusMinutes(offlineMinutesLimit));
        task.setRewardSeed(rewardSeed);
        task.setStatus(ActionTaskStatusEnum.RUNNING);
        task.setStopReason(null);

        task.setTargetCount(normalizedTargetCount);
        task.setCompletedCount(0L);
        task.setFlushedCount(0L);
        task.setSegmentSize(segmentSize);
        task.setCurrentSegmentStart(1L);
        task.setCurrentSegmentEnd(resolveCurrentSegmentEnd(normalizedTargetCount, segmentSize));
        task.setStatSnapshot(buildStatSnapshot(action, rewardSeed, offlineMinutesLimit));
        task.setCurrentSegmentRewardPlanJson(null);
        task.setPendingRewardPoolJson(null);
        return task;
    }

    /**
     * 生成采集任务快照。
     *
     * @param action 动作定义
     * @param rewardSeed 奖励随机种子
     * @param offlineMinutesLimit 离线分钟上限
     * @return 采集快照
     */
    public GatherTaskStatSnapshot buildStatSnapshot(ActionDefEntity action,
                                                    Long rewardSeed,
                                                    int offlineMinutesLimit) {
        GatherTaskRewardGenerator.ActionRewardConfig rewardConfig =
                gatherTaskRewardGenerator.requireActionRewardConfig(action.getActionCode());

        GatherTaskStatSnapshot snapshot = new GatherTaskStatSnapshot();
        snapshot.setGatherLevel(resolveGatherLevel(action));
        snapshot.setEquipmentEffectSnapshotJson(buildDefaultEquipmentEffectSnapshotJson());
        snapshot.setGatherDurationMs(resolveGatherDurationMs(action));
        snapshot.setGatherEfficiency(BigDecimal.ONE);
        snapshot.setOfflineMinutesLimit(offlineMinutesLimit);
        snapshot.setRewardSeed(rewardSeed);

        snapshot.setRewardListJson(gatherTaskRewardGenerator.buildSnapshotRewardListJson(rewardConfig));

        if (rewardConfig.getRewardItems() != null && rewardConfig.getRewardItems().size() == 1) {
            snapshot.setItemCode(rewardConfig.getRewardItems().get(0).getItemCode());
        } else {
            snapshot.setItemCode(null);
        }

        snapshot.setSkillCode(rewardConfig.getSkillCode());
        snapshot.setExpGain(rewardConfig.getExpGain());
        snapshot.setCriticalRate(rewardConfig.getCriticalRate());
        snapshot.setQuantityChance1(rewardConfig.getQuantityChance1());
        snapshot.setQuantityChance2(rewardConfig.getQuantityChance2());
        snapshot.setQuantityChance3(rewardConfig.getQuantityChance3());
        return snapshot;
    }

    /**
     * 规范化目标轮次。
     *
     * @param targetCount 目标轮次
     * @return 规范化后的目标轮次
     */
    public long normalizeTargetCount(Long targetCount) {
        if (targetCount == null) {
            return ActionTaskConstants.INFINITE_TARGET_COUNT;
        }
        long value = targetCount.longValue();
        if (value == ActionTaskConstants.INFINITE_TARGET_COUNT) {
            return value;
        }
        if (value <= 0L) {
            throw new IllegalArgumentException("targetCount 只能是正整数或 -1");
        }
        return value;
    }

    /**
     * 解析当前 segment 结束轮次。
     *
     * @param targetCount 目标轮次
     * @param segmentSize 分段大小
     * @return 当前 segment 结束轮次
     */
    private long resolveCurrentSegmentEnd(long targetCount, int segmentSize) {
        if (targetCount == ActionTaskConstants.INFINITE_TARGET_COUNT) {
            return segmentSize;
        }
        return Math.min(targetCount, segmentSize);
    }

    /**
     * 解析采集等级。
     *
     * @param action 动作定义
     * @return 当前采集等级
     */
    private int resolveGatherLevel(ActionDefEntity action) {
        if (action == null || action.getMinSkillLevel() == null || action.getMinSkillLevel() <= 0) {
            return 1;
        }
        return action.getMinSkillLevel();
    }

    /**
     * 解析当前采集时长。
     *
     * @param action 动作定义
     * @return 每轮耗时（毫秒）
     */
    private int resolveGatherDurationMs(ActionDefEntity action) {
        if (action == null || action.getBaseDurationMs() == null || action.getBaseDurationMs() <= 0) {
            return 1000;
        }
        return action.getBaseDurationMs();
    }

    /**
     * 解析当前允许离线分钟上限。
     *
     * @return 离线分钟上限
     */
    private int resolveOfflineMinutesLimit() {
        return ActionTaskConstants.DEFAULT_OFFLINE_HOURS * 60;
    }

    /**
     * 生成奖励随机种子。
     *
     * @return 奖励随机种子
     */
    private long generateRewardSeed() {
        return ThreadLocalRandom.current().nextLong(1L, Long.MAX_VALUE);
    }

    /**
     * 构建默认的装备影响快照。
     *
     * @return 装备影响快照 JSON
     */
    private String buildDefaultEquipmentEffectSnapshotJson() {
        return "{\"durationRate\":1,\"efficiencyRate\":1,\"offlineBonusHours\":0}";
    }

    /**
     * 校验工厂入参。
     *
     * @param player 玩家
     * @param action 动作定义
     * @param operateTime 操作时间
     */
    private void validateInput(PlayerEntity player, ActionDefEntity action, LocalDateTime operateTime) {
        if (player == null) {
            throw new IllegalArgumentException("player不能为空");
        }
        if (action == null) {
            throw new IllegalArgumentException("action不能为空");
        }
        if (operateTime == null) {
            throw new IllegalArgumentException("operateTime不能为空");
        }
    }
}
