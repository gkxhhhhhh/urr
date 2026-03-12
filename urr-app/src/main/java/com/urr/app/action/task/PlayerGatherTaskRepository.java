package com.urr.app.action.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urr.domain.action.task.ActionTaskConstants;
import com.urr.domain.action.task.ActionTaskStatusEnum;
import com.urr.domain.action.task.ActionTaskStopReasonEnum;
import com.urr.domain.action.task.ActionTaskTypeEnum;
import com.urr.domain.action.task.GatherTaskStatSnapshot;
import com.urr.domain.action.task.PlayerActionTaskEntity;
import com.urr.domain.action.task.PlayerGatherTask;
import com.urr.domain.action.task.PlayerGatherTaskEntity;
import com.urr.infra.mapper.PlayerActionTaskMapper;
import com.urr.infra.mapper.PlayerGatherTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 采集任务持久化仓储。
 *
 * 说明：
 * 1. 这里只处理数据库真相层读写。
 * 2. 不负责启动任务、不负责执行任务、不负责结算任务。
 * 3. 复用 t_urr_player_activity 作为动作任务根表。
 * 4. 采集专属字段落在 t_urr_player_gather_task。
 */
@Repository
@RequiredArgsConstructor
public class PlayerGatherTaskRepository {

    /**
     * 通用动作任务根表 Mapper。
     */
    private final PlayerActionTaskMapper playerActionTaskMapper;

    /**
     * 采集任务扩展表 Mapper。
     */
    private final PlayerGatherTaskMapper playerGatherTaskMapper;

    /**
     * JSON 编解码器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 按玩家ID查询当前采集任务。
     *
     * @param playerId 玩家ID
     * @return 采集任务，不存在时返回 null
     */
    public PlayerGatherTask findCurrentByPlayerId(Long playerId) {
        if (playerId == null) {
            return null;
        }

        PlayerActionTaskEntity taskEntity = playerActionTaskMapper.selectOne(
                new LambdaQueryWrapper<PlayerActionTaskEntity>()
                        .eq(PlayerActionTaskEntity::getPlayerId, playerId)
                        .eq(PlayerActionTaskEntity::getTaskType, ActionTaskTypeEnum.GATHER.getCode())
                        .orderByDesc(PlayerActionTaskEntity::getId)
                        .last("limit 1")
        );
        return buildGatherTask(taskEntity);
    }

