package com.urr.app.game.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.urr.app.action.task.GatherTaskReadService;
import com.urr.app.action.task.query.QueryGatherTaskPanelQuery;
import com.urr.app.action.task.result.QueryGatherTaskPanelResult;
import com.urr.app.game.ActionTreeAppService;
import com.urr.app.game.HomeInitSnapshotAppService;
import com.urr.app.game.result.HomeInitSnapshotResult;
import com.urr.domain.action.task.PlayerActionTaskEntity;
import com.urr.domain.player.PlayerEntity;
import com.urr.domain.skill.PlayerSkillEntity;
import com.urr.domain.skill.SkillDefEntity;
import com.urr.infra.mapper.PlayerActionTaskMapper;
import com.urr.infra.mapper.PlayerMapper;
import com.urr.infra.mapper.PlayerSkillMapper;
import com.urr.infra.mapper.SkillDefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 首页初始化快照应用服务实现。
 *
 * 说明：
 * 1. 聚合当前首页真实依赖的导航、技能、任务面板、角色基础信息。
 * 2. 不扩散到动作写接口、市场接口、战斗接口。
 * 3. 优先复用现有 action-tree 和 gather panel 读能力，避免重复造查询。
 */
@Service
@RequiredArgsConstructor
public class HomeInitSnapshotAppServiceImpl implements HomeInitSnapshotAppService {

    /**
     * 采集任务类型。
     */
    private static final String TASK_TYPE_GATHER = "GATHER";

    /**
     * 运行中状态。
     */
    private static final String STATUS_RUNNING = "RUNNING";

    /**
     * 玩家 Mapper。
     */
    private final PlayerMapper playerMapper;

    /**
     * 玩家技能 Mapper。
     */
    private final PlayerSkillMapper playerSkillMapper;

    /**
     * 技能定义 Mapper。
     */
    private final SkillDefMapper skillDefMapper;

    /**
     * 动作任务根表 Mapper。
     */
    private final PlayerActionTaskMapper playerActionTaskMapper;

    /**
     * 动作树应用服务。
     */
    private final ActionTreeAppService actionTreeAppService;

    /**
     * 采集任务读服务。
     */
    private final GatherTaskReadService gatherTaskReadService;

    /**
     * 加载首页初始化快照。
     *
     * @param accountId 账号ID
     * @param playerId 角色ID
     * @return 首页初始化快照
     */
    @Override
    public HomeInitSnapshotResult loadSnapshot(Long accountId, Long playerId) {
        if (accountId == null) {
            throw new IllegalArgumentException("未登录");
        }
        if (playerId == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }

        PlayerEntity player = requirePlayer(accountId, playerId);

        HomeInitSnapshotResult result = new HomeInitSnapshotResult();
        result.setPlayer(buildPlayerSummary(player));
        result.setActiveRoleCount(countActiveRole(player.getAccountId(), player.getServerId()));
        result.setLeftNav(actionTreeAppService.getActionTree(accountId, playerId));
        result.setSkills(loadSkillSummaryList(playerId));
        result.setPanel(loadPanel(accountId, playerId));
        return result;
    }

    /**
     * 查询并校验角色归属。
     *
     * @param accountId 账号ID
     * @param playerId 角色ID
     * @return 角色实体
     */
    private PlayerEntity requirePlayer(Long accountId, Long playerId) {
        PlayerEntity player = playerMapper.selectById(playerId);
        if (player == null || isDeleted(player.getDeleteFlag())) {
            throw new IllegalArgumentException("角色不存在");
        }
        if (!Objects.equals(accountId, player.getAccountId())) {
            throw new IllegalArgumentException("无权限读取该角色");
        }
        return player;
    }

