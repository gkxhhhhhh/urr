package com.urr.app.action.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urr.app.action.task.CraftTaskAppService;
import com.urr.app.action.task.CraftTaskSettleService;
import com.urr.app.action.task.GatherTaskInventoryConsumePrepareService;
import com.urr.app.action.task.PlayerActionQueueRepository;
import com.urr.app.action.task.PlayerActionTaskRepository;
import com.urr.app.action.task.PlayerCraftTaskRepository;
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
import com.urr.domain.action.task.CraftTaskTimeSupport;
import com.urr.domain.action.task.PlayerActionQueueEntity;
import com.urr.domain.action.task.PlayerActionTask;
import com.urr.domain.action.task.PlayerActionTaskEntity;
import com.urr.domain.action.task.PlayerCraftTask;
import com.urr.domain.craft.RecipeDefEntity;
import com.urr.domain.player.PlayerEntity;
import com.urr.domain.skill.PlayerSkillEntity;
import com.urr.domain.skill.SkillDefEntity;
import com.urr.infra.mapper.ActionDefMapper;
import com.urr.infra.mapper.PlayerActionTaskMapper;
import com.urr.infra.mapper.PlayerMapper;
import com.urr.infra.mapper.PlayerSkillMapper;
import com.urr.infra.mapper.SkillDefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 制造任务应用服务实现。
 */
@Service
@RequiredArgsConstructor
public class CraftTaskAppServiceImpl implements CraftTaskAppService {

    /**
     * 玩家 Mapper。
     */
    private final PlayerMapper playerMapper;

    /**
     * 动作定义 Mapper。
     */
    private final ActionDefMapper actionDefMapper;

    /**
     * 技能定义 Mapper。
     */
    private final SkillDefMapper skillDefMapper;

    /**
     * 玩家技能 Mapper。
     */
    private final PlayerSkillMapper playerSkillMapper;

    /**
     * 根任务 Mapper。
     */
    private final PlayerActionTaskMapper playerActionTaskMapper;

    /**
     * 根任务仓储。
     */
    private final PlayerActionTaskRepository playerActionTaskRepository;

    /**
     * 动作队列仓储。
     */
    private final PlayerActionQueueRepository playerActionQueueRepository;

    /**
     * 制造任务仓储。
     */
    private final PlayerCraftTaskRepository playerCraftTaskRepository;

    /**
     * 制造结算服务。
     */
    private final CraftTaskSettleService craftTaskSettleService;

    /**
     * 正式扣库存前准备服务。
     */
    private final GatherTaskInventoryConsumePrepareService gatherTaskInventoryConsumePrepareService;