    /**
     * 按任务ID查询采集任务。
     *
     * @param taskId 任务ID
     * @return 采集任务，不存在时返回 null
     */
    public PlayerGatherTask findByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }

        PlayerActionTaskEntity taskEntity = playerActionTaskMapper.selectById(taskId);
        return buildGatherTask(taskEntity);
    }

    /**
     * 查询角色当前仍有 pending_reward_pool 的采集任务列表。
     *
     * 说明：
     * 1. 这里只服务读接口的展示库存拼装。
     * 2. 当前先按玩家自己的采集任务列表顺序扫描，保持实现简单。
     * 3. 判定条件以 completedCount > flushedCount 或 pendingRewardPoolJson 非空为准。
     *
     * @param playerId 玩家ID
     * @return 仍有 pending 的采集任务列表
     */
    public java.util.List<PlayerGatherTask> findPendingRewardTaskListByPlayerId(Long playerId) {
        java.util.List<PlayerGatherTask> result = new java.util.ArrayList<PlayerGatherTask>();
        if (playerId == null) {
            return result;
        }

        java.util.List<PlayerActionTaskEntity> taskEntityList = playerActionTaskMapper.selectList(
                new LambdaQueryWrapper<PlayerActionTaskEntity>()
                        .eq(PlayerActionTaskEntity::getPlayerId, playerId)
                        .eq(PlayerActionTaskEntity::getTaskType, ActionTaskTypeEnum.GATHER.getCode())
                        .orderByDesc(PlayerActionTaskEntity::getId)
        );
        for (int i = 0; i < taskEntityList.size(); i++) {
            PlayerGatherTask task = buildGatherTask(taskEntityList.get(i));
            if (task == null) {
                continue;
            }
            if (task.hasPendingRewardToFlush() || task.hasPendingRewardPool()) {
                result.add(task);
            }
        }
        return result;
    }

    /**
     * 新增一条采集任务。
     *
     * @param task 采集任务领域对象
     */
    @Transactional(rollbackFor = Exception.class)
    public void insert(PlayerGatherTask task) {
        validateTask(task);
        normalizeTaskForInsert(task);

        PlayerActionTaskEntity actionTaskEntity = new PlayerActionTaskEntity();
        fillActionTaskEntityForInsert(actionTaskEntity, task);
        playerActionTaskMapper.insert(actionTaskEntity);

        task.setId(actionTaskEntity.getId());

        PlayerGatherTaskEntity gatherTaskEntity = new PlayerGatherTaskEntity();
        fillGatherTaskEntity(gatherTaskEntity, task);
        playerGatherTaskMapper.insert(gatherTaskEntity);
    }

    /**
     * 更新一条采集任务。
     *
     * @param task 采集任务领域对象
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(PlayerGatherTask task) {
        validateTask(task);
        if (task.getId() == null) {
            throw new IllegalArgumentException("更新采集任务时，task.id 不能为空");
        }

        PlayerActionTaskEntity actionTaskEntity = playerActionTaskMapper.selectById(task.getId());
        if (actionTaskEntity == null) {
            throw new IllegalStateException("动作任务根记录不存在，taskId=" + task.getId());
        }

        normalizeTaskForUpdate(task, actionTaskEntity);
        fillActionTaskEntityForUpdate(actionTaskEntity, task);
        int actionRows = playerActionTaskMapper.updateById(actionTaskEntity);
        if (actionRows != 1) {
            throw new IllegalStateException("更新动作任务根记录失败，taskId=" + task.getId());
        }

        PlayerGatherTaskEntity gatherTaskEntity = playerGatherTaskMapper.selectById(task.getId());
        if (gatherTaskEntity == null) {
            gatherTaskEntity = new PlayerGatherTaskEntity();
            fillGatherTaskEntity(gatherTaskEntity, task);
            int insertRows = playerGatherTaskMapper.insert(gatherTaskEntity);
            if (insertRows != 1) {
                throw new IllegalStateException("新增采集任务扩展记录失败，taskId=" + task.getId());
            }
            return;
        }

        fillGatherTaskEntity(gatherTaskEntity, task);
        int gatherRows = playerGatherTaskMapper.updateById(gatherTaskEntity);
        if (gatherRows != 1) {
            throw new IllegalStateException("更新采集任务扩展记录失败，taskId=" + task.getId());
        }
    }

    /**
     * 把根表实体组装成采集任务领域对象。
     *
     * @param taskEntity 根表实体
     * @return 采集任务，不是采集任务时返回 null
     */
    private PlayerGatherTask buildGatherTask(PlayerActionTaskEntity taskEntity) {
        if (taskEntity == null) {
            return null;
        }

        ActionTaskTypeEnum taskType = ActionTaskTypeEnum.fromCode(taskEntity.getTaskType());
        if (!ActionTaskTypeEnum.GATHER.equals(taskType)) {
            return null;
        }

        PlayerGatherTaskEntity gatherTaskEntity = playerGatherTaskMapper.selectById(taskEntity.getId());
        if (gatherTaskEntity == null) {
            throw new IllegalStateException("采集任务扩展记录不存在，taskId=" + taskEntity.getId());
        }

        PlayerGatherTask task = new PlayerGatherTask();
        task.setId(taskEntity.getId());
        task.setPlayerId(taskEntity.getPlayerId());
        task.setServerId(taskEntity.getServerId());
        task.setActionCode(taskEntity.getActionCode());
        task.setTaskType(taskType);
        task.setStartTime(taskEntity.getStartTime());
        task.setLastInteractTime(taskEntity.getLastInteractTime());
        task.setLastSettleTime(taskEntity.getLastSettleTime());
        task.setOfflineExpireAt(taskEntity.getOfflineExpireAt());
        task.setRewardSeed(taskEntity.getRewardSeed());
        task.setStatus(ActionTaskStatusEnum.fromCode(taskEntity.getStatus()));
        task.setStopReason(ActionTaskStopReasonEnum.fromCode(taskEntity.getStopReason()));

        task.setTargetCount(gatherTaskEntity.getTargetCount());
        task.setCompletedCount(gatherTaskEntity.getCompletedCount());
        task.setFlushedCount(gatherTaskEntity.getFlushedCount());
        task.setCurrentSegmentStart(gatherTaskEntity.getCurrentSegmentStart());
        task.setCurrentSegmentEnd(gatherTaskEntity.getCurrentSegmentEnd());
        task.setSegmentSize(gatherTaskEntity.getSegmentSize());
        task.setStatSnapshot(readStatSnapshot(gatherTaskEntity.getStatSnapshot()));
        task.setCurrentSegmentRewardPlanJson(gatherTaskEntity.getCurrentSegmentRewardPlanJson());
        task.setPendingRewardPoolJson(gatherTaskEntity.getPendingRewardPoolJson());
        return task;
    }

    /**
     * 校验采集任务基本参数。
     *
     * @param task 采集任务
     */
    private void validateTask(PlayerGatherTask task) {
        if (task == null) {
            throw new IllegalArgumentException("采集任务不能为空");
        }
        if (task.getPlayerId() == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
        if (!StringUtils.hasText(task.getActionCode())) {
            throw new IllegalArgumentException("actionCode不能为空");
        }
    }

    /**
     * 规范化新增时的默认值。
     *
     * @param task 采集任务
     */
    private void normalizeTaskForInsert(PlayerGatherTask task) {
        if (task.getTaskType() == null) {
            task.setTaskType(ActionTaskTypeEnum.GATHER);
        }
        if (task.getStatus() == null) {
            task.setStatus(ActionTaskStatusEnum.QUEUED);
        }
        if (task.getTargetCount() == null) {
            task.setTargetCount(ActionTaskConstants.INFINITE_TARGET_COUNT);
        }
        if (task.getCompletedCount() == null) {
            task.setCompletedCount(0L);
        }
        if (task.getFlushedCount() == null) {
            task.setFlushedCount(0L);
        }
        if (task.getSegmentSize() == null) {
            task.setSegmentSize(ActionTaskConstants.DEFAULT_SEGMENT_SIZE);
        }
        if (task.getStartTime() == null) {
            task.setStartTime(LocalDateTime.now());
        }
        if (task.getLastSettleTime() == null) {
            task.setLastSettleTime(task.getStartTime());
        }
    }

    /**
     * 规范化更新时的默认值。
     *
     * @param task 采集任务
     * @param actionTaskEntity 已存在的根表实体
     */
    private void normalizeTaskForUpdate(PlayerGatherTask task, PlayerActionTaskEntity actionTaskEntity) {
        if (task.getTaskType() == null) {
            task.setTaskType(ActionTaskTypeEnum.GATHER);
        }
        if (task.getTargetCount() == null) {
            task.setTargetCount(ActionTaskConstants.INFINITE_TARGET_COUNT);
        }
        if (task.getCompletedCount() == null) {
            task.setCompletedCount(0L);
        }
        if (task.getFlushedCount() == null) {
            task.setFlushedCount(0L);
        }
        if (task.getSegmentSize() == null) {
            task.setSegmentSize(ActionTaskConstants.DEFAULT_SEGMENT_SIZE);
        }
        if (task.getStartTime() == null) {
            task.setStartTime(actionTaskEntity.getStartTime());
        }
        if (task.getLastSettleTime() == null) {
            task.setLastSettleTime(actionTaskEntity.getLastSettleTime());
        }
    }

    /**
     * 填充根表实体的新增字段。
     *
     * @param entity 根表实体
     * @param task 采集任务
     */
    private void fillActionTaskEntityForInsert(PlayerActionTaskEntity entity, PlayerGatherTask task) {
        entity.setPlayerId(task.getPlayerId());
        entity.setServerId(task.getServerId());
        entity.setBehaviorId(0L);
        entity.setActionId(0L);
        entity.setCategoryId(0L);
        entity.setSubCategoryId(0L);
        entity.setActivityType(1);
        entity.setTargetId(null);
        entity.setState(buildLegacyState(task.getStatus()));
        entity.setActionCode(task.getActionCode());
        entity.setTaskType(task.getTaskType().getCode());
        entity.setStatus(task.getStatus().getCode());
        entity.setStopReason(getStopReasonCode(task.getStopReason()));
        entity.setStartTime(task.getStartTime());
        entity.setLastInteractTime(task.getLastInteractTime());
        entity.setLastCalcTime(task.getLastSettleTime());
        entity.setLastSettleTime(task.getLastSettleTime());
        entity.setOfflineExpireAt(task.getOfflineExpireAt());
        entity.setRewardSeed(task.getRewardSeed());
        entity.setParamJson(null);
    }

    /**
     * 填充根表实体的更新字段。
     *
     * @param entity 根表实体
     * @param task 采集任务
     */
    private void fillActionTaskEntityForUpdate(PlayerActionTaskEntity entity, PlayerGatherTask task) {
        entity.setServerId(task.getServerId());
        entity.setActionCode(task.getActionCode());
        entity.setTaskType(task.getTaskType().getCode());

        if (task.getStatus() != null) {
            entity.setStatus(task.getStatus().getCode());
            entity.setState(buildLegacyState(task.getStatus()));
        }

        entity.setStopReason(getStopReasonCode(task.getStopReason()));
        entity.setStartTime(task.getStartTime());
        entity.setLastInteractTime(task.getLastInteractTime());
        entity.setLastSettleTime(task.getLastSettleTime());
        entity.setLastCalcTime(task.getLastSettleTime());
        entity.setOfflineExpireAt(task.getOfflineExpireAt());
        entity.setRewardSeed(task.getRewardSeed());
    }

    /**
     * 填充采集扩展实体。
     *
     * @param entity 采集扩展实体
     * @param task 采集任务
     */
    private void fillGatherTaskEntity(PlayerGatherTaskEntity entity, PlayerGatherTask task) {
        entity.setTaskId(task.getId());
        entity.setTargetCount(task.getTargetCount());
        entity.setCompletedCount(task.getCompletedCount());
        entity.setFlushedCount(task.getFlushedCount());
        entity.setCurrentSegmentStart(task.getCurrentSegmentStart());
        entity.setCurrentSegmentEnd(task.getCurrentSegmentEnd());
        entity.setSegmentSize(task.getSegmentSize());
        entity.setStatSnapshot(writeStatSnapshot(task.getStatSnapshot()));
        entity.setCurrentSegmentRewardPlanJson(task.getCurrentSegmentRewardPlanJson());
        entity.setPendingRewardPoolJson(task.getPendingRewardPoolJson());
    }

    /**
     * 根据任务状态构建兼容旧字段的 state。
     *
     * @param status 任务状态
     * @return 旧 state 数值
     */
    private Integer buildLegacyState(ActionTaskStatusEnum status) {
        if (status == null) {
            return 0;
        }
        if (ActionTaskStatusEnum.QUEUED.equals(status)) {
            return 0;
        }
        if (ActionTaskStatusEnum.RUNNING.equals(status)) {
            return 1;
        }
        if (ActionTaskStatusEnum.COMPLETED.equals(status)) {
            return 2;
        }
        if (ActionTaskStatusEnum.STOPPED.equals(status)
                || ActionTaskStatusEnum.EXPIRED.equals(status)
                || ActionTaskStatusEnum.FAILED.equals(status)) {
            return 3;
        }
        return 0;
    }

    /**
     * 获取停止原因编码。
     *
     * @param stopReason 停止原因枚举
     * @return 停止原因编码
     */
    private String getStopReasonCode(ActionTaskStopReasonEnum stopReason) {
        return stopReason == null ? null : stopReason.getCode();
    }

    /**
     * 读取采集快照 JSON。
     *
     * @param json JSON 字符串
     * @return 采集快照对象
     */
    private GatherTaskStatSnapshot readStatSnapshot(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, GatherTaskStatSnapshot.class);
        } catch (IOException e) {
            throw new IllegalStateException("解析采集快照失败", e);
        }
    }

    /**
     * 写出采集快照 JSON。
     *
     * @param snapshot 采集快照
     * @return JSON 字符串
     */
    private String writeStatSnapshot(GatherTaskStatSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化采集快照失败", e);
        }
    }
}