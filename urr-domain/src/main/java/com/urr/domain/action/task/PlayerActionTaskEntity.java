package com.urr.domain.action.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动作任务根实体，映射现有 t_urr_player_activity。
 *
 * 说明：
 * 1. 这次复用 t_urr_player_activity 作为“单角色当前动作槽位”。
 * 2. 原有 activity 体系字段继续保留，避免对旧语义做大改。
 * 3. 新增 task_type / status / stop_reason 等字段后，这张表承担动作任务根信息。
 */
@Data
@TableName("t_urr_player_activity")
public class PlayerActionTaskEntity {

    /**
     * 主键ID。
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
     * 行为ID。
     * 兼容旧 activity 模型保留。
     */
    private Long behaviorId;

    /**
     * 动作ID。
     * 兼容旧 activity 模型保留。
     */
    private Long actionId;

    /**
     * 动作编码。
     * 对应 t_urr_action_def.action_code。
     */
    private String actionCode;

    /**
     * 类型ID。
     * 兼容旧 activity 模型保留。
     */
    private Long categoryId;

    /**
     * 子类型ID。
     * 兼容旧 activity 模型保留。
     */
    private Long subCategoryId;

    /**
     * 旧活动类型字段。
     * 当前先兼容保留。
     */
    private Integer activityType;

    /**
     * 旧目标ID字段。
     * 当前先兼容保留。
     */
    private Long targetId;

    /**
     * 旧状态字段。
     * 当前先兼容保留。
     */
    private Integer state;

    /**
     * 任务业务类型。
     * 例如：GATHER / BATTLE / CRAFT。
     */
    private String taskType;

    /**
     * 任务状态。
     * 例如：QUEUED / RUNNING / COMPLETED。
     */
    private String status;

    /**
     * 任务停止原因。
     */
    private String stopReason;

    /**
     * 任务开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 本次任务锁定的最近交互时间。
     */
    private LocalDateTime lastInteractTime;

    /**
     * 旧的上次计算时间字段。
     * 当前可与 lastSettleTime 保持同步。
     */
    private LocalDateTime lastCalcTime;

    /**
     * 最近一次任务层结算时间。
     */
    private LocalDateTime lastSettleTime;

    /**
     * 本次任务锁定的离线截止时间。
     */
    private LocalDateTime offlineExpireAt;

    /**
     * 奖励随机种子。
     */
    private Long rewardSeed;

    /**
     * 兼容旧扩展参数字段。
     */
    private String paramJson;

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