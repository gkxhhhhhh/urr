package com.urr.app.action.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.urr.domain.action.task.ActionTaskStatusEnum;
import com.urr.domain.action.task.ActionTaskStopReasonEnum;
import com.urr.domain.action.task.ActionTaskTypeEnum;
import com.urr.domain.action.task.PlayerActionTask;
import com.urr.domain.action.task.PlayerActionTaskEntity;
import com.urr.infra.mapper.PlayerActionTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 通用动作任务根表仓储。
 *
 * 说明：
 * 1. 这里只处理 t_urr_player_activity 根表。
 * 2. 当前主要给“查当前运行中任务 / 替换旧任务”使用。
 * 3. 后续战斗、制造接入时，也可以继续复用这里的运行槽位查询能力。
 */
@Repository
@RequiredArgsConstructor
public class PlayerActionTaskRepository {

    /**
     * 动作任务根表 Mapper。
     */
    private final PlayerActionTaskMapper playerActionTaskMapper;

    /**
     * 按玩家ID查询当前运行中的任务。
     *
     * @param playerId 玩家ID
     * @return 当前运行中的任务，不存在时返回 null
     */
    public PlayerActionTask findRunningByPlayerId(Long playerId) {
        if (playerId == null) {
            return null;
        }

        PlayerActionTaskEntity entity = playerActionTaskMapper.selectOne(
                new LambdaQueryWrapper<PlayerActionTaskEntity>()
                        .eq(PlayerActionTaskEntity::getPlayerId, playerId)
                        .eq(PlayerActionTaskEntity::getStatus, ActionTaskStatusEnum.RUNNING.getCode())
                        .orderByDesc(PlayerActionTaskEntity::getId)
                        .last("limit 1")
        );
        return toDomain(entity);
    }

    /**
     * 按任务ID查询根任务。
     *
     * @param taskId 任务ID
     * @return 根任务，不存在时返回 null
     */
    public PlayerActionTask findById(Long taskId) {
        if (taskId == null) {
            return null;
        }
        return toDomain(playerActionTaskMapper.selectById(taskId));
    }

    /**
     * 把一条运行中任务标记为“玩家替换停止”。
     *
     * @param taskId 任务ID
     */
    public void stopByUserReplace(Long taskId) {
        if (taskId == null) {
            return;
        }

        PlayerActionTaskEntity entity = playerActionTaskMapper.selectById(taskId);
        if (entity == null) {
            return;
        }

        entity.setStatus(ActionTaskStatusEnum.STOPPED.getCode());
        entity.setStopReason(ActionTaskStopReasonEnum.USER_REPLACE.getCode());
        entity.setState(3);

        int rows = playerActionTaskMapper.updateById(entity);
        if (rows != 1) {
            throw new IllegalStateException("替换旧任务失败，taskId=" + taskId);
        }
    }

    /**
     * 把根表实体转换成领域对象。
     *
     * @param entity 根表实体
     * @return 领域对象
     */
    private PlayerActionTask toDomain(PlayerActionTaskEntity entity) {
        if (entity == null) {
            return null;
        }

        PlayerActionTask task = new PlayerActionTask();
        task.setId(entity.getId());
        task.setPlayerId(entity.getPlayerId());
        task.setServerId(entity.getServerId());
        task.setActionCode(entity.getActionCode());
        task.setTaskType(ActionTaskTypeEnum.fromCode(entity.getTaskType()));
        task.setStartTime(entity.getStartTime());
        task.setLastInteractTime(entity.getLastInteractTime());
        task.setLastSettleTime(entity.getLastSettleTime());
        task.setOfflineExpireAt(entity.getOfflineExpireAt());
        task.setRewardSeed(entity.getRewardSeed());
        task.setStatus(ActionTaskStatusEnum.fromCode(entity.getStatus()));
        task.setStopReason(ActionTaskStopReasonEnum.fromCode(entity.getStopReason()));
        return task;
    }
}