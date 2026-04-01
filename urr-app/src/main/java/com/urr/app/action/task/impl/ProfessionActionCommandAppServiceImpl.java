package com.urr.app.action.task.impl;

import com.urr.app.action.task.CraftTaskAppService;
import com.urr.app.action.task.GatherTaskAppService;
import com.urr.app.action.task.PlayerActionTaskRepository;
import com.urr.app.action.task.ProfessionActionCommandAppService;
import com.urr.app.action.task.ProfessionActionReadService;
import com.urr.app.action.task.command.EnqueueGatherTaskCommand;
import com.urr.app.action.task.command.ProfessionActionCommand;
import com.urr.app.action.task.command.StartGatherTaskCommand;
import com.urr.app.action.task.command.StopGatherTaskCommand;
import com.urr.app.action.task.query.QueryGatherTaskPanelQuery;
import com.urr.app.action.task.result.ProfessionActionCommandResult;
import com.urr.app.action.task.result.QueryGatherTaskPanelResult;
import com.urr.app.action.task.result.StartGatherTaskResult;
import com.urr.app.action.task.result.StopGatherTaskResult;
import com.urr.domain.action.task.ActionTaskTypeEnum;
import com.urr.domain.action.task.PlayerActionTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 职业动作命令应用服务实现。
 */
@Service
@RequiredArgsConstructor
public class ProfessionActionCommandAppServiceImpl implements ProfessionActionCommandAppService {

    /**
     * 立即开始命令。
     */
    private static final String COMMAND_START_NOW = "START_NOW";

    /**
     * 入队命令。
     */
    private static final String COMMAND_ENQUEUE = "ENQUEUE";

    /**
     * 停止命令。
     */
    private static final String COMMAND_STOP = "STOP";

    /**
     * 刷新命令。
     */
    private static final String COMMAND_REFRESH = "REFRESH";

    /**
     * 采集任务应用服务。
     */
    private final GatherTaskAppService gatherTaskAppService;

    /**
     * 制造任务应用服务。
     */
    private final CraftTaskAppService craftTaskAppService;

    /**
     * 动作根任务仓储。
     */
    private final PlayerActionTaskRepository playerActionTaskRepository;

    /**
     * 通用职业动作读服务。
     */
    private final ProfessionActionReadService professionActionReadService;

    /**
     * 执行职业动作命令。
     *
     * @param command 职业动作命令
     * @return 命令结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProfessionActionCommandResult execute(ProfessionActionCommand command) {
        validateBaseCommand(command);
        String normalizedCommandType = normalizeCommandType(command.getCommandType());
        command.setCommandType(normalizedCommandType);
        normalizeActionCode(command);
        validateTypeSpecificFields(command);
        ProfessionActionCommandResult result = new ProfessionActionCommandResult();
        result.setCommandType(normalizedCommandType);
        result.setSuccess(Boolean.TRUE);
        result.setPlayerId(command.getPlayerId());
        result.setActionCode(command.getActionCode());
        result.setTargetCount(command.getTargetCount());
        if (COMMAND_START_NOW.equals(normalizedCommandType)) {
            StartGatherTaskResult operateResult = executeStartNow(command);
            result.setOperateResult(operateResult);
            result.setMessage("职业动作已开始");
            if (operateResult != null) {
                result.setActionCode(operateResult.getActionCode());
            }
        } else if (COMMAND_ENQUEUE.equals(normalizedCommandType)) {
            StartGatherTaskResult operateResult = executeEnqueue(command);
            result.setOperateResult(operateResult);
            result.setMessage("职业动作已加入队列");
            if (operateResult != null) {
                result.setActionCode(operateResult.getActionCode());
            }
        } else if (COMMAND_STOP.equals(normalizedCommandType)) {
            StopGatherTaskResult stopResult = executeStop(command);
            result.setStopResult(stopResult);
            result.setMessage("当前职业动作已停止");
        } else {
            result.setMessage("职业动作状态已刷新");
        }
        result.setLatestPanel(loadLatestPanel(command));
        return result;
    }

    /**
     * 执行开始命令。
     *
     * @param command 命令
     * @return 结果
     */
    private StartGatherTaskResult executeStartNow(ProfessionActionCommand command) {
        if (isCraftAction(command.getActionCode())) {
            return craftTaskAppService.startNow(toStartCommand(command));
        }
        return gatherTaskAppService.startNow(toStartCommand(command));
    }

