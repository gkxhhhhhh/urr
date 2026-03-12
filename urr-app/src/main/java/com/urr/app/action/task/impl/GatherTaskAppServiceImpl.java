package com.urr.app.action.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.urr.app.action.task.GatherTaskAdvanceService;
import com.urr.app.action.task.GatherTaskAppService;
import com.urr.app.action.task.GatherTaskFactory;
import com.urr.app.action.task.GatherTaskStopService;
import com.urr.app.action.task.PlayerActionQueueRepository;
import com.urr.app.action.task.PlayerActionTaskRepository;
import com.urr.app.action.task.PlayerGatherTaskRedisConverter;
import com.urr.app.action.task.PlayerGatherTaskRedisRepository;
import com.urr.app.action.task.PlayerGatherTaskRepository;
import com.urr.app.action.task.command.AdvanceGatherTaskCommand;
import com.urr.app.action.task.command.EnqueueGatherTaskCommand;
import com.urr.app.action.task.command.StartGatherTaskCommand;
import com.urr.app.action.task.command.StopGatherTaskCommand;
import com.urr.app.action.task.result.StartGatherTaskResult;
import com.urr.app.action.task.result.StopGatherTaskResult;
import com.urr.domain.action.ActionDefEntity;
import com.urr.domain.action.task.ActionTaskConstants;
import com.urr.domain.action.task.ActionTaskStatusEnum;
import com.urr.domain.action.task.ActionTaskStopReasonEnum;
import com.urr.domain.action.task.ActionTaskTypeEnum;
import com.urr.domain.action.task.PlayerActionQueueEntity;
import com.urr.domain.action.task.PlayerActionTask;
import com.urr.domain.action.task.PlayerGatherTask;
import com.urr.domain.player.PlayerEntity;
import com.urr.infra.mapper.ActionDefMapper;
import com.urr.infra.mapper.PlayerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 采集任务应用服务实现。
 *
 * 说明：
 * 1. 本类只做“开始采集 / 加入队列 / 停止当前采集任务”的最小编排。
 * 2. 启动和入队逻辑继续沿用会话 4/5/6/7 已经落地的能力。
 * 3. 停止当前任务时，直接复用现有 GatherTaskStopService，不重写 stop/flush 闭环。
 * 4. 当前还没有做队列自动消费。
 */
@Service
@RequiredArgsConstructor
public class GatherTaskAppServiceImpl implements GatherTaskAppService {

    /**
     * 玩家 Mapper。
     */
    private final PlayerMapper playerMapper;

    /**
     * 动作定义 Mapper。
     */
    private final ActionDefMapper actionDefMapper;

    /**
     * 通用动作任务根表仓储。
     */
    private final PlayerActionTaskRepository playerActionTaskRepository;

    /**
     * 采集任务 DB 真相层仓储。
     */
    private final PlayerGatherTaskRepository playerGatherTaskRepository;

    /**
     * 采集任务 Redis 热态仓储。
     */
    private final PlayerGatherTaskRedisRepository playerGatherTaskRedisRepository;

    /**
     * 动作队列仓储。
     */
    private final PlayerActionQueueRepository playerActionQueueRepository;

    /**
     * 采集任务工厂。
     */
    private final GatherTaskFactory gatherTaskFactory;

    /**
     * 采集任务最小懒推进服务。
     */
    private final GatherTaskAdvanceService gatherTaskAdvanceService;

    /**
     * 采集任务停止服务。
     */
    private final GatherTaskStopService gatherTaskStopService;

