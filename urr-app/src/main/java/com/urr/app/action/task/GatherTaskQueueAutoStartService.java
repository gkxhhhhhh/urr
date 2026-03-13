package com.urr.app.action.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.urr.domain.action.ActionDefEntity;
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

/**
 * 采集任务队列自动拉起服务。
 *
 * 说明：
 * 1. 这里只负责“当前没有运行中任务时，从队列拉起下一条采集任务”。
 * 2. 不负责 stop / flush / advance 逻辑。
 * 3. 不负责前端展示逻辑。
 * 4. 同一玩家的自动拉起过程，使用玩家行锁做最小串行化保护。
 */
@Service
@RequiredArgsConstructor
public class GatherTaskQueueAutoStartService {

    /**
     * 玩家 Mapper。
     */
    private final PlayerMapper playerMapper;

    /**
     * 动作定义 Mapper。
     */
    private final ActionDefMapper actionDefMapper;

    /**
     * 动作根任务仓储。
     */
    private final PlayerActionTaskRepository playerActionTaskRepository;

    /**
     * 动作队列仓储。
     */
    private final PlayerActionQueueRepository playerActionQueueRepository;

    /**
     * 采集任务仓储。
     */
    private final PlayerGatherTaskRepository playerGatherTaskRepository;

    /**
     * 采集任务 Redis 热态仓储。
     */
    private final PlayerGatherTaskRedisRepository playerGatherTaskRedisRepository;

    /**
     * 采集任务工厂。
     */
    private final GatherTaskFactory gatherTaskFactory;

    /**
     * 尝试为指定玩家自动拉起队列中的下一条采集任务。
     *
     * 说明：
     * 1. 如果当前已经存在运行中任务，则直接返回，不重复拉起。
     * 2. 如果队列为空，则直接返回 null。
     * 3. 如果成功消费队头，则创建新的运行中采集任务并写入 DB + Redis。
     *
     * @param playerId 玩家ID
     * @return 新拉起的采集任务；没有拉起时返回 null
     */
    @Transactional(rollbackFor = Exception.class)
    public PlayerGatherTask tryStartNextQueuedTask(Long playerId) {
        if (playerId == null) {
            return null;
        }

        PlayerEntity player = lockPlayer(playerId);
        if (player == null) {
            return null;
        }

        PlayerActionTask runningTask = playerActionTaskRepository.findRunningByPlayerId(playerId);
        if (runningTask != null) {
            return null;
        }

        PlayerActionQueueEntity queueEntity =
                playerActionQueueRepository.findFirstQueuedByPlayerIdAndTaskType(playerId, ActionTaskTypeEnum.GATHER);
        if (queueEntity == null) {
            return null;
        }

        int deleteRows = playerActionQueueRepository.deleteQueuedById(playerId, queueEntity.getId());
        if (deleteRows != 1) {
            return null;
        }

        ActionDefEntity action = requireGatherAction(queueEntity.getActionCode());
        validateBasicStartCondition(player, action);

        LocalDateTime now = LocalDateTime.now();
        refreshPlayerLastInteractTime(player, now);

        PlayerGatherTask newTask =
                gatherTaskFactory.createRunningTask(player, action, queueEntity.getTargetCount(), now);
        playerGatherTaskRepository.insert(newTask);
        playerGatherTaskRedisRepository.saveTaskHotState(newTask);
        return newTask;
    }

    /**
     * 锁定玩家行。
     *
     * 说明：
     * 1. 这里只做同一玩家维度的最小串行化保护。
     * 2. 不引入额外分布式锁。
     *
     * @param playerId 玩家ID
     * @return 玩家实体
     */
    private PlayerEntity lockPlayer(Long playerId) {
        return playerMapper.selectOne(
                new LambdaQueryWrapper<PlayerEntity>()
                        .eq(PlayerEntity::getId, playerId)
                        .last("limit 1 for update")
        );
    }

    /**
     * 查询并校验当前动作是否为可持续采集动作。
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
            throw new IllegalArgumentException("动作不存在或未启用，actionCode=" + actionCode);
        }
        if (!StringUtils.hasText(action.getActionKind())
                || !"LOOP".equalsIgnoreCase(action.getActionKind())) {
            throw new IllegalArgumentException("当前动作不是可持续采集动作，actionCode=" + actionCode);
        }
        if (!StringUtils.hasText(action.getActionCode())
                || !action.getActionCode().startsWith("GATHER_")) {
            throw new IllegalArgumentException("当前动作不是采集动作，actionCode=" + actionCode);
        }
        return action;
    }

    /**
     * 校验玩家最基本的启动条件。
     *
     * @param player 玩家
     * @param action 动作定义
     */
    private void validateBasicStartCondition(PlayerEntity player, ActionDefEntity action) {
        int playerLevel = player.getLevel() == null ? 1 : player.getLevel().intValue();
        int minPlayerLevel = action.getMinPlayerLevel() == null ? 1 : action.getMinPlayerLevel().intValue();
        if (playerLevel < minPlayerLevel) {
            throw new IllegalArgumentException("角色等级不足，无法自动拉起队列中的采集任务");
        }

        int playerEnergy = player.getEnergy() == null ? 0 : player.getEnergy().intValue();
        int baseEnergyCost = action.getBaseEnergyCost() == null ? 0 : action.getBaseEnergyCost().intValue();
        if (playerEnergy < baseEnergyCost) {
            throw new IllegalArgumentException("体力不足，无法自动拉起队列中的采集任务");
        }
    }

    /**
     * 刷新玩家最近交互时间。
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
}