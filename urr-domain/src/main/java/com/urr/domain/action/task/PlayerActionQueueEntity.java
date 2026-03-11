package com.urr.domain.action.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 玩家动作队列实体，映射 t_urr_player_action_queue。
 *
 * 说明：
 * 1. 这里记录“尚未真正启动”的排队请求。
 * 2. 当前会话只先支持采集入队，但队列维度按角色通用设计。
 * 3. 后续队列自动消费时，可以直接复用这里的记录。
 */
@Data
@TableName("t_urr_player_action_queue")
public class PlayerActionQueueEntity {

    /**
     * 队列记录ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 玩家ID。
     */
    private Long playerId;

    /**
     * 区服ID。
     */
    private Integer serverId;

    /**
     * 任务业务类型。
     * 当前先写 GATHER。
     */
    private String taskType;

    /**
     * 动作编码。
     */
    private String actionCode;

    /**
     * 目标轮次。
     * 当前主要给采集任务使用，-1 表示无限次数。
     */
    private Long targetCount;

    /**
     * 队列状态。
     * 当前会话只先用 QUEUED。
     */
    private String status;

    /**
     * 乐观锁版本号。
     */
    @Version
    private Integer version;

    /**
     * 备注。
     */
    private String remarks;

    /**
     * 创建人。
     */
    private String createUser;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 修改人。
     */
    private String updateUser;

    /**
     * 修改时间。
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer deleteFlag;
}