    /**
     * 立即开始采集。
     *
     * @param command 开始采集命令
     * @return 启动结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StartGatherTaskResult startNow(StartGatherTaskCommand command) {
        validateStartCommand(command);

        PlayerEntity player = requireOwnedPlayer(command.getAccountId(), command.getPlayerId());
        ActionDefEntity action = requireGatherAction(command.getActionCode());
        validateBasicStartCondition(player, action);

        PlayerActionTask runningTask = playerActionTaskRepository.findRunningByPlayerId(player.getId());
        if (runningTask != null) {
            return replaceCurrentTaskAndStart(player, action, command.getTargetCount(), runningTask);
        }

        return startNewRunningTask(player, action, command.getTargetCount(), null);
    }

    /**
     * 加入采集队列。
     *
     * @param command 入队命令
     * @return 启动或入队结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StartGatherTaskResult enqueue(EnqueueGatherTaskCommand command) {
        validateEnqueueCommand(command);

        PlayerEntity player = requireOwnedPlayer(command.getAccountId(), command.getPlayerId());
        ActionDefEntity action = requireGatherAction(command.getActionCode());
        validateBasicStartCondition(player, action);

        PlayerActionTask runningTask = playerActionTaskRepository.findRunningByPlayerId(player.getId());
        if (runningTask == null) {
            return startNewRunningTask(player, action, command.getTargetCount(), null);
        }

        ensureQueueNotFull(player.getId());

        PlayerActionQueueEntity queueEntity = buildQueueEntity(player, action, command.getTargetCount());
        playerActionQueueRepository.insert(queueEntity);

        StartGatherTaskResult result = new StartGatherTaskResult();
        result.setTaskId(null);
        result.setQueueId(queueEntity.getId());
        result.setPlayerId(player.getId());
        result.setActionCode(action.getActionCode());
        result.setStatus(ActionTaskStatusEnum.QUEUED.getCode());
        result.setQueued(Boolean.TRUE);
        result.setQueuePosition(playerActionQueueRepository.calculateQueuePosition(player.getId(), queueEntity.getId()));
        result.setReplacedTaskId(null);
        return result;
    }

    /**
     * 停止当前运行中的采集任务。
     *
     * @param command 停止命令
     * @return 停止结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StopGatherTaskResult stopCurrent(StopGatherTaskCommand command) {
        validateStopCommand(command);

        PlayerEntity player = requireOwnedPlayer(command.getAccountId(), command.getPlayerId());
        PlayerActionTask runningTask = playerActionTaskRepository.findRunningByPlayerId(player.getId());
        if (runningTask == null) {
            throw new IllegalArgumentException("当前无运行中的采集任务可停止");
        }
        if (!ActionTaskTypeEnum.GATHER.equals(runningTask.getTaskType())) {
            throw new IllegalArgumentException("当前运行中的任务不是采集任务，无法停止");
        }

        return gatherTaskStopService.stopByTaskId(
                runningTask.getId(),
                ActionTaskStopReasonEnum.USER_STOP,
                LocalDateTime.now()
        );
    }

    /**
     * 替换当前任务并启动新采集任务。
     *
     * @param player 玩家
     * @param action 动作定义
     * @param targetCount 目标轮次
     * @param runningTask 当前运行中的旧任务
     * @return 启动结果
     */
    private StartGatherTaskResult replaceCurrentTaskAndStart(PlayerEntity player,
                                                             ActionDefEntity action,
                                                             Long targetCount,
                                                             PlayerActionTask runningTask) {
        Long replacedTaskId = runningTask.getId();

        if (ActionTaskTypeEnum.GATHER.equals(runningTask.getTaskType())) {
            advanceGatherTaskBeforeReplace(replacedTaskId);
        }

        playerActionTaskRepository.stopByUserReplace(replacedTaskId);

        StartGatherTaskResult result = startNewRunningTask(player, action, targetCount, replacedTaskId);

        if (ActionTaskTypeEnum.GATHER.equals(runningTask.getTaskType())) {
            refreshStoppedGatherHotState(player.getId(), replacedTaskId);
        }

        return result;
    }

