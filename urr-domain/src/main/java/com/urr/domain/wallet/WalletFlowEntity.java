package com.urr.domain.wallet;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_urr_wallet_flow")
public class WalletFlowEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;
    private Integer serverId;
    private String currencyCode;
    private Long delta;
    private Long balanceAfter;
    private String reason;
    private String refType;
    private Long refId;
    private String requestId;
    private LocalDateTime flowTime;
    private String remarks;
    private String createUser;
    private LocalDateTime createTime;
    private String updateUser;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleteFlag;
}
