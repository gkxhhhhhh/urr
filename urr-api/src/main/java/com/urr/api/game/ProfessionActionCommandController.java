package com.urr.api.game;

import com.urr.api.auth.AuthRequired;
import com.urr.api.game.dto.ProfessionActionCommandRequest;
import com.urr.api.game.dto.ProfessionActionCommandResponse;
import com.urr.api.game.dto.ProfessionActionCommandType;
import com.urr.app.action.task.ProfessionActionCommandAppService;
import com.urr.app.action.task.result.ProfessionActionCommandResult;
import com.urr.commons.api.ResponseData;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 职业动作命令 Controller。
 *
 * 说明：
 * 1. 统一收口职业动作命令写入口。
 * 2. 当前内部仍只复用已落地的 gather task 能力。
 * 3. 不在这里扩散到市场、战斗、首页初始化快照。
 */
@RestController
@RequestMapping("/api/game/profession-actions")
@RequiredArgsConstructor
@Validated
public class ProfessionActionCommandController {

    /**
     * 职业动作命令应用服务。
     */
    private final ProfessionActionCommandAppService professionActionCommandAppService;

    /**
     * 职业动作命令组装器。
     */
    private final ProfessionActionCommandControllerAssembler professionActionCommandControllerAssembler;

    /**
     * 采集接口异常翻译器。
     */
    private final GatherTaskApiExceptionTranslator gatherTaskApiExceptionTranslator;

    /**
     * 执行职业动作命令。
     *
     * @param request 命令请求
     * @return 命令响应
     */
    @PostMapping("/command")
    public ResponseData<ProfessionActionCommandResponse> execute(@RequestBody @Valid ProfessionActionCommandRequest request) {
        Long accountId = AuthRequired.requireAccountId();
        ProfessionActionCommandType commandType = parseCommandType(request.getCommandType());

        try {
            ProfessionActionCommandResult result = professionActionCommandAppService.execute(
                    professionActionCommandControllerAssembler.toCommand(accountId, request, commandType)
            );
            ProfessionActionCommandResponse response = professionActionCommandControllerAssembler.toResponse(result);
            return ResponseData.success(response);
        } catch (RuntimeException exception) {
            throw gatherTaskApiExceptionTranslator.translate(exception);
        }
    }

    /**
     * 解析命令类型。
     *
     * @param commandTypeCode 命令类型编码
     * @return 命令类型
     */
    private ProfessionActionCommandType parseCommandType(String commandTypeCode) {
        ProfessionActionCommandType commandType = ProfessionActionCommandType.fromCode(commandTypeCode);
        if (commandType == null) {
            throw new IllegalArgumentException("commandType 只能是 START_NOW / ENQUEUE / STOP / REFRESH");
        }
        return commandType;
    }
}