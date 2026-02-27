package com.urr.api.player;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.urr.api.auth.AuthContext;
import com.urr.api.auth.AuthRequired;
import com.urr.api.auth.dto.RenamePlayerReq;
import com.urr.api.player.dto.CreatePlayerReq;
import com.urr.api.player.dto.UpdateNicknameReq;
import com.urr.app.player.PlayerAppService;
import com.urr.commons.api.ResponseData;
import com.urr.domain.player.PlayerEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@Validated
public class PlayerController {

    private final PlayerAppService playerAppService;

    @GetMapping("/me")
    public ResponseData<List<PlayerEntity>> me(@RequestParam(defaultValue = "1") Integer serverId) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(playerAppService.getMyPlayer(accountId, serverId));
    }

    /** 创建角色 */
    @PostMapping("/createRole")
    public ResponseData<PlayerEntity> create(@RequestBody @Valid CreatePlayerReq req) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(playerAppService.createMyPlayer(accountId, req.getServerId(), req.getNickname(), req.getAvatar(),req.getType()));
    }

    /** 按ID查询 */
    @GetMapping("/{id}")
    public ResponseData<PlayerEntity> get(@PathVariable Long id) {
        return ResponseData.success(playerAppService.getByIdOrThrow(id));
    }

    /** 分页查询 */
    @GetMapping
    public ResponseData<IPage<PlayerEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 @RequestParam(required = false) Long accountId,
                                                 @RequestParam(required = false) String nicknameLike) {
        return ResponseData.success(playerAppService.pagePlayers(pageNum, pageSize, accountId, nicknameLike));
    }

    /** 修改昵称 */
    @PatchMapping("/{playerId}/nickname")
    public void rename(@PathVariable Long playerId,
                       @RequestParam(defaultValue="1") Integer serverId,
                       @RequestBody @Valid RenamePlayerReq req) {
        Long accountId = AuthRequired.requireAccountId();
        playerAppService.renameMyPlayer(accountId, playerId, serverId, req.getNickname());
    }
}