    /**
     * 构建角色基础摘要。
     *
     * @param player 角色实体
     * @return 角色基础摘要
     */
    private HomeInitSnapshotResult.PlayerSummary buildPlayerSummary(PlayerEntity player) {
        HomeInitSnapshotResult.PlayerSummary summary = new HomeInitSnapshotResult.PlayerSummary();
        summary.setPlayerId(player.getId());
        summary.setServerId(player.getServerId());
        summary.setType(player.getType());
        summary.setNickname(player.getNickname());
        summary.setAvatar(player.getAvatar());
        summary.setLevel(defaultInt(player.getLevel(), 1));
        summary.setExp(defaultLong(player.getExp(), 0L));
        summary.setPower(defaultLong(player.getPower(), 0L));
        summary.setEnergy(defaultInt(player.getEnergy(), 0));
        summary.setEnergyUpdateTime(player.getEnergyUpdateTime());
        summary.setLastOnlineTime(player.getLastOnlineTime());
        summary.setLastInteractTime(player.getLastInteractTime());
        summary.setLastSettleTime(player.getLastSettleTime());
        return summary;
    }

    /**
     * 加载技能摘要列表。
     *
     * 规则：
     * 1. 先读技能定义，保证返回顺序稳定。
     * 2. 再叠加玩家技能等级。
     * 3. 当前仓库没有现成“技能进度百分比换算规则”，本轮 progress 先稳定返回 0。
     *
     * @param playerId 角色ID
     * @return 技能摘要列表
     */
    private List<HomeInitSnapshotResult.SkillSummary> loadSkillSummaryList(Long playerId) {
        List<SkillDefEntity> skillDefList = skillDefMapper.selectList(
                new LambdaQueryWrapper<SkillDefEntity>()
                        .eq(SkillDefEntity::getDeleteFlag, 0)
                        .orderByAsc(SkillDefEntity::getSkillType)
                        .orderByAsc(SkillDefEntity::getId)
        );

        Map<Long, PlayerSkillEntity> playerSkillMap = loadPlayerSkillMap(playerId);
        List<HomeInitSnapshotResult.SkillSummary> resultList = new ArrayList<HomeInitSnapshotResult.SkillSummary>();

        for (int i = 0; i < skillDefList.size(); i++) {
            SkillDefEntity skillDef = skillDefList.get(i);
            PlayerSkillEntity playerSkill = playerSkillMap.get(skillDef.getId());
            resultList.add(buildSkillSummary(skillDef, playerSkill));
        }
        return resultList;
    }

    /**
     * 按技能ID加载玩家技能映射。
     *
     * @param playerId 角色ID
     * @return skillId -> 玩家技能实体
     */
    private Map<Long, PlayerSkillEntity> loadPlayerSkillMap(Long playerId) {
        List<PlayerSkillEntity> playerSkillList = playerSkillMapper.selectList(
                new LambdaQueryWrapper<PlayerSkillEntity>()
                        .eq(PlayerSkillEntity::getPlayerId, playerId)
                        .eq(PlayerSkillEntity::getDeleteFlag, 0)
                        .orderByAsc(PlayerSkillEntity::getId)
        );

        Map<Long, PlayerSkillEntity> resultMap = new LinkedHashMap<Long, PlayerSkillEntity>();
        for (int i = 0; i < playerSkillList.size(); i++) {
            PlayerSkillEntity playerSkill = playerSkillList.get(i);
            if (playerSkill.getSkillId() == null) {
                continue;
            }
            resultMap.put(playerSkill.getSkillId(), playerSkill);
        }
        return resultMap;
    }

    /**
     * 构建单个技能摘要。
     *
     * @param skillDef 技能定义
     * @param playerSkill 玩家技能
     * @return 技能摘要
     */
    private HomeInitSnapshotResult.SkillSummary buildSkillSummary(SkillDefEntity skillDef, PlayerSkillEntity playerSkill) {
        HomeInitSnapshotResult.SkillSummary summary = new HomeInitSnapshotResult.SkillSummary();
        summary.setCode(skillDef.getSkillCode());
        summary.setName(skillDef.getNameZh());
        summary.setType(resolveSkillType(skillDef.getSkillType()));
        summary.setLevel(resolveSkillLevel(playerSkill));
        summary.setProgress(resolveSkillProgress(playerSkill));
        summary.setExtraLevel(0);
        return summary;
    }

    /**
     * 读取首页任务面板。
     *
     * @param accountId 账号ID
     * @param playerId 角色ID
     * @return 首页任务面板
     */
    private QueryGatherTaskPanelResult loadPanel(Long accountId, Long playerId) {
        QueryGatherTaskPanelQuery query = new QueryGatherTaskPanelQuery();
        query.setAccountId(accountId);
        query.setPlayerId(playerId);
        query.setReadTime(null);

        QueryGatherTaskPanelResult result = gatherTaskReadService.queryPanel(query);
        if (result != null) {
            return result;
        }
        return QueryGatherTaskPanelResult.createEmpty(playerId, LocalDateTime.now());
    }

