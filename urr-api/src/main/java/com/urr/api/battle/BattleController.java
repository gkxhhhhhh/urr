package com.urr.api.battle;

import com.urr.api.auth.AuthRequired;
import com.urr.api.battle.dto.BattleStartReq;
import com.urr.app.battle.BattleAppService;
import com.urr.commons.api.ResponseData;
import com.urr.domain.battle.dto.BattleStartResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/battle")
@RequiredArgsConstructor
@Validated
public class BattleController {

    private final BattleAppService battleAppService;

    @PostMapping("/start")
    public ResponseData<BattleStartResultDTO> start(@RequestBody @Valid BattleStartReq req) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(battleAppService.startBattle(accountId, req.getPlayerId(), req.getActionCode()));
    }
}
