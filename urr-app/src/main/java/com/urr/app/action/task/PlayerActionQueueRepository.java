package com.urr.app.action.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.urr.domain.action.task.ActionTaskStatusEnum;
import com.urr.domain.action.task.PlayerActionQueueEntity;
import com.urr.infra.mapper.PlayerActionQueueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 玩家动作队列仓储。
 *
 * 说明：
 * 1. 当前只做最小入队能力。
 * 2. 不负责自动消费队列。
 * 3. 队列顺序使用自增 id 作为最小顺序能力。
 */
@Repository
@RequiredArgsConstructor
public class PlayerActionQueueRepository {

    /**
     * 队列 Mapper。
     */
    private final PlayerActionQueueMapper playerActionQueueMapper;

    /**
     * 统计角色当前排队中的记录数。
     *
     * @param playerId 玩家ID
     * @return 排队数量
     */
    public long countQueuedByPlayerId(Long playerId) {
        if (playerId == null) {
            return 0L;
        }

        Long count = playerActionQueueMapper.selectCount(
                new LambdaQueryWrapper<PlayerActionQueueEntity>()
                        .eq(PlayerActionQueueEntity::getPlayerId, playerId)
                        .eq(PlayerActionQueueEntity::getStatus, ActionTaskStatusEnum.QUEUED.getCode())
        );
        return count == null ? 0L : count.longValue();
    }

    /**
     * 保存一条入队记录。
     *
     * @param entity 队列实体
     */
    public void insert(PlayerActionQueueEntity entity) {
        validateEntity(entity);
        int rows = playerActionQueueMapper.insert(entity);
        if (rows != 1) {
            throw new IllegalStateException("保存动作队列失败");
        }
    }

    /**
     * 计算一条队列记录当前的排队位置。
     *
     * @param playerId 玩家ID
     * @param queueId 队列ID
     * @return 排队位置，从 1 开始
     */
    public int calculateQueuePosition(Long playerId, Long queueId) {
        if (playerId == null || queueId == null) {
            return 0;
        }

        Long count = playerActionQueueMapper.selectCount(
                new LambdaQueryWrapper<PlayerActionQueueEntity>()
                        .eq(PlayerActionQueueEntity::getPlayerId, playerId)
                        .eq(PlayerActionQueueEntity::getStatus, ActionTaskStatusEnum.QUEUED.getCode())
                        .le(PlayerActionQueueEntity::getId, queueId)
        );
        if (count == null) {
            return 0;
        }
        return count.intValue();
    }

    /**
     * 校验队列实体。
     *
     * @param entity 队列实体
     */
    private void validateEntity(PlayerActionQueueEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("队列实体不能为空");
        }
        if (entity.getPlayerId() == null) {
            throw new IllegalArgumentException("队列实体 playerId 不能为空");
        }
        if (entity.getServerId() == null) {
            throw new IllegalArgumentException("队列实体 serverId 不能为空");
        }
        if (entity.getTaskType() == null || entity.getTaskType().trim().isEmpty()) {
            throw new IllegalArgumentException("队列实体 taskType 不能为空");
        }
        if (entity.getActionCode() == null || entity.getActionCode().trim().isEmpty()) {
            throw new IllegalArgumentException("队列实体 actionCode 不能为空");
        }
        if (entity.getStatus() == null || entity.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("队列实体 status 不能为空");
        }
    }
}