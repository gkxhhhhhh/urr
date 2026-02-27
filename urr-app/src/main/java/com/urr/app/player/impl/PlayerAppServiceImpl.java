package com.urr.app.player.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.urr.app.player.PlayerAppService;
import com.urr.commons.exception.BizException;
import com.urr.commons.exception.ErrorCode;
import com.urr.domain.player.PlayerEntity;
import com.urr.infra.mapper.PlayerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerAppServiceImpl implements PlayerAppService {

    private final PlayerMapper playerMapper;

    @Override
    @Transactional
    public PlayerEntity createPlayer(Long accountId, String nickname) {
        // 规则：同账号下昵称不重复（你想改成全服唯一时，把 accountId 条件删掉即可）
        Long cnt = playerMapper.selectCount(new LambdaQueryWrapper<PlayerEntity>()
                .eq(PlayerEntity::getAccountId, accountId)
                .eq(PlayerEntity::getNickname, nickname));
        if (cnt != null && cnt > 0) {
            throw new BizException(ErrorCode.CONFLICT, "昵称已存在");
        }

        PlayerEntity p = new PlayerEntity();
        p.setAccountId(accountId);
        p.setNickname(nickname);
        p.setLevel(1);
        p.setExp(0L);

        playerMapper.insert(p);
        return p;
    }

    @Override
    public PlayerEntity getByIdOrThrow(Long playerId) {
        PlayerEntity p = playerMapper.selectById(playerId);
        if (p == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return p;
    }

    @Override
    public IPage<PlayerEntity> pagePlayers(int pageNum, int pageSize, Long accountId, String nicknameLike) {
        LambdaQueryWrapper<PlayerEntity> qw = new LambdaQueryWrapper<PlayerEntity>()
                .orderByDesc(PlayerEntity::getId);

        if (accountId != null) {
            qw.eq(PlayerEntity::getAccountId, accountId);
        }
        if (StringUtils.hasText(nicknameLike)) {
            qw.like(PlayerEntity::getNickname, nicknameLike);
        }

        Page<PlayerEntity> page = new Page<>(pageNum, pageSize);
        return playerMapper.selectPage(page, qw);
    }

    @Override
    @Transactional
    public void updateNickname(Long playerId, String nickname) {
        PlayerEntity p = getByIdOrThrow(playerId);
        p.setNickname(nickname);
        int updated = playerMapper.updateById(p);
        if (updated != 1) {
            throw new BizException(ErrorCode.CONFLICT, "更新失败（可能存在并发修改）");
        }
    }

    @Override
    public List<PlayerEntity> getMyPlayer(Long accountId, Integer serverId) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId不能为空");
        }
        if (serverId == null) {
            throw new IllegalArgumentException("serverId不能为空");
        }

        return playerMapper.selectList(new LambdaQueryWrapper<PlayerEntity>()
                .eq(PlayerEntity::getAccountId, accountId)
                .eq(PlayerEntity::getServerId, serverId));
    }

    @Override
    @Transactional
    public PlayerEntity createMyPlayer(Long accountId, Integer serverId, String nickname, String avatar,String type) {
        if (accountId == null) {
            throw new IllegalArgumentException("未登录");
        }
        if (serverId == null) {
            throw new IllegalArgumentException("serverId不能为空");
        }
        if (!StringUtils.hasText(nickname)) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        nickname = nickname.trim();
        if (nickname.length() < 2 || nickname.length() > 32) {
            throw new IllegalArgumentException("昵称长度需在2~32之间");
        }

        // 1) 同账号同区服：是否已创建角色（建议先限制 1 个，符合“创建角色”流程）
        Long mineCnt = playerMapper.selectCount(new LambdaQueryWrapper<PlayerEntity>()
                .eq(PlayerEntity::getAccountId, accountId)
                .eq(PlayerEntity::getServerId, serverId));
        if (mineCnt != null && mineCnt > 4) {
            throw new IllegalArgumentException("该账号在本区服已创建4个角色");
        }

        // 2) 区服内昵称唯一（匹配 uk_server_nickname）
        Long nameCnt = playerMapper.selectCount(new LambdaQueryWrapper<PlayerEntity>()
                .eq(PlayerEntity::getServerId, serverId)
                .eq(PlayerEntity::getNickname, nickname));
        if (nameCnt != null && nameCnt > 0) {
            throw new IllegalArgumentException("昵称已被占用");
        }

        // 3) 插入：其余字段走 DB 默认值（level/exp/power/energy/last_settle_time/version）
        PlayerEntity e = new PlayerEntity();
        e.setAccountId(accountId);
        e.setServerId(serverId);
        e.setNickname(nickname);
        e.setAvatar(StringUtils.hasText(avatar) ? avatar.trim() : null);
        e.setType(Integer.valueOf(type));
        playerMapper.insert(e);

        // insert 后 MP 会回填自增 id
        return e;
    }


    @Override
    @Transactional
    public void renameMyPlayer(Long accountId, Long playerId, Integer serverId, String newNickname) {
        if (accountId == null) {
            throw new IllegalArgumentException("未登录");
        }
        if (playerId == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
        if (serverId == null) {
            throw new IllegalArgumentException("serverId不能为空");
        }
        if (!StringUtils.hasText(newNickname)) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        newNickname = newNickname.trim();
        if (newNickname.length() < 2 || newNickname.length() > 32) {
            throw new IllegalArgumentException("昵称长度需在2~32之间");
        }

        // 1) 查角色 + 归属校验
        PlayerEntity e = playerMapper.selectById(playerId);
        if (e == null || (e.getDeleteFlag() != null && e.getDeleteFlag() == 1)) {
            throw new IllegalArgumentException("角色不存在");
        }
        if (!accountId.equals(e.getAccountId())) {
            throw new IllegalArgumentException("无权限操作该角色");
        }
        if (!serverId.equals(e.getServerId())) {
            throw new IllegalArgumentException("区服不匹配");
        }

        // 2) 区服内昵称唯一（排除自己）
        Long nameCnt = playerMapper.selectCount(new LambdaQueryWrapper<PlayerEntity>()
                .eq(PlayerEntity::getServerId, serverId)
                .eq(PlayerEntity::getNickname, newNickname)
                .ne(PlayerEntity::getId, playerId));
        if (nameCnt != null && nameCnt > 0) {
            throw new IllegalArgumentException("昵称已被占用");
        }

        // 3) 更新（走 @Version 乐观锁）
        e.setNickname(newNickname);
        int rows = playerMapper.updateById(e);
        if (rows != 1) {
            throw new IllegalStateException("更新失败（可能存在并发修改）");
        }
    }
}