    /**
     * 启动一条新的运行中采集任务。
     *
     * @param player 玩家
     * @param action 动作定义
     * @param targetCount 目标轮次
     * @param replacedTaskId 被替换的旧任务ID
     * @return 启动结果
     */
    private StartGatherTaskResult startNewRunningTask(PlayerEntity player,
                                                      ActionDefEntity action,
                                                      Long targetCount,
                                                      Long replacedTaskId) {
        LocalDateTime now = LocalDateTime.now();

        refreshPlayerLastInteractTime(player, now);

        PlayerGatherTask task = gatherTaskFactory.createRunningTask(player, action, targetCount, now);
        playerGatherTaskRepository.insert(task);
        playerGatherTaskRedisRepository.saveTaskHotState(task);

        StartGatherTaskResult result = new StartGatherTaskResult();
        result.setTaskId(task.getId());
        result.setQueueId(null);
        result.setPlayerId(task.getPlayerId());
        result.setActionCode(task.getActionCode());
        result.setStatus(task.getStatus().getCode());
        result.setQueued(Boolean.FALSE);
        result.setQueuePosition(null);
        result.setReplacedTaskId(replacedTaskId);
        return result;
    }

    /**
     * 在替换旧采集任务前，先把它推进到当前时刻。
     *
     * @param taskId 旧采集任务ID
     */
    private void advanceGatherTaskBeforeReplace(Long taskId) {
        AdvanceGatherTaskCommand advanceCommand = new AdvanceGatherTaskCommand();
        advanceCommand.setTaskId(taskId);
        advanceCommand.setAdvanceTime(LocalDateTime.now());
        gatherTaskAdvanceService.advanceTo(advanceCommand);
    }

    /**
     * 刷新被替换采集任务的 Redis 热态。
     *
     * 说明：
     * 1. 运行槽位 / 运行态 / 当前段计划可以删除。
     * 2. 如果仍有 pending_reward_pool，则继续保留热态镜像，方便后续 flush 前读取。
     *
     * @param playerId 玩家ID
     * @param taskId 任务ID
     */
    private void refreshStoppedGatherHotState(Long playerId, Long taskId) {
        playerGatherTaskRedisRepository.deleteRunningTaskIfMatch(playerId, taskId);
        playerGatherTaskRedisRepository.deleteRuntime(taskId);
        playerGatherTaskRedisRepository.deleteSegmentPlan(taskId);

        PlayerGatherTask stoppedTask = playerGatherTaskRepository.findByTaskId(taskId);
        if (stoppedTask == null || !stoppedTask.hasPendingRewardPool()) {
            playerGatherTaskRedisRepository.deletePendingRewardPool(taskId);
            return;
        }

        playerGatherTaskRedisRepository.savePendingRewardPool(
                PlayerGatherTaskRedisConverter.toPendingRewardPoolCache(stoppedTask)
        );
    }

    /**
     * 校验开始命令。
     *
     * @param command 开始命令
     */
    private void validateStartCommand(StartGatherTaskCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("开始采集命令不能为空");
        }
        if (command.getAccountId() == null) {
            throw new IllegalArgumentException("未登录");
        }
        if (command.getPlayerId() == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
        if (!StringUtils.hasText(command.getActionCode())) {
            throw new IllegalArgumentException("actionCode不能为空");
        }

        gatherTaskFactory.normalizeTargetCount(command.getTargetCount());
        command.setActionCode(command.getActionCode().trim());
    }

    /**
     * 校验入队命令。
     *
     * @param command 入队命令
     */
    private void validateEnqueueCommand(EnqueueGatherTaskCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("入队命令不能为空");
        }
        if (command.getAccountId() == null) {
            throw new IllegalArgumentException("未登录");
        }
        if (command.getPlayerId() == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
        if (!StringUtils.hasText(command.getActionCode())) {
            throw new IllegalArgumentException("actionCode不能为空");
        }

        gatherTaskFactory.normalizeTargetCount(command.getTargetCount());
        command.setActionCode(command.getActionCode().trim());
    }

    /**
     * 校验停止命令。
     *
     * @param command 停止命令
     */
    private void validateStopCommand(StopGatherTaskCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("停止命令不能为空");
        }
        if (command.getAccountId() == null) {
            throw new IllegalArgumentException("未登录");
        }
        if (command.getPlayerId() == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
    }

