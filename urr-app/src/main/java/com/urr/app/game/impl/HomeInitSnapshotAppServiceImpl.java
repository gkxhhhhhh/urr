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
     * 技能经验曲线指数。
     */
    private static final double SKILL_EXP_POWER = 2.2D;

    /**
     * 91 级系数关键点。
     */
    private static final double COEFFICIENT_LEVEL_91 = 218D;

    /**
     * 120 级系数关键点。
     */
    private static final double COEFFICIENT_LEVEL_120 = 1585D;

    /**
     * 141 级系数关键点。
     */
    private static final double COEFFICIENT_LEVEL_141 = 10816D;

    /**
     * 167 级系数关键点。
     */
    private static final double COEFFICIENT_LEVEL_167 = 81346D;

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
     * 2. 再叠加玩家技能等级与经验进度。
     * 3. progress 按当前升级体系公式换算成 0 ~ 100 的整数百分比。
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
        return Math.max(playerSkill.getSkillLevel().intValue(), 1);
    }

    /**
     * 解析技能进度。
     *
     * 规则：
     * 1. 直接使用 t_urr_player_skill.skillExp 作为累计经验读取。
     * 2. 当前等级从“本级起点经验”推进到“下一级目标经验”时，返回 0 ~ 100 的整数百分比。
     * 3. 1 / 2 级系数都按 4 处理，和本轮升级公式保持一致。
     *
     * @param playerSkill 玩家技能
     * @return 技能进度
     */
    private Integer resolveSkillProgress(PlayerSkillEntity playerSkill) {
        if (playerSkill == null) {
            return 0;
        }

        int currentLevel = resolveSkillLevel(playerSkill);
        long currentExp = resolveSkillExp(playerSkill);
        long currentLevelStartExp = resolveCurrentLevelStartExp(currentLevel);
        long nextLevelTotalExp = calculateTotalExp(currentLevel + 1);
        long neededExp = nextLevelTotalExp - currentLevelStartExp;
        if (neededExp <= 0L) {
            return 0;
        }

        long progressedExp = currentExp - currentLevelStartExp;
        if (progressedExp <= 0L) {
            return 0;
        }
        if (progressedExp >= neededExp) {
            return 100;
        }

        long progress = Math.round(progressedExp * 100D / neededExp);
        return (int) clampLong(progress, 0L, 100L);
    }

    /**
     * 读取技能累计经验。
     *
     * @param playerSkill 玩家技能
     * @return 累计经验
     */
    private long resolveSkillExp(PlayerSkillEntity playerSkill) {
        if (playerSkill == null || playerSkill.getSkillExp() == null) {
            return 0L;
        }
        return Math.max(playerSkill.getSkillExp().longValue(), 0L);
    }

    /**
     * 解析当前等级的起点累计经验。
     *
     * 说明：
     * 1. 1 级视为初始等级，起点经验按 0 处理。
     * 2. 2 级及以上使用 E_total(L) 作为本级起点，表示“已经到达当前等级”的累计门槛。
     *
     * @param currentLevel 当前等级
     * @return 本级起点累计经验
     */
    private long resolveCurrentLevelStartExp(int currentLevel) {
        if (currentLevel <= 1) {
            return 0L;
        }
        return calculateTotalExp(currentLevel);
    }

    /**
     * 计算某个等级对应的累计经验总量。
     *
     * 公式：
     * E_total(L) = Round(A(L) × L^2.2)
     *
     * @param level 等级
     * @return 累计经验
     */
    private long calculateTotalExp(int level) {
        int safeLevel = Math.max(level, 1);
        double coefficient = resolveCoefficient(safeLevel);
        double totalExp = coefficient * Math.pow(safeLevel, SKILL_EXP_POWER);
        return Math.max(Math.round(totalExp), 0L);
    }

    /**
     * 解析指定等级的系数 A(L)。
     *
     * 说明：
     * 1. 1 / 2 级沿用常数 4。
     * 2. 线性区间按端点插值。
     * 3. 指数区间按端点做指数插值，保证曲线连续可读。
     *
     * @param level 等级
     * @return 系数
     */
    private double resolveCoefficient(int level) {
        if (level <= 20) {
            return 4D;
        }
        if (level <= 23) {
            return interpolateLinear(level, 20, 4D, 23, 5D);
        }
        if (level <= 67) {
            return interpolateLinear(level, 24, 5D, 67, 48D);
        }
        if (level <= 74) {
            return interpolateLinear(level, 68, 50D, 74, 74D);
        }
        if (level <= 90) {
            return interpolateQuadratic(level, 75, 78D, 90, 210D, 91, COEFFICIENT_LEVEL_91);
        }
        if (level == 91) {
            return COEFFICIENT_LEVEL_91;
        }
        if (level <= 119) {
            return interpolateExponential(level, 92, 230D, 119, 1460D);
        }
        if (level == 120) {
            return COEFFICIENT_LEVEL_120;
        }
        if (level <= 140) {
            return interpolateExponential(level, 121, 1720D, 140, 9300D);
        }
        if (level == 141) {
            return COEFFICIENT_LEVEL_141;
        }
        if (level <= 166) {
            return interpolateExponential(level, 142, 11700D, 166, 75000D);
        }
        if (level == 167) {
            return COEFFICIENT_LEVEL_167;
        }
        return COEFFICIENT_LEVEL_167 * Math.pow(1.08D, level - 167D);
    }

    /**
     * 线性插值。
     *
     * @param level 当前等级
     * @param startLevel 起始等级
     * @param startValue 起始系数
     * @param endLevel 结束等级
     * @param endValue 结束系数
     * @return 插值结果
     */
    private double interpolateLinear(int level, int startLevel, double startValue, int endLevel, double endValue) {
        if (endLevel <= startLevel) {
            return startValue;
        }
        double ratio = (level - startLevel) * 1D / (endLevel - startLevel);
        return startValue + (endValue - startValue) * ratio;
    }

    /**
     * 指数插值。
     *
     * @param level 当前等级
     * @param startLevel 起始等级
     * @param startValue 起始系数
     * @param endLevel 结束等级
     * @param endValue 结束系数
     * @return 插值结果
     */
    private double interpolateExponential(int level, int startLevel, double startValue, int endLevel, double endValue) {
        if (endLevel <= startLevel) {
            return startValue;
        }
        if (startValue <= 0D || endValue <= 0D) {
            return startValue;
        }
        double ratio = (level - startLevel) * 1D / (endLevel - startLevel);
        return startValue * Math.pow(endValue / startValue, ratio);
    }

    /**
     * 二次曲线插值。
     *
     * @param level 当前等级
     * @param x1 第一个锚点等级
     * @param y1 第一个锚点系数
     * @param x2 第二个锚点等级
     * @param y2 第二个锚点系数
     * @param x3 第三个锚点等级
     * @param y3 第三个锚点系数
     * @return 插值结果
     */
    private double interpolateQuadratic(int level, int x1, double y1, int x2, double y2, int x3, double y3) {
        double current = level;
        double l1 = ((current - x2) * (current - x3)) / ((x1 - x2) * 1D * (x1 - x3));
        double l2 = ((current - x1) * (current - x3)) / ((x2 - x1) * 1D * (x2 - x3));
        double l3 = ((current - x1) * (current - x2)) / ((x3 - x1) * 1D * (x3 - x2));
        return y1 * l1 + y2 * l2 + y3 * l3;
    }

    /**
     * 夹取 long 范围。
     *
     * @param value 原值
     * @param min 最小值
     * @param max 最大值
     * @return 结果
     */
    private long clampLong(long value, long min, long max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
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
