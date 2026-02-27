package com.urr.domain.account;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_urr_account")
public class AccountEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 账号(唯一) */
    private String account;

    /** 密码hash（建议BCrypt） */
    private String passwordHash;

    /** 盐(可选)，BCrypt可不填 */
    private String salt;

    /** 状态 1正常 2封禁 */
    private Integer status;

    private String registerIp;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;

    private String remarks;

    private String createUser;
    private LocalDateTime createTime;

    private String updateUser;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleteFlag;
}