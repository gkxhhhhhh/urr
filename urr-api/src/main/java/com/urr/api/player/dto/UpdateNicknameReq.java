package com.urr.api.player.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class UpdateNicknameReq {
    @NotBlank
    @Size(min = 2, max = 20)
    private String nickname;
}
