package com.urr.app.action.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urr.app.action.task.command.AdvanceGatherTaskCommand;
import com.urr.app.action.task.result.AdvanceGatherTaskResult;
import com.urr.domain.action.task.GatherTaskPendingRewardPoolCache;
import com.urr.domain.action.task.GatherTaskRewardEntry;
import com.urr.domain.action.task.GatherTaskRewardPool;
import com.urr.domain.action.task.GatherTaskSegmentPlanCache;
import com.urr.domain.action.task.GatherTaskSegmentRewardPlan;
import com.urr.domain.action.task.PlayerGatherTask;
import com.urr.domain.action.task.PlayerGatherTaskRuntimeCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 采集任务最小懒推进服务。
 *
 * 说明：
 * 1. 这里专门负责“把当前采集任务按需推进到某个时刻”。
 * 2. 默认 advanceTo 仍然只落 completedCount / lastSettleTime / pending_reward_pool / current segment。
 * 3. 新增 advanceToAndApplyDelta，专门给运行中实时入包使用。
 * 4. 实时入包只消费“本次新增轮次的 delta 列表”，不会重复消费历史累计池。
 * 5. 推进前会先做一次运行态兜底恢复，确保 Redis 丢失后仍可继续推进。
 */
@Service
public class GatherTaskAdvanceService {

    /**
     * 采集任务 DB 仓储。
     */
    private final PlayerGatherTaskRepository playerGatherTaskRepository;

    /**
     * 采集任务 Redis 仓储。
     */
    private final PlayerGatherTaskRedisRepository playerGatherTaskRedisRepository;

    /**
     * 奖励生成器。
     */
    private final GatherTaskRewardGenerator gatherTaskRewardGenerator;

    /**
     * pending_reward_pool 聚合器。
     */
    private final GatherTaskPendingRewardAggregator gatherTaskPendingRewardAggregator;

    /**
     * 当前分段规划器。
     */
    private final GatherTaskSegmentPlanner gatherTaskSegmentPlanner;

    /**
     * 采集任务运行态恢复服务。
     */
    private final GatherTaskRuntimeRecoveryService gatherTaskRuntimeRecoveryService;

    /**
     * 正式库存入库适配器。
     */
    private final GatherTaskRewardMaterializer gatherTaskRewardMaterializer;

    /**
     * 采集经验正式入账服务。
     */
    private final GatherTaskSkillExpService gatherTaskSkillExpService;

    /**
     * JSON 编解码器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造方法。
     *
     * @param playerGatherTaskRepository 采集任务 DB 仓储
     * @param playerGatherTaskRedisRepository 采集任务 Redis 仓储
     * @param gatherTaskRewardGenerator 奖励生成器
     * @param gatherTaskPendingRewardAggregator 待刷收益池聚合器
     * @param gatherTaskSegmentPlanner 分段规划器
     * @param gatherTaskRuntimeRecoveryService 运行态恢复服务
     * @param gatherTaskRewardMaterializer 正式库存入库适配器
     * @param gatherTaskSkillExpService 采集经验正式入账服务
     * @param objectMapper JSON 编解码器
     */
    public GatherTaskAdvanceService(PlayerGatherTaskRepository playerGatherTaskRepository,
                                    PlayerGatherTaskRedisRepository playerGatherTaskRedisRepository,
                                    GatherTaskRewardGenerator gatherTaskRewardGenerator,
                                    GatherTaskPendingRewardAggregator gatherTaskPendingRewardAggregator,
                                    GatherTaskSegmentPlanner gatherTaskSegmentPlanner,
                                    GatherTaskRuntimeRecoveryService gatherTaskRuntimeRecoveryService,
                                    GatherTaskRewardMaterializer gatherTaskRewardMaterializer,
                                    GatherTaskSkillExpService gatherTaskSkillExpService,
                                    ObjectMapper objectMapper) {
        this.playerGatherTaskRepository = playerGatherTaskRepository;
        this.playerGatherTaskRedisRepository = playerGatherTaskRedisRepository;
        this.gatherTaskRewardGenerator = gatherTaskRewardGenerator;
        this.gatherTaskPendingRewardAggregator = gatherTaskPendingRewardAggregator;
        this.gatherTaskSegmentPlanner = gatherTaskSegmentPlanner;
        this.gatherTaskRuntimeRecoveryService = gatherTaskRuntimeRecoveryService;
        this.gatherTaskRewardMaterializer = gatherTaskRewardMaterializer;
        this.gatherTaskSkillExpService = gatherTaskSkillExpService;
        this.objectMapper = objectMapper;
    }

