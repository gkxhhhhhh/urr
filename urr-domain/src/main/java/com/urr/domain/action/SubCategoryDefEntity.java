package com.urr.domain.action;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_urr_sub_category_def")
public class SubCategoryDefEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;
    private String subCode;
    private String subName;
    private Long parentId;
    private Integer sort;
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