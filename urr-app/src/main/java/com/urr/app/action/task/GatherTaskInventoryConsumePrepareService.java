package com.urr.app.action.task;

import com.urr.app.action.task.command.AdvanceGatherTaskCommand;
import com.urr.app.action.task.result.AdvanceGatherTaskResult;
import com.urr.app.action.task.result.PendingRewardFlushResult;
import com.urr.app.action.task.result.PrepareConsumeInventoryResult;
import com.urr.domain.action.task.ActionTaskTypeEnum;
import com.urr.domain.action.task.PlayerActionTask;
import com.urr.domain.action.task.PlayerGatherTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 正式库存消耗前的采集 flush 准备服务。
 *
 * 说明：
 * 1. 任何会正式扣减库存的业务，在真正扣减前都应该先调这个服务。
 * 2. 顺序固定为：先 advance 当前运行采集，再 flush 玩家所有 pending 采集任务。
 * 3. 这个服务只做“正式扣减前兜底”，不直接承担卖出/制造/任务提交流程。
 * 4. 这个服务复用现有采集推进与 flush 链路，不额外发明新库存系统。
 */
@Service
@RequiredArgsConstructor
public class GatherTaskInventoryConsumePrepareService {

    /**
     * 动作根任务仓储。
     */
    private final PlayerActionTaskRepository playerActionTaskRepository;

    /**
     * 采集任务仓储。
     */
    private final PlayerGatherTaskRepository playerGatherTaskRepository;

    /**
     * 采集任务最小懒推进服务。
     */
    private final GatherTaskAdvanceService gatherTaskAdvanceService;

    /**
     * 采集任务 pending flush 服务。
     */
    private final GatherTaskPendingRewardFlushService gatherTaskPendingRewardFlushService;

    /**
     * 使用当前时间执行一次“库存消耗前准备”。
     *
     * @param playerId 玩家ID
     * @return 准备结果
     */
    @Transactional(rollbackFor = Exception.class)
    public PrepareConsumeInventoryResult prepareBeforeConsume(Long playerId) {
        return doPrepareBeforeConsume(playerId, LocalDateTime.now());
    }

    /**
     * 使用指定时间执行一次“库存消耗前准备”。
     *
     * @param playerId 玩家ID
     * @param operateTime 操作时间
     * @return 准备结果
     */
    @Transactional(rollbackFor = Exception.class)
    public PrepareConsumeInventoryResult prepareBeforeConsume(Long playerId, LocalDateTime operateTime) {
        return doPrepareBeforeConsume(playerId, operateTime);
    }

    /**
     * 执行真正的“库存消耗前准备”。
     *
     * @param playerId 玩家ID
     * @param operateTime 操作时间
     * @return 准备结果
     */
    private PrepareConsumeInventoryResult doPrepareBeforeConsume(Long playerId, LocalDateTime operateTime) {
        validateInput(playerId, operateTime);

        AdvanceGatherTaskResult advanceResult = advanceRunningGatherTaskIfNecessary(playerId, operateTime);
        List<PlayerGatherTask> pendingTaskList = playerGatherTaskRepository.findPendingRewardTaskListByPlayerId(playerId);

        PrepareConsumeInventoryResult result = buildBaseResult(playerId, operateTime, pendingTaskList.size());
        appendAdvanceResult(result, advanceResult);
        flushPendingTaskList(pendingTaskList, result);

        result.setAfterPendingTaskCount(countPendingTask(playerId));
        result.setRewardFlushed(result.getFlushedTaskCount() != null && result.getFlushedTaskCount() > 0);
        result.setInventoryPrepared(Boolean.TRUE);
        return result;
    }

    /**
     * 如有运行中的采集任务，先推进到本次操作时刻。
     *
     * @param playerId 玩家ID
     * @param operateTime 操作时间
     * @return 推进结果；没有运行中采集任务时返回 null
     */
    private AdvanceGatherTaskResult advanceRunningGatherTaskIfNecessary(Long playerId, LocalDateTime operateTime) {
        PlayerActionTask runningTask = playerActionTaskRepository.findRunningByPlayerId(playerId);
        if (runningTask == null) {
            return null;
        }
        if (!ActionTaskTypeEnum.GATHER.equals(runningTask.getTaskType())) {
            return null;
        }

        AdvanceGatherTaskCommand command = new AdvanceGatherTaskCommand();
        command.setTaskId(runningTask.getId());
        command.setAdvanceTime(operateTime);
        return gatherTaskAdvanceService.advanceTo(command);
    }