    /**
     * 校验玩家最基本的开始条件。
     *
     * @param player 玩家
     * @param action 动作定义
     */
    private void validateBasicStartCondition(PlayerEntity player, ActionDefEntity action) {
        int playerLevel = player.getLevel() == null ? 1 : player.getLevel().intValue();
        int minPlayerLevel = action.getMinPlayerLevel() == null ? 1 : action.getMinPlayerLevel().intValue();
        if (playerLevel < minPlayerLevel) {
            throw new IllegalArgumentException("角色等级不足，无法开始该采集动作");
        }

        int playerEnergy = player.getEnergy() == null ? 0 : player.getEnergy().intValue();
        int baseEnergyCost = action.getBaseEnergyCost() == null ? 0 : action.getBaseEnergyCost().intValue();
        if (playerEnergy < baseEnergyCost) {
            throw new IllegalArgumentException("体力不足，无法开始该采集动作");
        }
    }

    /**
     * 查询并校验玩家归属。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @return 玩家
     */
    private PlayerEntity requireOwnedPlayer(Long accountId, Long playerId) {
        PlayerEntity player = playerMapper.selectById(playerId);
        if (player == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        if (!Objects.equals(accountId, player.getAccountId())) {
            throw new IllegalArgumentException("无权限操作该角色");
        }
        return player;
    }

    /**
     * 查询并校验当前动作是否为“可用于采集”的动作。
     *
     * @param actionCode 动作编码
     * @return 动作定义
     */
    private ActionDefEntity requireGatherAction(String actionCode) {
        ActionDefEntity action = actionDefMapper.selectOne(
                new LambdaQueryWrapper<ActionDefEntity>()
                        .eq(ActionDefEntity::getActionCode, actionCode)
                        .eq(ActionDefEntity::getStatus, 1)
                        .last("limit 1")
        );

        if (action == null) {
            throw new IllegalArgumentException("动作不存在或未启用");
        }
        if (!StringUtils.hasText(action.getActionKind()) || !"LOOP".equalsIgnoreCase(action.getActionKind())) {
            throw new IllegalArgumentException("当前动作不是可持续采集动作");
        }
        if (!StringUtils.hasText(action.getActionCode()) || !action.getActionCode().startsWith("GATHER_")) {
            throw new IllegalArgumentException("当前动作不是采集动作");
        }

        return action;
    }

    /**
     * 校验当前角色队列是否已经达到上限。
     *
     * @param playerId 玩家ID
     */
    private void ensureQueueNotFull(Long playerId) {
        long queuedCount = playerActionQueueRepository.countQueuedByPlayerId(playerId);
        if (queuedCount >= ActionTaskConstants.MAX_QUEUE_SIZE_PER_PLAYER) {
            throw new IllegalArgumentException("当前角色排队数量已达到上限");
        }
    }

    /**
     * 刷新玩家最近一次交互时间。
     *
     * @param player 玩家
     * @param operateTime 操作时间
     */
    private void refreshPlayerLastInteractTime(PlayerEntity player, LocalDateTime operateTime) {
        player.setLastInteractTime(operateTime);
        int rows = playerMapper.updateById(player);
        if (rows != 1) {
            throw new IllegalStateException("更新玩家最近交互时间失败，playerId=" + player.getId());
        }
    }

    /**
     * 组装队列实体。
     *
     * @param player 玩家
     * @param action 动作定义
     * @param targetCount 目标轮次
     * @return 队列实体
     */
    private PlayerActionQueueEntity buildQueueEntity(PlayerEntity player, ActionDefEntity action, Long targetCount) {
        PlayerActionQueueEntity entity = new PlayerActionQueueEntity();
        entity.setPlayerId(player.getId());
        entity.setServerId(player.getServerId());
        entity.setTaskType(ActionTaskTypeEnum.GATHER.getCode());
        entity.setActionCode(action.getActionCode());
        entity.setTargetCount(gatherTaskFactory.normalizeTargetCount(targetCount));
        entity.setStatus(ActionTaskStatusEnum.QUEUED.getCode());
        return entity;
    }
}