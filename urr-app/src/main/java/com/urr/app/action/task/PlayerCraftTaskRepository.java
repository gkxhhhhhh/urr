package com.urr.app.action.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.urr.domain.action.task.ActionTaskStatusEnum;
import com.urr.domain.action.task.CraftTaskTimeSupport;
import com.urr.domain.action.task.PlayerCraftTask;
import com.urr.domain.action.task.PlayerCraftTaskEntity;
import com.urr.domain.craft.RecipeDefEntity;
import com.urr.infra.mapper.PlayerCraftTaskMapper;
import com.urr.infra.mapper.RecipeDefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 玩家制造任务仓储。
 */
@Repository
@RequiredArgsConstructor
public class PlayerCraftTaskRepository {

    /**
     * 制造任务明细 Mapper。
     */
    private final PlayerCraftTaskMapper playerCraftTaskMapper;

    /**
     * 配方定义 Mapper。
     */
    private final RecipeDefMapper recipeDefMapper;

    /**
     * 按根任务ID查询制造任务。
     *
     * @param taskId 根任务ID
     * @return 制造任务
     */
    public PlayerCraftTask findByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }
        PlayerCraftTaskEntity entity = playerCraftTaskMapper.selectOne(
                new LambdaQueryWrapper<PlayerCraftTaskEntity>()
                        .eq(PlayerCraftTaskEntity::getTaskId, taskId)
                        .last("limit 1")
        );
        return toDomain(entity);
    }

    /**
     * 插入制造任务。
     *
     * @param task 制造任务
     */
    public void insert(PlayerCraftTask task) {
        if (task == null) {
            throw new IllegalArgumentException("制造任务不能为空");
        }
        PlayerCraftTaskEntity entity = toEntity(task);
        int rows = playerCraftTaskMapper.insert(entity);
        if (rows != 1) {
            throw new IllegalStateException("保存制造任务失败");
        }
    }

    /**
     * 按 taskId 更新制造任务。
     *
     * @param task 制造任务
     */
    public void updateByTaskId(PlayerCraftTask task) {
        if (task == null || task.getId() == null) {
            throw new IllegalArgumentException("制造任务ID不能为空");
        }
        PlayerCraftTaskEntity entity = playerCraftTaskMapper.selectOne(
                new LambdaQueryWrapper<PlayerCraftTaskEntity>()
                        .eq(PlayerCraftTaskEntity::getTaskId, task.getId())
                        .last("limit 1")
        );
        if (entity == null) {
            throw new IllegalStateException("制造任务不存在，taskId=" + task.getId());
        }
        entity.setPlayerId(task.getPlayerId());
        entity.setServerId(task.getServerId());
        entity.setActionCode(task.getActionCode());
        entity.setTargetCount(task.isInfiniteTarget() ? 0L : task.getTargetCount());
        entity.setCompletedCount(task.getCompletedCount());
        entity.setRecipeSnapshotJson(task.getRecipeSnapshotJson());
        entity.setNextRoundFinishTime(task.getNextRoundFinishTime());
        entity.setStatus(task.getStatus() == null ? ActionTaskStatusEnum.RUNNING.getCode() : task.getStatus().getCode());
        entity.setUpdateTime(System.currentTimeMillis());
        int rows = playerCraftTaskMapper.updateById(entity);
        if (rows != 1) {
            throw new IllegalStateException("更新制造任务失败，taskId=" + task.getId());
        }
    }

    /**
     * 按 taskId 修改明细状态。
     *
     * @param taskId 任务ID
     * @param status 状态
     */
    public void updateStatusByTaskId(Long taskId, ActionTaskStatusEnum status) {
        if (taskId == null || status == null) {
            return;
        }
        PlayerCraftTaskEntity entity = playerCraftTaskMapper.selectOne(
                new LambdaQueryWrapper<PlayerCraftTaskEntity>()
                        .eq(PlayerCraftTaskEntity::getTaskId, taskId)
                        .last("limit 1")
        );
        if (entity == null) {
            return;
        }
        entity.setStatus(status.getCode());
        entity.setUpdateTime(System.currentTimeMillis());
        int rows = playerCraftTaskMapper.updateById(entity);
        if (rows != 1) {
            throw new IllegalStateException("更新制造任务状态失败，taskId=" + taskId);
        }
    }

    /**
     * 按配方编码查询配方。
     *
     * @param recipeCode 配方编码
     * @return 配方
     */
    public RecipeDefEntity findRecipeByCode(String recipeCode) {
        if (recipeCode == null || recipeCode.trim().isEmpty()) {
            return null;
        }
        return recipeDefMapper.selectOne(
                new LambdaQueryWrapper<RecipeDefEntity>()
                        .eq(RecipeDefEntity::getRecipeCode, recipeCode.trim())
                        .eq(RecipeDefEntity::getDeleteFlag, 0)
                        .last("limit 1")
        );
    }

    /**
     * 把实体转换为领域对象。
     *
     * @param entity 实体
     * @return 领域对象
     */
    private PlayerCraftTask toDomain(PlayerCraftTaskEntity entity) {
        if (entity == null) {
            return null;
        }
        PlayerCraftTask task = new PlayerCraftTask();
        task.setId(entity.getTaskId());
        task.setPlayerId(entity.getPlayerId());
        task.setServerId(entity.getServerId());
        task.setActionCode(entity.getActionCode());
        task.setTargetCount(entity.getTargetCount());
        task.setCompletedCount(entity.getCompletedCount());
        task.setRecipeSnapshotJson(entity.getRecipeSnapshotJson());
        task.setNextRoundFinishTime(entity.getNextRoundFinishTime());
        task.setStatus(ActionTaskStatusEnum.fromCode(entity.getStatus()));
        task.setLastSettleTime(CraftTaskTimeSupport.fromEpochMilli(entity.getUpdateTime()));
        return task;
    }

    /**
     * 把领域对象转换为实体。
     *
     * @param task 领域对象
     * @return 实体
     */
    private PlayerCraftTaskEntity toEntity(PlayerCraftTask task) {
        PlayerCraftTaskEntity entity = new PlayerCraftTaskEntity();
        entity.setTaskId(task.getId());
        entity.setPlayerId(task.getPlayerId());
        entity.setServerId(task.getServerId());
        entity.setActionCode(task.getActionCode());
        entity.setTargetCount(task.isInfiniteTarget() ? 0L : task.getTargetCount());
        entity.setCompletedCount(task.getCompletedCount() == null ? 0L : task.getCompletedCount());
        entity.setRecipeSnapshotJson(task.getRecipeSnapshotJson());
        entity.setNextRoundFinishTime(task.getNextRoundFinishTime());
        entity.setStatus(task.getStatus() == null ? ActionTaskStatusEnum.RUNNING.getCode() : task.getStatus().getCode());
        entity.setCreateTime(System.currentTimeMillis());
        entity.setUpdateTime(System.currentTimeMillis());
        return entity;
    }
}