    /**
     * JSON 工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 立即开始制造。
     *
     * @param command 开始命令
     * @return 启动结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StartGatherTaskResult startNow(StartGatherTaskCommand command) {
        validateStartCommand(command);
        PlayerEntity player = requireOwnedPlayer(command.getAccountId(), command.getPlayerId());
        ActionDefEntity actionDef = requireCraftAction(command.getActionCode());
        String recipeCode = resolveRecipeCode(actionDef);
        RecipeDefEntity recipeDef = requireRecipe(recipeCode);
        validateCraftLevel(player.getId(), recipeDef, actionDef);
        LocalDateTime now = LocalDateTime.now();
        Long replacedTaskId = replaceRunningTaskIfNecessary(player.getId(), now);
        PlayerActionTaskEntity rootTask = buildRootTask(player, actionDef, now);
        int insertRows = playerActionTaskMapper.insert(rootTask);
        if (insertRows != 1 || rootTask.getId() == null) {
            throw new IllegalStateException("创建制造根任务失败");
        }
        PlayerCraftTask craftTask = buildCraftTask(rootTask, player, actionDef, recipeDef, command.getTargetCount(), now);
        playerCraftTaskRepository.insert(craftTask);
        return buildStartNowResult(rootTask.getId(), player.getId(), actionDef.getActionCode(), command.getTargetCount(), replacedTaskId);
    }

    /**
     * 制造入队。
     *
     * @param command 入队命令
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StartGatherTaskResult enqueue(EnqueueGatherTaskCommand command) {
        validateEnqueueCommand(command);
        PlayerEntity player = requireOwnedPlayer(command.getAccountId(), command.getPlayerId());
        ActionDefEntity actionDef = requireCraftAction(command.getActionCode());
        String recipeCode = resolveRecipeCode(actionDef);
        RecipeDefEntity recipeDef = requireRecipe(recipeCode);
        validateCraftLevel(player.getId(), recipeDef, actionDef);
        PlayerActionTask runningTask = playerActionTaskRepository.findRunningByPlayerId(player.getId());
        if (runningTask == null) {
            StartGatherTaskCommand startCommand = new StartGatherTaskCommand();
            startCommand.setAccountId(command.getAccountId());
            startCommand.setPlayerId(command.getPlayerId());
            startCommand.setActionCode(command.getActionCode());
            startCommand.setTargetCount(command.getTargetCount());
            return startNow(startCommand);
        }
        long queuedCount = playerActionQueueRepository.countQueuedByPlayerId(player.getId());
        if (queuedCount >= ActionTaskConstants.MAX_QUEUE_SIZE_PER_PLAYER) {
            throw new IllegalStateException("当前队列已满");
        }
        PlayerActionQueueEntity queueEntity = new PlayerActionQueueEntity();
        queueEntity.setPlayerId(player.getId());
        queueEntity.setServerId(player.getServerId());
        queueEntity.setTaskType(ActionTaskTypeEnum.CRAFT.getCode());
        queueEntity.setActionCode(actionDef.getActionCode());
        queueEntity.setTargetCount(command.getTargetCount());
        queueEntity.setStatus(ActionTaskStatusEnum.QUEUED.getCode());
        queueEntity.setRemarks("craft_queue");
        queueEntity.setCreateUser("-1");
        queueEntity.setUpdateUser("-1");
        playerActionQueueRepository.insert(queueEntity);
        StartGatherTaskResult result = new StartGatherTaskResult();
        result.setTaskId(null);
        result.setQueueId(queueEntity.getId());
        result.setPlayerId(player.getId());
        result.setActionCode(actionDef.getActionCode());
        result.setStatus(ActionTaskStatusEnum.QUEUED.getCode());
        result.setQueued(Boolean.TRUE);
        result.setQueuePosition(playerActionQueueRepository.calculateQueuePosition(player.getId(), queueEntity.getId()));
        result.setReplacedTaskId(null);
        return result;
    }

    /**
     * 停止当前制造任务。
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
        if (runningTask == null || !ActionTaskTypeEnum.CRAFT.equals(runningTask.getTaskType())) {
            throw new IllegalStateException("当前没有运行中的制造任务");
        }
        LocalDateTime now = LocalDateTime.now();
        PlayerCraftTask latestTask = craftTaskSettleService.settleTo(runningTask.getId(), now);
        PlayerActionTaskEntity rootEntity = playerActionTaskMapper.selectById(runningTask.getId());
        if (rootEntity == null) {
            throw new IllegalStateException("制造根任务不存在");
        }
        if (ActionTaskStatusEnum.RUNNING.getCode().equals(rootEntity.getStatus())) {
            rootEntity.setStatus(ActionTaskStatusEnum.STOPPED.getCode());
            rootEntity.setStopReason(ActionTaskStopReasonEnum.USER_STOP.getCode());
            rootEntity.setState(3);
            rootEntity.setLastSettleTime(now);
            rootEntity.setLastCalcTime(now);
            rootEntity.setUpdateUser("-1");
            playerActionTaskMapper.updateById(rootEntity);
            playerCraftTaskRepository.updateStatusByTaskId(runningTask.getId(), ActionTaskStatusEnum.STOPPED);
        }
        PlayerCraftTask finalTask = playerCraftTaskRepository.findByTaskId(runningTask.getId());
        StopGatherTaskResult result = new StopGatherTaskResult();
        result.setTaskId(runningTask.getId());
        result.setPlayerId(player.getId());
        result.setStatus(rootEntity.getStatus());
        result.setStopReason(rootEntity.getStopReason());
        result.setCompletedCount(finalTask == null ? 0L : finalTask.getCompletedCount());
        result.setFlushedCount(finalTask == null ? 0L : finalTask.getCompletedCount());
        result.setFlushedRoundCount(0L);
        result.setAppliedRewardEntryCount(0);
        result.setRewardFlushed(Boolean.FALSE);
        result.setStopTime(now);
        return result;
    }

    /**
     * 校验开始命令。
     *
     * @param command 开始命令
     */
    private void validateStartCommand(StartGatherTaskCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("开始命令不能为空");
        }
        if (command.getAccountId() == null) {
            throw new IllegalArgumentException("accountId不能为空");
        }
        if (command.getPlayerId() == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
        if (!StringUtils.hasText(command.getActionCode())) {
            throw new IllegalArgumentException("actionCode不能为空");
        }
        validateTargetCount(command.getTargetCount());
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
            throw new IllegalArgumentException("accountId不能为空");
        }
        if (command.getPlayerId() == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
        if (!StringUtils.hasText(command.getActionCode())) {
            throw new IllegalArgumentException("actionCode不能为空");
        }
        validateTargetCount(command.getTargetCount());
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
            throw new IllegalArgumentException("accountId不能为空");
        }
        if (command.getPlayerId() == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
    }