    /**
     * 执行入队命令。
     *
     * @param command 命令
     * @return 结果
     */
    private StartGatherTaskResult executeEnqueue(ProfessionActionCommand command) {
        if (isCraftAction(command.getActionCode())) {
            return craftTaskAppService.enqueue(toEnqueueCommand(command));
        }
        return gatherTaskAppService.enqueue(toEnqueueCommand(command));
    }

    /**
     * 执行停止命令。
     *
     * @param command 命令
     * @return 结果
     */
    private StopGatherTaskResult executeStop(ProfessionActionCommand command) {
        PlayerActionTask runningTask = playerActionTaskRepository.findRunningByPlayerId(command.getPlayerId());
        if (runningTask != null && ActionTaskTypeEnum.CRAFT.equals(runningTask.getTaskType())) {
            return craftTaskAppService.stopCurrent(toStopCommand(command));
        }
        return gatherTaskAppService.stopCurrent(toStopCommand(command));
    }

    /**
     * 判断是否制造动作。
     *
     * @param actionCode 动作编码
     * @return true-制造，false-非制造
     */
    private boolean isCraftAction(String actionCode) {
        return StringUtils.hasText(actionCode) && actionCode.trim().toUpperCase().startsWith("CRAFT_");
    }

    /**
     * 校验命令基础字段。
     *
     * @param command 职业动作命令
     */
    private void validateBaseCommand(ProfessionActionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("职业动作命令不能为空");
        }
        if (command.getAccountId() == null) {
            throw new IllegalArgumentException("未登录");
        }
        if (command.getPlayerId() == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
        if (!StringUtils.hasText(command.getCommandType())) {
            throw new IllegalArgumentException("commandType不能为空");
        }
    }

    /**
     * 规范化命令类型。
     *
     * @param commandType 原始命令类型
     * @return 规范化结果
     */
    private String normalizeCommandType(String commandType) {
        if (!StringUtils.hasText(commandType)) {
            throw new IllegalArgumentException("commandType不能为空");
        }
        String normalized = commandType.trim();
        if (COMMAND_START_NOW.equalsIgnoreCase(normalized)) {
            return COMMAND_START_NOW;
        }
        if (COMMAND_ENQUEUE.equalsIgnoreCase(normalized)) {
            return COMMAND_ENQUEUE;
        }
        if (COMMAND_STOP.equalsIgnoreCase(normalized)) {
            return COMMAND_STOP;
        }
        if (COMMAND_REFRESH.equalsIgnoreCase(normalized)) {
            return COMMAND_REFRESH;
        }
        throw new IllegalArgumentException("commandType 只能是 START_NOW / ENQUEUE / STOP / REFRESH");
    }

    /**
     * 规范化动作编码。
     *
     * @param command 命令
     */
    private void normalizeActionCode(ProfessionActionCommand command) {
        if (command == null) {
            return;
        }
        if (!StringUtils.hasText(command.getActionCode())) {
            return;
        }
        command.setActionCode(command.getActionCode().trim());
    }

    /**
     * 校验类型相关字段。
     *
     * @param command 命令
     */
    private void validateTypeSpecificFields(ProfessionActionCommand command) {
        if (command == null) {
            return;
        }
        if (needActionCode(command.getCommandType()) && !StringUtils.hasText(command.getActionCode())) {
            throw new IllegalArgumentException("actionCode不能为空");
        }
        if (needTargetCount(command.getCommandType())) {
            validateTargetCount(command.getTargetCount());
        }
    }

