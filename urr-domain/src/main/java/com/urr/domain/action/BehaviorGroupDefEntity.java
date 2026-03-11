package com.urr.domain.action;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_urr_behavior_group_def")
public class BehaviorGroupDefEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupCode;
    private String groupName;
    private Integer status;
    private String paramsJson;

    @Version
    private Integer version;

    private String remarks;
    private String createUser;
    private LocalDateTime createTime;
    private String updateUser;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleteFlag;
}