    /**
     * 统计当前区服活跃角色数。
     *
     * 说明：
     * 1. 当前前端顶部只需要“活跃角色数量”，不需要整份角色运行态明细。
     * 2. 当前前端本质上也是按角色逐个判断是否存在运行中的任务。
     * 3. 当前单账号单区服最多 4 个角色，逐角色判断更直接，也更容易维护。
     *
     * @param accountId 账号ID
     * @param serverId 区服ID
     * @return 活跃角色数
     */
    private Integer countActiveRole(Long accountId, Integer serverId) {
        List<PlayerEntity> roleList = playerMapper.selectList(
                new LambdaQueryWrapper<PlayerEntity>()
                        .eq(PlayerEntity::getAccountId, accountId)
                        .eq(PlayerEntity::getServerId, serverId)
                        .eq(PlayerEntity::getDeleteFlag, 0)
                        .orderByAsc(PlayerEntity::getId)
        );

        int activeCount = 0;
        for (int i = 0; i < roleList.size(); i++) {
            PlayerEntity role = roleList.get(i);
            if (role == null || role.getId() == null) {
                continue;
            }
            if (hasRunningGatherTask(role.getId())) {
                activeCount += 1;
            }
        }
        return activeCount;
    }

    /**
     * 判断角色是否存在运行中的采集任务。
     *
     * @param playerId 角色ID
     * @return true-存在运行中采集任务，false-不存在
     */
    private boolean hasRunningGatherTask(Long playerId) {
        Long count = playerActionTaskMapper.selectCount(
                new LambdaQueryWrapper<PlayerActionTaskEntity>()
                        .eq(PlayerActionTaskEntity::getPlayerId, playerId)
                        .eq(PlayerActionTaskEntity::getTaskType, TASK_TYPE_GATHER)
                        .eq(PlayerActionTaskEntity::getStatus, STATUS_RUNNING)
                        .eq(PlayerActionTaskEntity::getDeleteFlag, 0)
        );
        return count != null && count.longValue() > 0L;
    }

    /**
     * 解析技能展示类型。
     *
     * @param skillType 技能类型
     * @return 展示类型
     */
    private String resolveSkillType(Integer skillType) {
        if (skillType != null && (skillType.intValue() == 3 || skillType.intValue() == 4)) {
            return "attribute";
        }
        return "profession";
    }

    /**
     * 解析技能等级。
     *
     * 规则：
     * 1. 与当前动作树解锁逻辑保持一致。
     * 2. 玩家没有技能记录时，默认按 1 级返回。
     *
     * @param playerSkill 玩家技能
     * @return 技能等级
     */
    private Integer resolveSkillLevel(PlayerSkillEntity playerSkill) {
        if (playerSkill == null || playerSkill.getSkillLevel() == null) {
            return 1;
        }
        return playerSkill.getSkillLevel();
    }

    /**
     * 解析技能进度。
     *
     * 说明：
     * 当前仓库还没有统一的“skillExp -> progress 百分比”规则服务，
     * 本轮先返回 0，避免后端在没有业务依据时自行编造进度算法。
     *
     * @param playerSkill 玩家技能
     * @return 技能进度
     */
    private Integer resolveSkillProgress(PlayerSkillEntity playerSkill) {
        return 0;
    }

    /**
     * 判断逻辑删除标记。
     *
     * @param deleteFlag 删除标记
     * @return true-已删除，false-未删除
     */
    private boolean isDeleted(Integer deleteFlag) {
        return deleteFlag != null && deleteFlag.intValue() == 1;
    }

    /**
     * 兜底 int 值。
     *
     * @param value 原值
     * @param defaultValue 默认值
     * @return 结果
     */
    private int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value.intValue();
    }

    /**
     * 兜底 long 值。
     *
     * @param value 原值
     * @param defaultValue 默认值
     * @return 结果
     */
    private long defaultLong(Long value, long defaultValue) {
        return value == null ? defaultValue : value.longValue();
    }
}