    /**
     * 把当前采集任务推进到指定时刻。
     *
     * 说明：
     * 1. completedCount = 已完成轮次，奖励逻辑上已归属玩家。
     * 2. 本方法只推进“新增完成轮次”，不会重复结算已完成部分。
     * 3. 本方法不会把 pending_reward_pool 正式刷入背包/钱包。
     *
     * @param command 推进命令
     * @return 推进结果
     */
    @Transactional(rollbackFor = Exception.class)
    public AdvanceGatherTaskResult advanceTo(AdvanceGatherTaskCommand command) {
        return doAdvance(command, false);
    }

    /**
     * 把当前采集任务推进到指定时刻，并把“本次新增 delta”实时入包。
     *
     * 说明：
     * 1. 这个方法只给运行中实时刷新使用。
     * 2. 历史 pending_reward_pool 会先补一次正式入库，然后清空。
     * 3. 本次新增轮次只按 delta 列表实时入库，不再走累计池 flush。
     * 4. 本次成功后，flushedCount 会直接推进到 completedCount。
     *
     * @param command 推进命令
     * @return 推进结果
     */
    @Transactional(rollbackFor = Exception.class)
    public AdvanceGatherTaskResult advanceToAndApplyDelta(AdvanceGatherTaskCommand command) {
        return doAdvance(command, true);
    }

    /**
     * 执行一次推进。
     *
     * @param command 推进命令
     * @param applyDelta 是否实时入包
     * @return 推进结果
     */
    private AdvanceGatherTaskResult doAdvance(AdvanceGatherTaskCommand command, boolean applyDelta) {
        validateCommand(command);

        PlayerGatherTask task = playerGatherTaskRepository.findByTaskId(command.getTaskId());
        if (task == null) {
            throw new IllegalArgumentException("采集任务不存在，taskId=" + command.getTaskId());
        }

        gatherTaskRuntimeRecoveryService.recoverHotStateIfNecessary(task);

        PlayerGatherTaskRuntimeCache runtimeCache = playerGatherTaskRedisRepository.findRuntimeByTaskId(task.getId());
        GatherTaskSegmentPlanCache segmentPlanCache = playerGatherTaskRedisRepository.findSegmentPlanByTaskId(task.getId());
        GatherTaskPendingRewardPoolCache pendingRewardPoolCache = playerGatherTaskRedisRepository.findPendingRewardPoolByTaskId(task.getId());

        AdvanceGatherTaskResult result = buildBaseResult(task, command.getAdvanceTime());
        if (!task.isRunning()) {
            boolean redisUpdated = false;
            if (task.hasPendingRewardPool() && pendingRewardPoolCache == null) {
                playerGatherTaskRedisRepository.savePendingRewardPool(
                        PlayerGatherTaskRedisConverter.toPendingRewardPoolCache(task)
                );
                redisUpdated = true;
            }
            result.setDbUpdated(Boolean.FALSE);
            result.setRedisUpdated(redisUpdated);
            return result;
        }

        LocalDateTime effectiveNow = resolveEffectiveNow(task, command.getAdvanceTime());
        result.setEffectiveNow(effectiveNow);

        long beforeCompletedCount = task.getSafeCompletedCount();
        long shouldCompletedCount = calculateShouldCompletedCount(task, effectiveNow);
        long advancedRoundCount = shouldCompletedCount - beforeCompletedCount;
        if (advancedRoundCount < 0L) {
            advancedRoundCount = 0L;
        }

        LocalDateTime newLastSettleTime = resolveNewLastSettleTime(task.getLastSettleTime(), effectiveNow);
        boolean dbUpdated = false;

        if (applyDelta) {
            boolean rewardApplied = applyHistoricalPendingIfNecessary(task, pendingRewardPoolCache);
            if (advancedRoundCount > 0L) {
                List<GatherTaskRewardEntry> newRewardList = generateRewardList(task,
                        beforeCompletedCount + 1L,
                        shouldCompletedCount);
                gatherTaskRewardMaterializer.materialize(task, newRewardList);
                gatherTaskSkillExpService.applySkillExp(task, advancedRoundCount);
                rewardApplied = true;
            }
            if (rewardApplied || task.getSafeFlushedCount() != shouldCompletedCount || task.hasPendingRewardPool()) {
                task.setFlushedCount(shouldCompletedCount);
                task.setPendingRewardPoolJson(null);
                dbUpdated = true;
            }
            if (advancedRoundCount > 0L) {
                task.setCompletedCount(shouldCompletedCount);
                dbUpdated = true;
            }
        } else {
            if (advancedRoundCount > 0L) {
                List<GatherTaskRewardEntry> newRewardList = generateRewardList(task,
                        beforeCompletedCount + 1L,
                        shouldCompletedCount);
                GatherTaskRewardPool rewardPool = readPendingRewardPool(task, pendingRewardPoolCache);
                gatherTaskPendingRewardAggregator.mergeRewards(rewardPool, newRewardList);
                task.setCompletedCount(shouldCompletedCount);
                task.setPendingRewardPoolJson(writeJson(rewardPool));
                dbUpdated = true;
            }
        }

        if (!sameTime(task.getLastSettleTime(), newLastSettleTime)) {
            task.setLastSettleTime(newLastSettleTime);
            dbUpdated = true;
        }

        boolean segmentChanged = rebuildCurrentSegmentPlan(task, segmentPlanCache);
        if (segmentChanged) {
            dbUpdated = true;
        }

        if (dbUpdated) {
            playerGatherTaskRepository.update(task);
        }

        boolean redisUpdated;
        if (dbUpdated) {
            playerGatherTaskRedisRepository.saveTaskHotState(task);
            redisUpdated = true;
        } else {
            redisUpdated = refreshRedisIfNecessary(task, runtimeCache, segmentPlanCache, pendingRewardPoolCache);
        }

        result.setAfterCompletedCount(task.getSafeCompletedCount());
        result.setAdvancedRoundCount(advancedRoundCount);
        result.setAfterLastSettleTime(task.getLastSettleTime());
        result.setCurrentSegmentStart(task.getCurrentSegmentStart());
        result.setCurrentSegmentEnd(task.getCurrentSegmentEnd());
        result.setDbUpdated(dbUpdated);
        result.setRedisUpdated(redisUpdated);
        return result;
    }

