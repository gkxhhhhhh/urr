package com.urr.app.game;

import com.urr.domain.action.dto.ActionTreeResponseDTO;

public interface ActionTreeAppService {

    ActionTreeResponseDTO getActionTree(Long accountId, Long playerId);
}