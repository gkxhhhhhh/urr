package com.urr.app.player;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.urr.domain.player.PlayerEntity;

import java.util.List;

public interface PlayerAppService {

    PlayerEntity createPlayer(Long accountId, String nickname);

    PlayerEntity getByIdOrThrow(Long playerId);

    IPage<PlayerEntity> pagePlayers(int pageNum, int pageSize, Long accountId, String nicknameLike);

    void updateNickname(Long playerId, String nickname);

    List<PlayerEntity> getMyPlayer(Long accountId, Integer serverId);

    PlayerEntity createMyPlayer(Long accountId, Integer serverId, String nickname, String avatar,String type);

    void renameMyPlayer(Long accountId, Long playerId, Integer serverId, String nickname);
}