    /**
     * 把历史 pending_reward_pool 先补一次正式入库。
     *
     * @param task 采集任务
     * @param pendingRewardPoolCache 待刷收益池缓存
     * @return true-本次有正式入库，false-没有
     */
    private boolean applyHistoricalPendingIfNecessary(PlayerGatherTask task,
                                                      GatherTaskPendingRewardPoolCache pendingRewardPoolCache) {
        long pendingRoundCount = task.getPendingRewardRoundCount();
        if (pendingRoundCount <= 0L) {
            return false;
        }

        GatherTaskRewardPool rewardPool = readPendingRewardPool(task, pendingRewardPoolCache);
        if (rewardPool == null || rewardPool.getSafeRewardList().isEmpty()) {
            throw new IllegalStateException("存在待刷轮次，但 pending_reward_pool 为空，taskId=" + task.getId());
        }

        gatherTaskRewardMaterializer.materialize(task, rewardPool);
        gatherTaskSkillExpService.applySkillExp(task, pendingRoundCount);
        return true;
    }

    /**
     * 生成本次新增完成轮次的奖励列表。
     *
     * @param task 采集任务
     * @param fromRoundIndex 起始轮次（含）
     * @param toRoundIndex 结束轮次（含）
     * @return 新增奖励列表
     */
    private List<GatherTaskRewardEntry> generateRewardList(PlayerGatherTask task,
                                                           long fromRoundIndex,
                                                           long toRoundIndex) {
        List<GatherTaskRewardEntry> rewardList = new ArrayList<GatherTaskRewardEntry>();
        if (toRoundIndex < fromRoundIndex) {
            return rewardList;
        }

        for (long roundIndex = fromRoundIndex; roundIndex <= toRoundIndex; roundIndex++) {
            List<GatherTaskRewardEntry> currentRoundRewardList =
                    gatherTaskRewardGenerator.generateRoundRewards(task, roundIndex);
            for (int i = 0; i < currentRoundRewardList.size(); i++) {
                rewardList.add(currentRoundRewardList.get(i));
            }
        }
        return rewardList;
    }