    /**
     * 校验目标次数。
     *
     * @param targetCount 目标次数
     */
    private void validateTargetCount(Long targetCount) {
        if (targetCount == null) {
            throw new IllegalArgumentException("targetCount不能为空");
        }
        if (targetCount.longValue() == ActionTaskConstants.INFINITE_TARGET_COUNT) {
            return;
        }
        if (targetCount.longValue() <= 0L) {
            throw new IllegalArgumentException("targetCount 只能是正整数或 -1");
        }
    }

    /**
     * 校验玩家归属。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @return 玩家实体
     */
    private PlayerEntity requireOwnedPlayer(Long accountId, Long playerId) {
        PlayerEntity player = playerMapper.selectOne(
                new LambdaQueryWrapper<PlayerEntity>()
                        .eq(PlayerEntity::getId, playerId)
                        .eq(PlayerEntity::getAccountId, accountId)
                        .eq(PlayerEntity::getDeleteFlag, 0)
                        .last("limit 1")
        );
        if (player == null) {
            throw new IllegalStateException("角色不存在或无权限操作");
        }
        return player;
    }

    /**
     * 校验制造动作。
     *
     * @param actionCode 动作编码
     * @return 动作定义
     */
    private ActionDefEntity requireCraftAction(String actionCode) {
        ActionDefEntity actionDef = actionDefMapper.selectOne(
                new LambdaQueryWrapper<ActionDefEntity>()
                        .eq(ActionDefEntity::getActionCode, actionCode.trim())
                        .eq(ActionDefEntity::getDeleteFlag, 0)
                        .last("limit 1")
        );
        if (actionDef == null) {
            throw new IllegalStateException("制造动作不存在，actionCode=" + actionCode);
        }
        if (!actionCode.trim().toUpperCase().startsWith("CRAFT_")) {
            throw new IllegalStateException("当前动作不是制造动作，actionCode=" + actionCode);
        }
        return actionDef;
    }

    /**
     * 校验配方。
     *
     * @param recipeCode 配方编码
     * @return 配方定义
     */
    private RecipeDefEntity requireRecipe(String recipeCode) {
        RecipeDefEntity recipeDef = playerCraftTaskRepository.findRecipeByCode(recipeCode);
        if (recipeDef == null) {
            throw new IllegalStateException("制造配方不存在，recipeCode=" + recipeCode);
        }
        return recipeDef;
    }

    /**
     * 校验制造等级。
     *
     * @param playerId 玩家ID
     * @param recipeDef 配方
     * @param actionDef 动作
     */
    private void validateCraftLevel(Long playerId, RecipeDefEntity recipeDef, ActionDefEntity actionDef) {
        if (recipeDef == null) {
            return;
        }
        if (allowBelowLevelAttempt(recipeDef)) {
            return;
        }
        int requiredLevel = recipeDef.getCraftLevelReq() == null ? 1 : recipeDef.getCraftLevelReq().intValue();
        if (actionDef != null && actionDef.getMinSkillLevel() != null && actionDef.getMinSkillLevel().intValue() > requiredLevel) {
            requiredLevel = actionDef.getMinSkillLevel().intValue();
        }
        int currentLevel = resolvePlayerSkillLevel(playerId, recipeDef.getCraftSkillId());
        if (currentLevel < requiredLevel) {
            throw new IllegalStateException("制造等级不足");
        }
    }

