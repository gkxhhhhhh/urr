package com.urr.domain.wallet;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_urr_wallet")
public class WalletEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;
    private Integer serverId;
    private String currencyCode;
    private Long balance;

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
