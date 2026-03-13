package com.urr.app.action.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.urr.domain.action.task.ActionTaskStatusEnum;
import com.urr.domain.action.task.ActionTaskTypeEnum;
import com.urr.domain.action.task.PlayerActionQueueEntity;
import com.urr.infra.mapper.PlayerActionQueueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家动作队列仓储。
 *
 * 说明：
 * 1. 这里只处理最小队列真相层读写。
 * 2. 队列顺序继续使用自增 id 作为最小顺序能力。
 * 3. 消费成功后的队列记录，直接删除即可，不额外发明新的“已消费状态”。
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
     * 查询角色当前排队中的指定任务类型列表。
     *
     * @param playerId 玩家ID
     * @param taskType 任务类型
     * @return 队列列表
     */
    public List<PlayerActionQueueEntity> findQueuedByPlayerIdAndTaskType(Long playerId, ActionTaskTypeEnum taskType) {
        List<PlayerActionQueueEntity> result = new ArrayList<>();
        if (playerId == null || taskType == null) {
            return result;
        }
        List<PlayerActionQueueEntity> entityList = playerActionQueueMapper.selectList(
                new LambdaQueryWrapper<PlayerActionQueueEntity>()
                        .eq(PlayerActionQueueEntity::getPlayerId, playerId)
                        .eq(PlayerActionQueueEntity::getTaskType, taskType.getCode())
                        .eq(PlayerActionQueueEntity::getStatus, ActionTaskStatusEnum.QUEUED.getCode())
                        .orderByAsc(PlayerActionQueueEntity::getId)
        );
        if (entityList == null) {
            return result;
        }
        return entityList;
    }

    /**
     * 查询角色当前排队中的第一条指定任务类型记录。
     *
     * @param playerId 玩家ID
     * @param taskType 任务类型
     * @return 队头记录，不存在时返回 null
     */
    public PlayerActionQueueEntity findFirstQueuedByPlayerIdAndTaskType(Long playerId, ActionTaskTypeEnum taskType) {
        if (playerId == null || taskType == null) {
            return null;
        }
        return playerActionQueueMapper.selectOne(
                new LambdaQueryWrapper<PlayerActionQueueEntity>()
                        .eq(PlayerActionQueueEntity::getPlayerId, playerId)
                        .eq(PlayerActionQueueEntity::getTaskType, taskType.getCode())
                        .eq(PlayerActionQueueEntity::getStatus, ActionTaskStatusEnum.QUEUED.getCode())
                        .orderByAsc(PlayerActionQueueEntity::getId)
                        .last("limit 1")
        );
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
     * 删除一条仍处于 QUEUED 的队列记录。
     *
     * @param playerId 玩家ID
     * @param queueId 队列ID
     * @return 删除行数
     */
    public int deleteQueuedById(Long playerId, Long queueId) {
        if (playerId == null || queueId == null) {
            return 0;
        }
        return playerActionQueueMapper.delete(
                new LambdaQueryWrapper<PlayerActionQueueEntity>()
                        .eq(PlayerActionQueueEntity::getId, queueId)
                        .eq(PlayerActionQueueEntity::getPlayerId, playerId)
                        .eq(PlayerActionQueueEntity::getStatus, ActionTaskStatusEnum.QUEUED.getCode())
        );
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