    /**
     * 重新构建当前 segment 最小计划。
     *
     * @param task 采集任务
     * @param segmentPlanCache Redis 中已有的 segment plan 缓存
     * @return true-发生变化，false-未变化
     */
    private boolean rebuildCurrentSegmentPlan(PlayerGatherTask task, GatherTaskSegmentPlanCache segmentPlanCache) {
        String oldPlanJson = task.getCurrentSegmentRewardPlanJson();
        if (!StringUtils.hasText(oldPlanJson) && segmentPlanCache != null && segmentPlanCache.hasPlanJson()) {
            oldPlanJson = segmentPlanCache.getPlanJson();
        }

        Long oldSegmentStart = task.getCurrentSegmentStart();
        Long oldSegmentEnd = task.getCurrentSegmentEnd();

        GatherTaskSegmentRewardPlan segmentRewardPlan = gatherTaskSegmentPlanner.buildCurrentSegmentPlan(task);
        if (segmentRewardPlan == null) {
            task.setCurrentSegmentStart(null);
            task.setCurrentSegmentEnd(null);
            task.setCurrentSegmentRewardPlanJson(null);
            return oldSegmentStart != null || oldSegmentEnd != null || StringUtils.hasText(oldPlanJson);
        }

        task.setCurrentSegmentStart(segmentRewardPlan.getSegmentStart());
        task.setCurrentSegmentEnd(segmentRewardPlan.getSegmentEnd());
        task.setSegmentSize(segmentRewardPlan.getSegmentSize());

        String newPlanJson = writeJson(segmentRewardPlan);
        task.setCurrentSegmentRewardPlanJson(newPlanJson);

        if (!sameLong(oldSegmentStart, task.getCurrentSegmentStart())) {
            return true;
        }
        if (!sameLong(oldSegmentEnd, task.getCurrentSegmentEnd())) {
            return true;
        }
        return !safeEquals(oldPlanJson, newPlanJson);
    }

    /**
     * 从任务真相层和 Redis 热态层中读取 pending_reward_pool。
     *
     * @param task 采集任务
     * @param pendingRewardPoolCache 待刷收益池缓存
     * @return 收益池对象
     */
    private GatherTaskRewardPool readPendingRewardPool(PlayerGatherTask task,
                                                       GatherTaskPendingRewardPoolCache pendingRewardPoolCache) {
        String rewardPoolJson = task.getPendingRewardPoolJson();
        if (!StringUtils.hasText(rewardPoolJson)
                && pendingRewardPoolCache != null
                && pendingRewardPoolCache.hasRewardPoolJson()) {
            rewardPoolJson = pendingRewardPoolCache.getRewardPoolJson();
        }
        if (!StringUtils.hasText(rewardPoolJson)) {
            return new GatherTaskRewardPool();
        }
        try {
            return objectMapper.readValue(rewardPoolJson, GatherTaskRewardPool.class);
        } catch (IOException e) {
            throw new IllegalStateException("pending_reward_pool_json 反序列化失败，taskId=" + task.getId(), e);
        }
    }

    /**
     * 计算在 effectiveNow 时刻，任务理论上应完成到多少轮。
     *
     * @param task 采集任务
     * @param effectiveNow 实际生效时间
     * @return 应完成轮数
     */
    private long calculateShouldCompletedCount(PlayerGatherTask task, LocalDateTime effectiveNow) {
        if (task.getStartTime() == null || effectiveNow == null) {
            return task.getSafeCompletedCount();
        }
        if (task.getStatSnapshot() == null
                || task.getStatSnapshot().getGatherDurationMs() == null
                || task.getStatSnapshot().getGatherDurationMs() <= 0) {
            throw new IllegalStateException("采集任务缺少有效的 gatherDurationMs，taskId=" + task.getId());
        }
        if (effectiveNow.isBefore(task.getStartTime())) {
            return task.getSafeCompletedCount();
        }

        long durationMs = Duration.between(task.getStartTime(), effectiveNow).toMillis();
        long theoreticalCompletedCount = durationMs / task.getStatSnapshot().getGatherDurationMs();
        if (task.isInfiniteTarget()) {
            return theoreticalCompletedCount;
        }
        if (task.getTargetCount() == null || task.getTargetCount() <= 0L) {
            return theoreticalCompletedCount;
        }
        return Math.min(theoreticalCompletedCount, task.getTargetCount());
    }

