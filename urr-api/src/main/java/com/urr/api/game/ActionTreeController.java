package com.urr.api.game;

import com.urr.api.auth.AuthRequired;
import com.urr.app.game.ActionTreeAppService;
import com.urr.commons.api.ResponseData;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
@Validated
public class ActionTreeController {

    private final ActionTreeAppService actionTreeAppService;

    @GetMapping("/action-tree")
    public ResponseData actionTree(@RequestParam("playerId") Long playerId) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(actionTreeAppService.getActionTree(accountId, playerId));
    }
}