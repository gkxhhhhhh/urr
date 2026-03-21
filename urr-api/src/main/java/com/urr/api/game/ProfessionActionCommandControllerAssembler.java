package com.urr.api.game;

import com.urr.api.game.dto.GatherTaskPanelResponse;
import com.urr.api.game.dto.ProfessionActionCommandRequest;
import com.urr.api.game.dto.ProfessionActionCommandResponse;
import com.urr.api.game.dto.ProfessionActionCommandType;
import com.urr.app.action.task.command.ProfessionActionCommand;
import com.urr.app.action.task.result.ProfessionActionCommandResult;
import com.urr.app.action.task.result.StartGatherTaskResult;
import com.urr.app.action.task.result.StopGatherTaskResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 职业动作命令 Controller 组装器。
 *
 * 说明：
 * 1. 这里只负责 API DTO 和 app command/result 之间的轻量转换。
 * 2. 不承载具体业务规则。
 */
@Component
@RequiredArgsConstructor
public class ProfessionActionCommandControllerAssembler {

    /**
     * 复用现有采集面板组装器。
     */
    private final GatherTaskControllerAssembler gatherTaskControllerAssembler;

    /**
     * 把请求转换成 app 命令。
     *
     * @param accountId 账号ID
     * @param request 接口请求
     * @param commandType 命令类型
     * @return app 命令
     */
    public ProfessionActionCommand toCommand(Long accountId,
                                             ProfessionActionCommandRequest request,
                                             ProfessionActionCommandType commandType) {
        ProfessionActionCommand command = new ProfessionActionCommand();
        command.setAccountId(accountId);
        command.setPlayerId(request.getPlayerId());
        command.setCommandType(commandType == null ? null : commandType.getCode());
        command.setActionCode(request.getActionCode());
        command.setTargetCount(request.getTargetCount());
        return command;
    }

    /**
     * 把 app 层结果转换成接口响应。
     *
     * @param result app 层结果
     * @return 接口响应
     */
    public ProfessionActionCommandResponse toResponse(ProfessionActionCommandResult result) {
        ProfessionActionCommandResponse response = new ProfessionActionCommandResponse();
        if (result == null) {
            response.setSuccess(Boolean.FALSE);
            return response;
        }

        response.setCommandType(result.getCommandType());
        response.setSuccess(Boolean.TRUE.equals(result.getSuccess()));
        response.setMessage(result.getMessage());
        response.setPlayerId(result.getPlayerId());
        response.setActionCode(result.getActionCode());
        response.setTargetCount(result.getTargetCount());
        response.setInfiniteTarget(isInfiniteTarget(result.getTargetCount()));

        fillOperateFields(response, result.getOperateResult());
        fillStopFields(response, result.getStopResult());
        response.setLatestPanel(toLatestPanel(result));

        return response;
    }

    /**
     * 回填启动 / 入队结果字段。
     *
     * @param response 接口响应
     * @param operateResult 启动 / 入队结果
     */
    private void fillOperateFields(ProfessionActionCommandResponse response,
                                   StartGatherTaskResult operateResult) {
        if (response == null || operateResult == null) {
            return;
        }

        response.setTaskId(operateResult.getTaskId());
        response.setQueueId(operateResult.getQueueId());
        response.setStatus(operateResult.getStatus());
        response.setQueued(Boolean.TRUE.equals(operateResult.getQueued()));
        response.setQueuePosition(operateResult.getQueuePosition());
        response.setReplacedTaskId(operateResult.getReplacedTaskId());

        if (operateResult.getPlayerId() != null) {
            response.setPlayerId(operateResult.getPlayerId());
        }
        if (operateResult.getActionCode() != null) {
            response.setActionCode(operateResult.getActionCode());
        }
    }

    /**
     * 回填停止结果字段。
     *
     * @param response 接口响应
     * @param stopResult 停止结果
     */
    private void fillStopFields(ProfessionActionCommandResponse response,
                                StopGatherTaskResult stopResult) {
        if (response == null || stopResult == null) {
            return;
        }

        response.setTaskId(stopResult.getTaskId());
        response.setStatus(stopResult.getStatus());
        response.setStopReason(stopResult.getStopReason());
        response.setCompletedCount(stopResult.getCompletedCount());
        response.setFlushedCount(stopResult.getFlushedCount());
        response.setStopTime(stopResult.getStopTime());

        if (stopResult.getPlayerId() != null) {
            response.setPlayerId(stopResult.getPlayerId());
        }

        ProfessionActionCommandResponse.FlushInfo flushInfo = new ProfessionActionCommandResponse.FlushInfo();
        flushInfo.setFlushedRoundCount(stopResult.getFlushedRoundCount());
        flushInfo.setAppliedRewardEntryCount(stopResult.getAppliedRewardEntryCount());
        flushInfo.setRewardFlushed(Boolean.TRUE.equals(stopResult.getRewardFlushed()));
        response.setFlush(flushInfo);
    }

    /**
     * 转换 latestPanel。
     *
     * @param result app 层结果
     * @return 最新面板响应
     */
    private GatherTaskPanelResponse toLatestPanel(ProfessionActionCommandResult result) {
        if (result == null || result.getLatestPanel() == null) {
            return null;
        }
        return gatherTaskControllerAssembler.toPanelResponse(result.getLatestPanel());
    }

    /**
     * 判断 targetCount 是否为无限次数。
     *
     * @param targetCount 目标次数
     * @return true-无限次数，false-非无限次数
     */
    private boolean isInfiniteTarget(Long targetCount) {
        return targetCount != null && targetCount.longValue() == -1L;
    }
}