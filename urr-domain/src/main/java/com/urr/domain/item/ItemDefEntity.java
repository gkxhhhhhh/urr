package com.urr.domain.item;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_urr_item_def")
public class ItemDefEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String itemCode;
    private String nameZh;
    private String nameEn;
    private Integer itemType;
    private Integer rarity;
    private Integer stackable;
    private Integer maxStack;
    private Integer bindType;
    private Integer tradeable;
    private Long sellPrice;
    private String metaJson;
    private String remarks;
    private String createUser;
    private LocalDateTime createTime;
    private String updateUser;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleteFlag;
}
