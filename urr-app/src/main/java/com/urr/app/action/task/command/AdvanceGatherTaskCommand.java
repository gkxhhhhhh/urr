package com.urr.app.action.task.command;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推进采集任务命令。
 */
@Data
public class AdvanceGatherTaskCommand {

    /**
     * 任务ID。
     */
    private Long taskId;

    /**
     * 推进到的目标时间。
     */
    private LocalDateTime advanceTime;
}