    /**
     * 判断是否需要 actionCode。
     *
     * @param commandType 命令类型
     * @return true-需要，false-不需要
     */
    private boolean needActionCode(String commandType) {
        return COMMAND_START_NOW.equals(commandType) || COMMAND_ENQUEUE.equals(commandType);
    }

    /**
     * 判断是否需要 targetCount。
     *
     * @param commandType 命令类型
     * @return true-需要，false-不需要
     */
    private boolean needTargetCount(String commandType) {
        return COMMAND_START_NOW.equals(commandType) || COMMAND_ENQUEUE.equals(commandType);
    }

    /**
     * 校验 targetCount。
     *
     * @param targetCount 目标次数
     */
    private void validateTargetCount(Long targetCount) {
        if (targetCount == null) {
            throw new IllegalArgumentException("targetCount不能为空");
        }
        if (targetCount.longValue() == -1L) {
            return;
        }
        if (targetCount.longValue() <= 0L) {
            throw new IllegalArgumentException("targetCount 只能是正整数或 -1");
        }
    }

    /**
     * 转换成开始命令。
     *
     * @param command 职业动作命令
     * @return 开始命令
     */
    private StartGatherTaskCommand toStartCommand(ProfessionActionCommand command) {
        StartGatherTaskCommand startCommand = new StartGatherTaskCommand();
        startCommand.setAccountId(command.getAccountId());
        startCommand.setPlayerId(command.getPlayerId());
        startCommand.setActionCode(command.getActionCode());
        startCommand.setTargetCount(command.getTargetCount());
        startCommand.setEquipInstanceId(command.getEquipInstanceId());
        startCommand.setTeaType(command.getTeaType());
        startCommand.setBlessedTeaUsed(command.getBlessedTeaUsed());
        startCommand.setDrinkConcentration(command.getDrinkConcentration());
        startCommand.setObservatoryLevel(command.getObservatoryLevel());
        startCommand.setExtraSuccessRate(command.getExtraSuccessRate());
        return startCommand;
    }

    /**
     * 转换成入队命令。
     *
     * @param command 职业动作命令
     * @return 入队命令
     */
    private EnqueueGatherTaskCommand toEnqueueCommand(ProfessionActionCommand command) {
        EnqueueGatherTaskCommand enqueueCommand = new EnqueueGatherTaskCommand();
        enqueueCommand.setAccountId(command.getAccountId());
        enqueueCommand.setPlayerId(command.getPlayerId());
        enqueueCommand.setActionCode(command.getActionCode());
        enqueueCommand.setTargetCount(command.getTargetCount());
        enqueueCommand.setEquipInstanceId(command.getEquipInstanceId());
        enqueueCommand.setTeaType(command.getTeaType());
        enqueueCommand.setBlessedTeaUsed(command.getBlessedTeaUsed());
        enqueueCommand.setDrinkConcentration(command.getDrinkConcentration());
        enqueueCommand.setObservatoryLevel(command.getObservatoryLevel());
        enqueueCommand.setExtraSuccessRate(command.getExtraSuccessRate());
        return enqueueCommand;
    }

    /**
     * 转换成停止命令。
     *
     * @param command 职业动作命令
     * @return 停止命令
     */
    private StopGatherTaskCommand toStopCommand(ProfessionActionCommand command) {
        StopGatherTaskCommand stopCommand = new StopGatherTaskCommand();
        stopCommand.setAccountId(command.getAccountId());
        stopCommand.setPlayerId(command.getPlayerId());
        return stopCommand;
    }

    /**
     * 读取最新面板。
     *
     * @param command 职业动作命令
     * @return 面板结果
     */
    private QueryGatherTaskPanelResult loadLatestPanel(ProfessionActionCommand command) {
        QueryGatherTaskPanelQuery query = new QueryGatherTaskPanelQuery();
        query.setAccountId(command.getAccountId());
        query.setPlayerId(command.getPlayerId());
        query.setReadTime(null);
        return professionActionReadService.queryPanel(query);
    }
}