    /**
     * 批量 flush 当前玩家所有仍有 pending 的采集任务。
     *
     * @param pendingTaskList 待 flush 的采集任务列表
     * @param result 汇总结果
     */
    private void flushPendingTaskList(List<PlayerGatherTask> pendingTaskList, PrepareConsumeInventoryResult result) {
        if (pendingTaskList == null || pendingTaskList.isEmpty()) {
            return;
        }

        Set<Long> handledTaskIdSet = new LinkedHashSet<Long>();
        for (int i = 0; i < pendingTaskList.size(); i++) {
            PlayerGatherTask task = pendingTaskList.get(i);
            if (task == null || task.getId() == null) {
                continue;
            }
            if (!handledTaskIdSet.add(task.getId())) {
                continue;
            }
            flushOneTask(task.getId(), result);
        }
    }

    /**
     * flush 单条采集任务并把结果累计到汇总对象。
     *
     * @param taskId 采集任务ID
     * @param result 汇总结果
     */
    private void flushOneTask(Long taskId, PrepareConsumeInventoryResult result) {
        PendingRewardFlushResult flushResult = gatherTaskPendingRewardFlushService.flushPendingReward(taskId);
        if (flushResult == null) {
            return;
        }

        if (Boolean.TRUE.equals(flushResult.getRewardFlushed())) {
            result.setFlushedTaskCount(result.getFlushedTaskCount() + 1);
        }

        result.setFlushedRoundCount(result.getFlushedRoundCount() + safeLong(flushResult.getFlushedRoundCount()));
        result.setAppliedRewardEntryCount(
                result.getAppliedRewardEntryCount() + safeInt(flushResult.getAppliedRewardEntryCount())
        );
    }

    /**
     * 统计当前仍有 pending 的采集任务数量。
     *
     * @param playerId 玩家ID
     * @return pending 任务数量
     */
    private int countPendingTask(Long playerId) {
        List<PlayerGatherTask> pendingTaskList = playerGatherTaskRepository.findPendingRewardTaskListByPlayerId(playerId);
        return pendingTaskList == null ? 0 : pendingTaskList.size();
    }

    /**
     * 把 advance 结果汇总到准备结果中。
     *
     * @param result 准备结果
     * @param advanceResult 推进结果
     */
    private void appendAdvanceResult(PrepareConsumeInventoryResult result, AdvanceGatherTaskResult advanceResult) {
        if (advanceResult == null) {
            return;
        }
        if (safeLong(advanceResult.getAdvancedRoundCount()) > 0L) {
            result.setAdvancedTaskCount(1);
        }
        result.setAdvancedRoundCount(safeLong(advanceResult.getAdvancedRoundCount()));
    }

    /**
     * 构建基础返回对象。
     *
     * @param playerId 玩家ID
     * @param operateTime 操作时间
     * @param beforePendingTaskCount flush 前 pending 任务数
     * @return 准备结果
     */
    private PrepareConsumeInventoryResult buildBaseResult(Long playerId, LocalDateTime operateTime, int beforePendingTaskCount) {
        PrepareConsumeInventoryResult result = new PrepareConsumeInventoryResult();
        result.setPlayerId(playerId);
        result.setOperateTime(operateTime);
        result.setBeforePendingTaskCount(beforePendingTaskCount);
        result.setAfterPendingTaskCount(beforePendingTaskCount);
        result.setAdvancedTaskCount(0);
        result.setAdvancedRoundCount(0L);
        result.setFlushedTaskCount(0);
        result.setFlushedRoundCount(0L);
        result.setAppliedRewardEntryCount(0);
        result.setRewardFlushed(Boolean.FALSE);
        result.setInventoryPrepared(Boolean.FALSE);
        return result;
    }

    /**
     * 安全读取 Long 值。
     *
     * @param value 原值
     * @return 非空 long 值
     */
    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 安全读取 Integer 值。
     *
     * @param value 原值
     * @return 非空 int 值
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 校验输入参数。
     *
     * @param playerId 玩家ID
     * @param operateTime 操作时间
     */
    private void validateInput(Long playerId, LocalDateTime operateTime) {
        if (playerId == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
        if (operateTime == null) {
            throw new IllegalArgumentException("operateTime不能为空");
        }
    }
}