    /**
     * 判断配方是否允许低于需求等级尝试。
     *
     * @param recipeDef 配方
     * @return true-允许，false-不允许
     */
    private boolean allowBelowLevelAttempt(RecipeDefEntity recipeDef) {
        if (recipeDef == null || !StringUtils.hasText(recipeDef.getMetaJson())) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(recipeDef.getMetaJson());
            JsonNode node = root.get("allowBelowLevelAttempt");
            return node != null && !node.isNull() && node.asBoolean(false);
        } catch (Exception ignore) {
            return false;
        }
    }

    /**
     * 解析玩家技能等级。
     *
     * @param playerId 玩家ID
     * @param skillId 技能ID
     * @return 技能等级
     */
    private int resolvePlayerSkillLevel(Long playerId, Long skillId) {
        if (playerId == null || skillId == null) {
            return 1;
        }
        PlayerSkillEntity playerSkill = playerSkillMapper.selectOne(
                new LambdaQueryWrapper<PlayerSkillEntity>()
                        .eq(PlayerSkillEntity::getPlayerId, playerId)
                        .eq(PlayerSkillEntity::getSkillId, skillId)
                        .eq(PlayerSkillEntity::getDeleteFlag, 0)
                        .last("limit 1")
        );
        if (playerSkill == null || playerSkill.getSkillLevel() == null || playerSkill.getSkillLevel().intValue() <= 0) {
            return 1;
        }
        return playerSkill.getSkillLevel().intValue();
    }

    /**
     * 替换当前运行任务。
     *
     * @param playerId 玩家ID
     * @param operateTime 操作时间
     * @return 被替换任务ID
     */
    private Long replaceRunningTaskIfNecessary(Long playerId, LocalDateTime operateTime) {
        PlayerActionTask runningTask = playerActionTaskRepository.findRunningByPlayerId(playerId);
        if (runningTask == null) {
            return null;
        }
        if (ActionTaskTypeEnum.GATHER.equals(runningTask.getTaskType())) {
            gatherTaskInventoryConsumePrepareService.prepareBeforeConsume(playerId, operateTime);
            playerActionTaskRepository.stopByUserReplace(runningTask.getId());
            return runningTask.getId();
        }
        if (ActionTaskTypeEnum.CRAFT.equals(runningTask.getTaskType())) {
            craftTaskSettleService.settleTo(runningTask.getId(), operateTime);
            playerCraftTaskRepository.updateStatusByTaskId(runningTask.getId(), ActionTaskStatusEnum.STOPPED);
            playerActionTaskRepository.stopByUserReplace(runningTask.getId());
            return runningTask.getId();
        }
        playerActionTaskRepository.stopByUserReplace(runningTask.getId());
        return runningTask.getId();
    }

    /**
     * 构建制造根任务。
     *
     * @param player 玩家
     * @param actionDef 动作
     * @param now 当前时间
     * @return 根任务实体
     */
    private PlayerActionTaskEntity buildRootTask(PlayerEntity player, ActionDefEntity actionDef, LocalDateTime now) {
        PlayerActionTaskEntity entity = new PlayerActionTaskEntity();
        entity.setPlayerId(player.getId());
        entity.setServerId(player.getServerId());
        entity.setActionId(actionDef.getId());
        entity.setActionCode(actionDef.getActionCode());
        entity.setBehaviorId(actionDef.getBehaviorId());
        entity.setCategoryId(actionDef.getCategoryId());
        entity.setSubCategoryId(actionDef.getSubCategoryId());
        entity.setActivityType(0);
        entity.setTargetId(0L);
        entity.setState(1);
        entity.setTaskType(ActionTaskTypeEnum.CRAFT.getCode());
        entity.setStatus(ActionTaskStatusEnum.RUNNING.getCode());
        entity.setStopReason(null);
        entity.setStartTime(now);
        entity.setLastInteractTime(now);
        entity.setLastCalcTime(now);
        entity.setLastSettleTime(now);
        entity.setOfflineExpireAt(now.plusHours(ActionTaskConstants.DEFAULT_OFFLINE_HOURS));
        entity.setRewardSeed(System.nanoTime());
        entity.setParamJson("{}");
        entity.setRemarks("craft_task");
        entity.setCreateUser("-1");
        entity.setUpdateUser("-1");
        return entity;
    }

    /**
     * 构建制造明细任务。
     *
     * @param rootTask 根任务
     * @param player 玩家
     * @param actionDef 动作
     * @param recipeDef 配方
     * @param targetCount 目标次数
     * @param now 当前时间
     * @return 明细任务
     */
    private PlayerCraftTask buildCraftTask(PlayerActionTaskEntity rootTask,
                                           PlayerEntity player,
                                           ActionDefEntity actionDef,
                                           RecipeDefEntity recipeDef,
                                           Long targetCount,
                                           LocalDateTime now) {
        PlayerCraftTask task = new PlayerCraftTask();
        task.setId(rootTask.getId());
        task.setPlayerId(player.getId());
        task.setServerId(player.getServerId());
        task.setActionCode(actionDef.getActionCode());
        task.setStartTime(now);
        task.setLastInteractTime(now);
        task.setLastSettleTime(now);
        task.setOfflineExpireAt(rootTask.getOfflineExpireAt());
        task.setRewardSeed(rootTask.getRewardSeed());
        task.setStatus(ActionTaskStatusEnum.RUNNING);
        task.setStopReason(null);
        task.setTargetCount(targetCount != null && targetCount.longValue() == ActionTaskConstants.INFINITE_TARGET_COUNT ? 0L : targetCount);
        task.setCompletedCount(0L);
        task.setRecipeSnapshotJson(buildRecipeSnapshotJson(actionDef, recipeDef));
        LocalDateTime firstFinishTime = now.plusNanos(recipeDef.getCraftTimeMs().longValue() * 1_000_000L);
        task.setNextRoundFinishTime(CraftTaskTimeSupport.toEpochMilli(firstFinishTime));
        return task;
    }

    /**
     * 构建配方快照 JSON。
     *
     * @param actionDef 动作
     * @param recipeDef 配方
     * @return 快照 JSON
     */
    private String buildRecipeSnapshotJson(ActionDefEntity actionDef, RecipeDefEntity recipeDef) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
            snapshot.put("actionCode", actionDef.getActionCode());
            snapshot.put("recipeCode", recipeDef.getRecipeCode());
            snapshot.put("skillId", recipeDef.getCraftSkillId());
            snapshot.put("skillCode", resolveSkillCode(recipeDef.getCraftSkillId()));
            snapshot.put("craftTimeMs", recipeDef.getCraftTimeMs());
            snapshot.put("craftLevelReq", recipeDef.getCraftLevelReq());
            snapshot.put("expGain", recipeDef.getExpGain());
            snapshot.put("costMap", normalizeJsonObject(recipeDef.getCostJson()));
            snapshot.put("outputMap", normalizeJsonObject(recipeDef.getOutputJson()));
            snapshot.put("metaJson", recipeDef.getMetaJson());
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception exception) {
            throw new IllegalStateException("生成制造配方快照失败", exception);
        }
    }

    /**
     * 解析技能编码。
     *
     * @param skillId 技能ID
     * @return 技能编码
     */
    private String resolveSkillCode(Long skillId) {
        if (skillId == null) {
            return null;
        }
        SkillDefEntity skillDef = skillDefMapper.selectById(skillId);
        return skillDef == null ? null : skillDef.getSkillCode();
    }

    /**
     * 把 JSON 对象规范成 map。
     *
     * @param json JSON 文本
     * @return map
     */
    private Map<String, Long> normalizeJsonObject(String json) {
        Map<String, Long> result = new LinkedHashMap<String, Long>();
        if (!StringUtils.hasText(json)) {
            return result;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                return result;
            }
            Iterator<Map.Entry<String, JsonNode>> iterator = root.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                long quantity = entry.getValue().asLong(0L);
                if (quantity <= 0L) {
                    continue;
                }
                result.put(entry.getKey(), quantity);
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("解析配方 JSON 失败", exception);
        }
    }

    /**
     * 解析动作上的 recipeCode。
     *
     * @param actionDef 动作定义
     * @return 配方编码
     */
    private String resolveRecipeCode(ActionDefEntity actionDef) {
        if (actionDef == null || !StringUtils.hasText(actionDef.getParamsJson())) {
            if (actionDef == null || !StringUtils.hasText(actionDef.getActionCode())) {
                throw new IllegalStateException("制造动作缺少 recipeCode");
            }
            return actionDef.getActionCode().replaceFirst("^CRAFT_", "RCP_");
        }
        try {
            JsonNode root = objectMapper.readTree(actionDef.getParamsJson());
            JsonNode recipeNode = root.get("recipeCode");
            if (recipeNode != null && !recipeNode.isNull() && StringUtils.hasText(recipeNode.asText())) {
                return recipeNode.asText().trim();
            }
        } catch (Exception ignore) {
        }
        if (!StringUtils.hasText(actionDef.getActionCode())) {
            throw new IllegalStateException("制造动作缺少 recipeCode");
        }
        return actionDef.getActionCode().replaceFirst("^CRAFT_", "RCP_");
    }

    /**
     * 构建立即开始结果。
     *
     * @param taskId 任务ID
     * @param playerId 玩家ID
     * @param actionCode 动作编码
     * @param targetCount 目标次数
     * @param replacedTaskId 被替换任务ID
     * @return 结果
     */
    private StartGatherTaskResult buildStartNowResult(Long taskId,
                                                      Long playerId,
                                                      String actionCode,
                                                      Long targetCount,
                                                      Long replacedTaskId) {
        StartGatherTaskResult result = new StartGatherTaskResult();
        result.setTaskId(taskId);
        result.setQueueId(null);
        result.setPlayerId(playerId);
        result.setActionCode(actionCode);
        result.setStatus(ActionTaskStatusEnum.RUNNING.getCode());
        result.setQueued(Boolean.FALSE);
        result.setQueuePosition(null);
        result.setReplacedTaskId(replacedTaskId);
        return result;
    }
}