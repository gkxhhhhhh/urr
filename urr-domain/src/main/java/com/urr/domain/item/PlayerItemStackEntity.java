package com.urr.domain.item;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_urr_player_item_stack")
public class PlayerItemStackEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;
    private Integer serverId;
    private Long itemId;
    private Long qty;
    private Integer location;

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
