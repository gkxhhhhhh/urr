package com.urr.api.game;

import com.urr.api.auth.AuthRequired;
import com.urr.api.game.dto.GatherTaskOperateMode;
import com.urr.api.game.dto.GatherTaskPanelResponse;
import com.urr.api.game.dto.OperateGatherTaskRequest;
import com.urr.api.game.dto.OperateGatherTaskResponse;
import com.urr.api.game.dto.QueryGatherTaskPanelRequest;
import com.urr.api.game.dto.StopGatherTaskRequest;
import com.urr.api.game.dto.StopGatherTaskResponse;
import com.urr.app.action.task.GatherTaskAppService;
import com.urr.app.action.task.GatherTaskReadService;
import com.urr.app.action.task.result.QueryGatherTaskPanelResult;
import com.urr.app.action.task.result.StartGatherTaskResult;
import com.urr.app.action.task.result.StopGatherTaskResult;
import com.urr.commons.api.ResponseData;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 采集任务 Controller。
 *
 * 说明：
 * 1. 这里只做收参、调服务、转接口出参、做最小错误收口。
 * 2. 不在这里实现启动逻辑、不在这里实现 stop/flush 逻辑、不在这里实现 read 逻辑。
 * 3. “立即开始 / 加入队列”统一走一个写接口，通过 mode 区分。
 */
@RestController
@RequestMapping("/api/game/gather-tasks")
@RequiredArgsConstructor
@Validated
public class GatherTaskController {

    /**
     * 采集任务应用服务。
     */
    private final GatherTaskAppService gatherTaskAppService;

    /**
     * 采集任务读服务。
     */
    private final GatherTaskReadService gatherTaskReadService;

    /**
     * Controller 组装器。
     */
    private final GatherTaskControllerAssembler gatherTaskControllerAssembler;

    /**
     * 采集接口异常翻译器。
     */
    private final GatherTaskApiExceptionTranslator gatherTaskApiExceptionTranslator;

    /**
     * 统一处理“立即开始 / 加入队列”写接口。
     *
     * @param request 写接口请求
     * @return 写接口响应
     */
    @PostMapping
    public ResponseData<OperateGatherTaskResponse> operate(@RequestBody @Valid OperateGatherTaskRequest request) {
        Long accountId = AuthRequired.requireAccountId();
        GatherTaskOperateMode mode = parseOperateMode(request.getMode());
        validateTargetCount(request.getTargetCount());

        try {
            StartGatherTaskResult result = doOperate(accountId, request, mode);
            OperateGatherTaskResponse response = gatherTaskControllerAssembler.toOperateResponse(mode, request, result);
            return ResponseData.success(response);
        } catch (RuntimeException exception) {
            throw gatherTaskApiExceptionTranslator.translate(exception);
        }
    }

    /**
     * 停止当前运行中的采集任务。
     *
     * @param request 停止请求
     * @return 停止结果
     */
    @PostMapping("/stop")
    public ResponseData<StopGatherTaskResponse> stop(@RequestBody @Valid StopGatherTaskRequest request) {
        Long accountId = AuthRequired.requireAccountId();

        try {
            StopGatherTaskResult result = gatherTaskAppService.stopCurrent(
                    gatherTaskControllerAssembler.toStopCommand(accountId, request)
            );
            StopGatherTaskResponse response = gatherTaskControllerAssembler.toStopResponse(result);
            return ResponseData.success(response);
        } catch (RuntimeException exception) {
            throw gatherTaskApiExceptionTranslator.translate(exception);
        }
    }

    /**
     * 查询采集面板。
     *
     * @param request 面板查询请求
     * @return 面板响应
     */
    @GetMapping("/panel")
    public ResponseData<GatherTaskPanelResponse> queryPanel(@Valid QueryGatherTaskPanelRequest request) {
        Long accountId = AuthRequired.requireAccountId();

        try {
            QueryGatherTaskPanelResult result = gatherTaskReadService.queryPanel(
                    gatherTaskControllerAssembler.toPanelQuery(accountId, request)
            );
            GatherTaskPanelResponse response = gatherTaskControllerAssembler.toPanelResponse(result);
            return ResponseData.success(response);
        } catch (RuntimeException exception) {
            throw gatherTaskApiExceptionTranslator.translate(exception);
        }
    }

    /**
     * 按 mode 分发到具体写服务。
     *
     * @param accountId 账号ID
     * @param request 写接口请求
     * @param mode 操作模式
     * @return 写接口结果
     */
    private StartGatherTaskResult doOperate(Long accountId,
                                            OperateGatherTaskRequest request,
                                            GatherTaskOperateMode mode) {
        if (GatherTaskOperateMode.START_NOW.equals(mode)) {
            return gatherTaskAppService.startNow(
                    gatherTaskControllerAssembler.toStartCommand(accountId, request)
            );
        }

        return gatherTaskAppService.enqueue(
                gatherTaskControllerAssembler.toEnqueueCommand(accountId, request)
        );
    }

    /**
     * 解析写接口模式。
     *
     * @param modeCode 模式编码
     * @return 模式枚举
     */
    private GatherTaskOperateMode parseOperateMode(String modeCode) {
        GatherTaskOperateMode mode = GatherTaskOperateMode.fromCode(modeCode);
        if (mode == null) {
            throw new IllegalArgumentException("mode 只能是 START_NOW 或 ENQUEUE");
        }
        return mode;
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
        if (targetCount.longValue() == -1L) {
            return;
        }
        if (targetCount.longValue() <= 0L) {
            throw new IllegalArgumentException("targetCount 只能是正整数或 -1");
        }
    }
}