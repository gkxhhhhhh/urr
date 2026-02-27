package com.urr.api.player.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CreatePlayerReq {
    @NotNull
    private Integer serverId;

    @NotBlank
    @Size(min=2,max=32)
    private String nickname;

    private String avatar;

    /** 角色类型 1：标准  3：铁人*/
    private String type;
}
