package com.urr.app.battle;

import com.urr.domain.battle.dto.BattleStartResultDTO;

public interface BattleAppService {

    BattleStartResultDTO startBattle(Long accountId, Long playerId, String actionCode);
}
