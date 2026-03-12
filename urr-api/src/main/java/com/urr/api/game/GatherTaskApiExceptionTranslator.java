package com.urr.api.game;

import com.urr.commons.exception.BizException;
import com.urr.commons.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 采集接口异常翻译器。
 *
 * 说明：
 * 1. 这里只做 very small 的 Controller 层错误收口。
 * 2. 不重写全局异常体系，不影响其他模块。
 * 3. 已知的业务失败翻译成 BizException，继续复用现有 GlobalExceptionHandler。
 */
@Component
public class GatherTaskApiExceptionTranslator {

    /**
     * 把运行时异常翻译成接口层更稳定的业务异常。
     *
     * @param exception 原始异常
     * @return 翻译后的异常
     */
    public RuntimeException translate(RuntimeException exception) {
        if (exception instanceof BizException) {
            return exception;
        }

        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            return exception;
        }

        if (isNotFound(message)) {
            return new BizException(ErrorCode.NOT_FOUND, message);
        }
        if (isConflict(message)) {
            return new BizException(ErrorCode.CONFLICT, message);
        }
        if (exception instanceof IllegalArgumentException) {
            return new BizException(ErrorCode.PARAM_INVALID, message);
        }
        if (exception instanceof IllegalStateException && isInternalState(message)) {
            return new BizException(ErrorCode.INTERNAL_ERROR, "系统异常");
        }

        return exception;
    }

    /**
     * 判断是否属于“资源不存在”错误。
     *
     * @param message 异常消息
     * @return true-属于，false-不属于
     */
    private boolean isNotFound(String message) {
        return contains(message, "角色不存在")
                || contains(message, "动作不存在");
    }

    /**
     * 判断是否属于“当前状态冲突”错误。
     *
     * @param message 异常消息
     * @return true-属于，false-不属于
     */
    private boolean isConflict(String message) {
        return contains(message, "当前角色排队数量已达到上限")
                || contains(message, "角色等级不足")
                || contains(message, "体力不足")
                || contains(message, "当前动作不是可持续采集动作")
                || contains(message, "当前动作不是采集动作")
                || contains(message, "当前无运行中的采集任务可停止")
                || contains(message, "当前运行中的任务不是采集任务，无法停止")
                || contains(message, "当前采集任务不是运行中状态");
    }

    /**
     * 判断是否属于需要对外隐藏细节的内部状态错误。
     *
     * @param message 异常消息
     * @return true-属于，false-不属于
     */
    private boolean isInternalState(String message) {
        return contains(message, "更新玩家最近交互时间失败")
                || contains(message, "保存动作队列失败")
                || contains(message, "替换旧任务失败")
                || contains(message, "停止采集任务后重新读取任务失败");
    }

    /**
     * 判断消息中是否包含指定关键字。
     *
     * @param message 原始消息
     * @param keyword 关键字
     * @return true-包含，false-不包含
     */
    private boolean contains(String message, String keyword) {
        if (!StringUtils.hasText(message) || !StringUtils.hasText(keyword)) {
            return false;
        }
        return message.contains(keyword);
    }
}