    /**
     * 计算本次推进真正生效的时间。
     *
     * @param task 采集任务
     * @param requestTime 请求推进时间
     * @return 生效时间
     */
    private LocalDateTime resolveEffectiveNow(PlayerGatherTask task, LocalDateTime requestTime) {
        if (task == null) {
            return requestTime;
        }
        if (requestTime == null) {
            return task.getOfflineExpireAt();
        }
        if (task.getOfflineExpireAt() == null) {
            return requestTime;
        }
        if (requestTime.isAfter(task.getOfflineExpireAt())) {
            return task.getOfflineExpireAt();
        }
        return requestTime;
    }

    /**
     * 计算新的 lastSettleTime。
     *
     * @param oldLastSettleTime 旧结算时间
     * @param effectiveNow 本次生效时间
     * @return 新结算时间
     */
    private LocalDateTime resolveNewLastSettleTime(LocalDateTime oldLastSettleTime, LocalDateTime effectiveNow) {
        if (effectiveNow == null) {
            return oldLastSettleTime;
        }
        if (oldLastSettleTime == null) {
            return effectiveNow;
        }
        if (effectiveNow.isAfter(oldLastSettleTime)) {
            return effectiveNow;
        }
        return oldLastSettleTime;
    }

    /**
     * 当 Redis 热态缺失时，按当前 DB 真相层补一份最小热态。
     *
     * @param task 采集任务
     * @param runtimeCache 运行态缓存
     * @param segmentPlanCache segment plan 缓存
     * @param pendingRewardPoolCache pending_reward_pool 缓存
     * @return true-刷新了 Redis，false-未刷新
     */
    private boolean refreshRedisIfNecessary(PlayerGatherTask task,
                                            PlayerGatherTaskRuntimeCache runtimeCache,
                                            GatherTaskSegmentPlanCache segmentPlanCache,
                                            GatherTaskPendingRewardPoolCache pendingRewardPoolCache) {
        if (runtimeCache == null) {
            playerGatherTaskRedisRepository.saveTaskHotState(task);
            return true;
        }
        if (task.getCurrentSegmentStart() != null
                && task.getCurrentSegmentEnd() != null
                && segmentPlanCache == null) {
            playerGatherTaskRedisRepository.saveTaskHotState(task);
            return true;
        }
        if (task.hasPendingRewardPool() && pendingRewardPoolCache == null) {
            playerGatherTaskRedisRepository.saveTaskHotState(task);
            return true;
        }
        return false;
    }

    /**
     * 组装基础结果。
     *
     * @param task 采集任务
     * @param requestTime 请求时间
     * @return 推进结果
     */
    private AdvanceGatherTaskResult buildBaseResult(PlayerGatherTask task, LocalDateTime requestTime) {
        AdvanceGatherTaskResult result = new AdvanceGatherTaskResult();
        result.setTaskId(task.getId());
        result.setRequestTime(requestTime);
        result.setBeforeCompletedCount(task.getSafeCompletedCount());
        result.setAfterCompletedCount(task.getSafeCompletedCount());
        result.setAdvancedRoundCount(0L);
        result.setBeforeLastSettleTime(task.getLastSettleTime());
        result.setAfterLastSettleTime(task.getLastSettleTime());
        result.setCurrentSegmentStart(task.getCurrentSegmentStart());
        result.setCurrentSegmentEnd(task.getCurrentSegmentEnd());
        result.setDbUpdated(Boolean.FALSE);
        result.setRedisUpdated(Boolean.FALSE);
        return result;
    }

    /**
     * 把对象序列化成 JSON。
     *
     * @param value 对象
     * @return JSON 字符串
     */
    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("对象序列化失败", e);
        }
    }

    /**
     * 校验推进命令。
     *
     * @param command 推进命令
     */
    private void validateCommand(AdvanceGatherTaskCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("推进采集任务命令不能为空");
        }
        if (command.getTaskId() == null) {
            throw new IllegalArgumentException("taskId不能为空");
        }
        if (command.getAdvanceTime() == null) {
            throw new IllegalArgumentException("advanceTime不能为空");
        }
    }

    /**
     * 判断两个 Long 是否相等。
     *
     * @param left 左值
     * @param right 右值
     * @return true-相等，false-不等
     */
    private boolean sameLong(Long left, Long right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    /**
     * 判断两个时间是否相等。
     *
     * @param left 左值
     * @param right 右值
     * @return true-相等，false-不等
     */
    private boolean sameTime(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    /**
     * 安全比较字符串。
     *
     * @param left 左值
     * @param right 右值
     * @return true-相等，false-不等
     */
    private boolean safeEquals(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }
}
