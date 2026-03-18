package com.urr.app.game.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urr.app.game.ActionTreeAppService;
import com.urr.domain.action.ActionDefEntity;
import com.urr.domain.action.BehaviorDefEntity;
import com.urr.domain.action.BehaviorGroupDefEntity;
import com.urr.domain.action.CategoryDefEntity;
import com.urr.domain.action.SubCategoryDefEntity;
import com.urr.domain.action.dto.ActionTreeResponseDTO;
import com.urr.domain.player.PlayerEntity;
import com.urr.domain.skill.PlayerSkillEntity;
import com.urr.domain.skill.SkillDefEntity;
import com.urr.infra.mapper.ActionDefMapper;
import com.urr.infra.mapper.BehaviorDefMapper;
import com.urr.infra.mapper.BehaviorGroupDefMapper;
import com.urr.infra.mapper.CategoryDefMapper;
import com.urr.infra.mapper.PlayerMapper;
import com.urr.infra.mapper.PlayerSkillMapper;
import com.urr.infra.mapper.SkillDefMapper;
import com.urr.infra.mapper.SubCategoryDefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 动作树应用服务实现。
 *
 * 规则：
 * 1. GROUP / BEHAVIOR / CATEGORY / SUB_CATEGORY / ACTION 按现有树结构组装。
 * 2. ACTION 解锁同时看玩家等级和职业技能等级。
 * 3. 旧动作如果未配置 params_json.skillCode，则继续沿用原来的玩家等级解锁逻辑。
 */
@Service
@RequiredArgsConstructor
public class ActionTreeAppServiceImpl implements ActionTreeAppService {

    /**
     * 组节点类型。
     */
    private static final String NODE_GROUP = "GROUP";

    /**
     * 行为节点类型。
     */
    private static final String NODE_BEHAVIOR = "BEHAVIOR";

    /**
     * 分类节点类型。
     */
    private static final String NODE_CATEGORY = "CATEGORY";

    /**
     * 子分类节点类型。
     */
    private static final String NODE_SUB_CATEGORY = "SUB_CATEGORY";

    /**
     * 动作节点类型。
     */
    private static final String NODE_ACTION = "ACTION";

    /**
     * 战斗特殊类型。
     */
    private static final String SPECIAL_BATTLE = "BATTLE";

    /**
     * 模块动作特殊类型。
     */
    private static final String SPECIAL_MODULE = "MODULE";

    /**
     * 玩家 Mapper。
     */
    private final PlayerMapper playerMapper;

    /**
     * 动作定义 Mapper。
     */
    private final ActionDefMapper actionDefMapper;

    /**
     * 行为域定义 Mapper。
     */
    private final BehaviorGroupDefMapper behaviorGroupDefMapper;

    /**
     * 行为定义 Mapper。
     */
    private final BehaviorDefMapper behaviorDefMapper;

    /**
     * 分类定义 Mapper。
     */
    private final CategoryDefMapper categoryDefMapper;

    /**
     * 子分类定义 Mapper。
     */
    private final SubCategoryDefMapper subCategoryDefMapper;

    /**
     * 技能定义 Mapper。
     */
    private final SkillDefMapper skillDefMapper;

    /**
     * 玩家技能 Mapper。
     */
    private final PlayerSkillMapper playerSkillMapper;

    /**
     * JSON 解析器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 获取玩家动作树。
     *
     * @param accountId 账号ID
     * @param playerId  玩家ID
     * @return 动作树
     */
    @Override
    public ActionTreeResponseDTO getActionTree(Long accountId, Long playerId) {
        if (accountId == null) {
            throw new IllegalArgumentException("未登录");
        }
        if (playerId == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }

        PlayerEntity player = requirePlayer(playerId);
        if (!Objects.equals(accountId, player.getAccountId())) {
            throw new IllegalArgumentException("无权限操作该角色");
        }

        int playerLevel = defaultInt(player.getLevel(), 1);

        List<BehaviorGroupDefEntity> groups = behaviorGroupDefMapper.selectList(
                new LambdaQueryWrapper<BehaviorGroupDefEntity>()
                        .eq(BehaviorGroupDefEntity::getStatus, 1)
                        .eq(BehaviorGroupDefEntity::getDeleteFlag, 0)
                        .orderByAsc(BehaviorGroupDefEntity::getId)
        );

        List<BehaviorDefEntity> behaviors = behaviorDefMapper.selectList(
                new LambdaQueryWrapper<BehaviorDefEntity>()
                        .eq(BehaviorDefEntity::getStatus, 1)
                        .eq(BehaviorDefEntity::getDeleteFlag, 0)
                        .orderByAsc(BehaviorDefEntity::getId)
        );

        List<CategoryDefEntity> categories = categoryDefMapper.selectList(
                new LambdaQueryWrapper<CategoryDefEntity>()
                        .eq(CategoryDefEntity::getStatus, 1)
                        .eq(CategoryDefEntity::getDeleteFlag, 0)
                        .orderByAsc(CategoryDefEntity::getSort)
                        .orderByAsc(CategoryDefEntity::getId)
        );

        List<SubCategoryDefEntity> subCategories = subCategoryDefMapper.selectList(
                new LambdaQueryWrapper<SubCategoryDefEntity>()
                        .eq(SubCategoryDefEntity::getStatus, 1)
                        .eq(SubCategoryDefEntity::getDeleteFlag, 0)
                        .orderByAsc(SubCategoryDefEntity::getSort)
                        .orderByAsc(SubCategoryDefEntity::getId)
        );

        List<ActionDefEntity> actions = actionDefMapper.selectList(
                new LambdaQueryWrapper<ActionDefEntity>()
                        .eq(ActionDefEntity::getStatus, 1)
                        .eq(ActionDefEntity::getDeleteFlag, 0)
                        .orderByAsc(ActionDefEntity::getBehaviorId)
                        .orderByAsc(ActionDefEntity::getCategoryId)
                        .orderByAsc(ActionDefEntity::getSubCategoryId)
                        .orderByAsc(ActionDefEntity::getId)
        );

        Map<String, Long> skillIdByCode = loadSkillIdByCode();
        Map<Long, Integer> playerSkillLevelBySkillId = loadPlayerSkillLevelBySkillId(playerId);

        Map<Long, ActionTreeResponseDTO.TreeNode> groupNodeById = new LinkedHashMap<>();
        Map<Long, ActionTreeResponseDTO.TreeNode> behaviorNodeById = new LinkedHashMap<>();
        Map<Long, ActionTreeResponseDTO.TreeNode> categoryNodeById = new LinkedHashMap<>();
        Map<Long, ActionTreeResponseDTO.TreeNode> subCategoryNodeById = new LinkedHashMap<>();

        /**
         * 1. GROUP
         */
        for (BehaviorGroupDefEntity group : sortGroups(groups)) {
            ActionTreeResponseDTO.TreeNode node = buildGroupNode(group);
            groupNodeById.put(group.getId(), node);
        }

        /**
         * 2. BEHAVIOR -> GROUP
         */
        List<BehaviorDefEntity> sortedBehaviors = new ArrayList<>(behaviors);
        sortedBehaviors.sort(
                Comparator.comparingInt((BehaviorDefEntity x) -> groupOrder(x.getGroupCode()))
                        .thenComparingLong(x -> defaultLong(x.getId(), 0L))
        );

        for (BehaviorDefEntity behavior : sortedBehaviors) {
            ActionTreeResponseDTO.TreeNode parent = findGroupNodeByCode(groupNodeById, behavior.getGroupCode());
            if (parent == null) {
                continue;
            }

            ActionTreeResponseDTO.TreeNode node = buildBehaviorNode(behavior);
            node.setParentNodeKey(parent.getNodeKey());
            parent.getChildren().add(node);
            behaviorNodeById.put(behavior.getId(), node);
        }

        /**
         * 3. CATEGORY -> BEHAVIOR
         */
        for (CategoryDefEntity category : categories) {
            ActionTreeResponseDTO.TreeNode parent = behaviorNodeById.get(category.getBehaviorId());
            if (parent == null) {
                continue;
            }

            ActionTreeResponseDTO.TreeNode node = buildCategoryNode(category);
            node.setParentNodeKey(parent.getNodeKey());
            parent.getChildren().add(node);
            categoryNodeById.put(category.getId(), node);
        }

        /**
         * 4. SUB_CATEGORY 先建节点
         */
        for (SubCategoryDefEntity subCategory : subCategories) {
            ActionTreeResponseDTO.TreeNode node = buildSubCategoryNode(subCategory);
            subCategoryNodeById.put(subCategory.getId(), node);
        }

        /**
         * 5. SUB_CATEGORY 挂到 CATEGORY 或父 SUB_CATEGORY
         */
        for (SubCategoryDefEntity subCategory : subCategories) {
            ActionTreeResponseDTO.TreeNode node = subCategoryNodeById.get(subCategory.getId());
            if (node == null) {
                continue;
            }

            long parentId = defaultLong(subCategory.getParentId(), 0L);
            if (parentId > 0) {
                ActionTreeResponseDTO.TreeNode parent = subCategoryNodeById.get(parentId);
                if (parent != null) {
                    node.setParentNodeKey(parent.getNodeKey());
                    parent.getChildren().add(node);
                }
                continue;
            }

            ActionTreeResponseDTO.TreeNode categoryNode = categoryNodeById.get(subCategory.getCategoryId());
            if (categoryNode != null) {
                node.setParentNodeKey(categoryNode.getNodeKey());
                categoryNode.getChildren().add(node);
            }
        }

        /**
         * 6. ACTION -> SUB_CATEGORY / CATEGORY / BEHAVIOR
         */
        for (ActionDefEntity action : actions) {
            ActionTreeResponseDTO.TreeNode node = buildActionNode(
                    action,
                    playerLevel,
                    skillIdByCode,
                    playerSkillLevelBySkillId
            );

            long subCategoryId = defaultLong(action.getSubCategoryId(), 0L);
            long categoryId = defaultLong(action.getCategoryId(), 0L);

            if (subCategoryId > 0) {
                ActionTreeResponseDTO.TreeNode parent = subCategoryNodeById.get(subCategoryId);
                if (parent != null) {
                    node.setParentNodeKey(parent.getNodeKey());
                    parent.getChildren().add(node);
                }
                continue;
            }

            if (categoryId > 0) {
                ActionTreeResponseDTO.TreeNode parent = categoryNodeById.get(categoryId);
                if (parent != null) {
                    node.setParentNodeKey(parent.getNodeKey());
                    parent.getChildren().add(node);
                }
                continue;
            }

            ActionTreeResponseDTO.TreeNode parent = behaviorNodeById.get(action.getBehaviorId());
            if (parent != null) {
                node.setParentNodeKey(parent.getNodeKey());
                parent.getChildren().add(node);
            }
        }

        List<ActionTreeResponseDTO.TreeNode> roots = new ArrayList<>(groupNodeById.values());
        roots.sort(this::compareNodes);

        for (ActionTreeResponseDTO.TreeNode root : roots) {
            fillDerivedFields(root, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        ActionTreeResponseDTO response = new ActionTreeResponseDTO();
        response.setPlayerId(playerId);
        response.setTreeVersion(1);
        response.setNodes(roots);
        return response;
    }

    /**
     * 校验并获取玩家。
     *
     * @param playerId 玩家ID
     * @return 玩家实体
     */
    private PlayerEntity requirePlayer(Long playerId) {
        PlayerEntity player = playerMapper.selectById(playerId);
        if (player == null || isDeleted(player.getDeleteFlag())) {
            throw new IllegalArgumentException("角色不存在");
        }
        return player;
    }

    /**
     * 按技能编码加载技能ID映射。
     *
     * @return skillCode -> skillId
     */
    private Map<String, Long> loadSkillIdByCode() {
        List<SkillDefEntity> skillDefs = skillDefMapper.selectList(
                new LambdaQueryWrapper<SkillDefEntity>()
                        .eq(SkillDefEntity::getDeleteFlag, 0)
                        .orderByAsc(SkillDefEntity::getId)
        );

        Map<String, Long> result = new LinkedHashMap<>();
        for (SkillDefEntity skillDef : skillDefs) {
            if (!StringUtils.hasText(skillDef.getSkillCode())) {
                continue;
            }
            result.put(skillDef.getSkillCode(), skillDef.getId());
        }
        return result;
    }

    /**
     * 按玩家ID加载玩家技能等级映射。
     *
     * @param playerId 玩家ID
     * @return skillId -> skillLevel
     */
    private Map<Long, Integer> loadPlayerSkillLevelBySkillId(Long playerId) {
        List<PlayerSkillEntity> playerSkills = playerSkillMapper.selectList(
                new LambdaQueryWrapper<PlayerSkillEntity>()
                        .eq(PlayerSkillEntity::getPlayerId, playerId)
                        .eq(PlayerSkillEntity::getDeleteFlag, 0)
                        .orderByAsc(PlayerSkillEntity::getId)
        );

        Map<Long, Integer> result = new LinkedHashMap<>();
        for (PlayerSkillEntity playerSkill : playerSkills) {
            result.put(
                    playerSkill.getSkillId(),
                    defaultInt(playerSkill.getSkillLevel(), 1)
            );
        }
        return result;
    }

    /**
     * 通过 groupCode 找到组节点。
     *
     * @param groupNodeById 组节点映射
     * @param groupCode     组编码
     * @return 树节点
     */
    private ActionTreeResponseDTO.TreeNode findGroupNodeByCode(
            Map<Long, ActionTreeResponseDTO.TreeNode> groupNodeById,
            String groupCode
    ) {
        for (ActionTreeResponseDTO.TreeNode node : groupNodeById.values()) {
            if (Objects.equals(node.getCode(), groupCode)) {
                return node;
            }
        }
        return null;
    }

    /**
     * 构建 GROUP 节点。
     *
     * @param entity 行为域定义
     * @return 树节点
     */
    private ActionTreeResponseDTO.TreeNode buildGroupNode(BehaviorGroupDefEntity entity) {
        ActionTreeResponseDTO.TreeNode node = baseNode(
                entity.getId(),
                NODE_GROUP,
                entity.getGroupCode(),
                entity.getGroupName(),
                groupOrder(entity.getGroupCode()),
                entity.getStatus()
        );
        if ("BATTLE".equals(entity.getGroupCode())) {
            node.setSpecialType(SPECIAL_BATTLE);
        }
        node.setMeta(buildSimpleMeta(entity.getParamsJson()));
        return node;
    }

    /**
     * 构建 BEHAVIOR 节点。
     *
     * @param entity 行为定义
     * @return 树节点
     */
    private ActionTreeResponseDTO.TreeNode buildBehaviorNode(BehaviorDefEntity entity) {
        ActionTreeResponseDTO.TreeNode node = baseNode(
                entity.getId(),
                NODE_BEHAVIOR,
                entity.getBehaviorCode(),
                entity.getBehaviorName(),
                defaultLong(entity.getId(), 0L).intValue(),
                entity.getStatus()
        );

        if ("BATTLE".equals(entity.getGroupCode()) || "COMBAT".equals(entity.getBehaviorCode())) {
            node.setSpecialType(SPECIAL_BATTLE);
        }

        Map<String, Object> meta = buildSimpleMeta(entity.getParamsJson());
        meta.put("groupCode", entity.getGroupCode());
        meta.put("settleGranularitySec", defaultInt(entity.getSettleGranularitySec(), 1));
        meta.put("allowParallel", defaultInt(entity.getAllowParallel(), 0) == 1);
        node.setMeta(meta);
        return node;
    }

    /**
     * 构建 CATEGORY 节点。
     *
     * @param entity 分类定义
     * @return 树节点
     */
    private ActionTreeResponseDTO.TreeNode buildCategoryNode(CategoryDefEntity entity) {
        ActionTreeResponseDTO.TreeNode node = baseNode(
                entity.getId(),
                NODE_CATEGORY,
                entity.getCategoryCode(),
                entity.getCategoryName(),
                defaultInt(entity.getSort(), 0),
                entity.getStatus()
        );
        node.setMeta(buildSimpleMeta(entity.getParamsJson()));
        return node;
    }

    /**
     * 构建 SUB_CATEGORY 节点。
     *
     * @param entity 子分类定义
     * @return 树节点
     */
    private ActionTreeResponseDTO.TreeNode buildSubCategoryNode(SubCategoryDefEntity entity) {
        ActionTreeResponseDTO.TreeNode node = baseNode(
                entity.getId(),
                NODE_SUB_CATEGORY,
                entity.getSubCode(),
                entity.getSubName(),
                defaultInt(entity.getSort(), 0),
                entity.getStatus()
        );

        Map<String, Object> meta = buildSimpleMeta(entity.getParamsJson());
        meta.put("parentId", defaultLong(entity.getParentId(), 0L));
        node.setMeta(meta);
        return node;
    }

    /**
     * 构建 ACTION 节点。
     *
     * 规则：
     * 1. 先看玩家等级是否满足 minPlayerLevel。
     * 2. 再看动作 params_json.skillCode 对应的职业等级是否满足 minSkillLevel。
     * 3. 如果动作未配置 skillCode，则保持旧逻辑，只看玩家等级。
     *
     * @param entity                    动作定义
     * @param playerLevel               玩家等级
     * @param skillIdByCode             技能ID映射
     * @param playerSkillLevelBySkillId 玩家技能等级映射
     * @return 树节点
     */
    private ActionTreeResponseDTO.TreeNode buildActionNode(
            ActionDefEntity entity,
            int playerLevel,
            Map<String, Long> skillIdByCode,
            Map<Long, Integer> playerSkillLevelBySkillId
    ) {
        ActionTreeResponseDTO.TreeNode node = baseNode(
                entity.getId(),
                NODE_ACTION,
                entity.getActionCode(),
                entity.getActionName(),
                defaultLong(entity.getId(), 0L).intValue(),
                entity.getStatus()
        );

        if ("MODULE".equals(entity.getActionKind())) {
            node.setSpecialType(SPECIAL_MODULE);
        }

        int minPlayerLevel = defaultInt(entity.getMinPlayerLevel(), 1);
        int minSkillLevel = defaultInt(entity.getMinSkillLevel(), 0);

        Object actionParams = parseJson(entity.getParamsJson());
        String skillCode = extractSkillCode(actionParams);
        int currentSkillLevel = resolvePlayerSkillLevel(skillCode, skillIdByCode, playerSkillLevelBySkillId);

        boolean playerLevelUnlocked = playerLevel >= minPlayerLevel;
        boolean skillLevelUnlocked = !StringUtils.hasText(skillCode) || currentSkillLevel >= minSkillLevel;

        node.setUnlocked(playerLevelUnlocked && skillLevelUnlocked);
        node.setExpandable(false);
        node.setLeaf(true);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("behaviorId", entity.getBehaviorId());
        meta.put("categoryId", defaultLong(entity.getCategoryId(), 0L));
        meta.put("subCategoryId", defaultLong(entity.getSubCategoryId(), 0L));
        meta.put("actionKind", entity.getActionKind());
        meta.put("baseDurationMs", defaultInt(entity.getBaseDurationMs(), 0));
        meta.put("baseEnergyCost", defaultInt(entity.getBaseEnergyCost(), 0));
        meta.put("minPlayerLevel", minPlayerLevel);
        meta.put("minSkillLevel", minSkillLevel);
        meta.put("skillCode", skillCode);
        meta.put("currentSkillLevel", currentSkillLevel);
        meta.put("durationScaleRule", entity.getDurationScaleRule());
        meta.put("rewardScaleRule", entity.getRewardScaleRule());
        meta.put("unlockCondition", parseJson(entity.getUnlockConditionJson()));
        meta.put("params", actionParams);
        node.setMeta(meta);
        return node;
    }

    /**
     * 从动作 params 中提取 skillCode。
     *
     * @param actionParams 动作参数
     * @return skillCode
     */
    private String extractSkillCode(Object actionParams) {
        if (!(actionParams instanceof Map)) {
            return null;
        }
        Object skillCode = ((Map<?, ?>) actionParams).get("skillCode");
        return skillCode == null ? null : String.valueOf(skillCode);
    }

    /**
     * 根据 skillCode 解析玩家当前技能等级。
     *
     * 规则：
     * 1. 未配置 skillCode，返回 0。
     * 2. skillCode 找不到 skillId，返回 1。
     * 3. 玩家没有该技能记录，返回 1。
     *
     * @param skillCode                 技能编码
     * @param skillIdByCode             技能ID映射
     * @param playerSkillLevelBySkillId 玩家技能等级映射
     * @return 当前技能等级
     */
    private int resolvePlayerSkillLevel(
            String skillCode,
            Map<String, Long> skillIdByCode,
            Map<Long, Integer> playerSkillLevelBySkillId
    ) {
        if (!StringUtils.hasText(skillCode)) {
            return 0;
        }

        Long skillId = skillIdByCode.get(skillCode);
        if (skillId == null) {
            return 1;
        }

        Integer level = playerSkillLevelBySkillId.get(skillId);
        return level == null ? 1 : level;
    }

    /**
     * 构建基础节点。
     *
     * @param id       主键ID
     * @param nodeType 节点类型
     * @param code     编码
     * @param name     名称
     * @param sort     排序
     * @param status   状态
     * @return 树节点
     */
    private ActionTreeResponseDTO.TreeNode baseNode(
            Long id,
            String nodeType,
            String code,
            String name,
            Integer sort,
            Integer status
    ) {
        ActionTreeResponseDTO.TreeNode node = new ActionTreeResponseDTO.TreeNode();
        node.setId(id);
        node.setNodeType(nodeType);
        node.setNodeKey(nodeType + ":" + id);
        node.setCode(code);
        node.setName(name);
        node.setSort(defaultInt(sort, 0));
        node.setStatus(defaultInt(status, 1));
        node.setUnlocked(true);
        node.setExpandable(false);
        node.setLeaf(false);
        return node;
    }

    /**
     * 递归回填树节点派生字段。
     *
     * @param node               当前节点
     * @param parentPathNodeKeys 父级节点 Key 路径
     * @param parentPathCodes    父级编码路径
     * @param parentPathNames    父级名称路径
     */
    private void fillDerivedFields(
            ActionTreeResponseDTO.TreeNode node,
            List<String> parentPathNodeKeys,
            List<String> parentPathCodes,
            List<String> parentPathNames
    ) {
        List<String> pathNodeKeys = new ArrayList<>(parentPathNodeKeys);
        pathNodeKeys.add(node.getNodeKey());
        node.setPathNodeKeys(pathNodeKeys);

        List<String> pathCodes = new ArrayList<>(parentPathCodes);
        pathCodes.add(node.getCode());
        node.setPathCodes(pathCodes);

        List<String> pathNames = new ArrayList<>(parentPathNames);
        pathNames.add(node.getName());
        node.setPathNames(pathNames);

        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            node.setChildren(new ArrayList<>());
            node.setExpandable(false);
            node.setLeaf(NODE_ACTION.equals(node.getNodeType()));

            if (!NODE_ACTION.equals(node.getNodeType())) {
                if (NODE_SUB_CATEGORY.equals(node.getNodeType()) && "OTHER".equals(node.getCode())) {
                    node.setUnlocked(true);
                } else {
                    node.setUnlocked(false);
                }
            }
            return;
        }

        node.getChildren().sort(this::compareNodes);

        boolean hasUnlockedChild = false;
        for (ActionTreeResponseDTO.TreeNode child : node.getChildren()) {
            fillDerivedFields(child, pathNodeKeys, pathCodes, pathNames);
            if (Boolean.TRUE.equals(child.getUnlocked())) {
                hasUnlockedChild = true;
            }
        }

        node.setExpandable(true);
        node.setLeaf(false);

        if (!NODE_ACTION.equals(node.getNodeType())) {
            node.setUnlocked(hasUnlockedChild);
        }
    }

    /**
     * 树节点排序。
     *
     * @param a 节点A
     * @param b 节点B
     * @return 比较结果
     */
    private int compareNodes(ActionTreeResponseDTO.TreeNode a, ActionTreeResponseDTO.TreeNode b) {
        int typeCompare = Integer.compare(typeOrder(a.getNodeType()), typeOrder(b.getNodeType()));
        if (typeCompare != 0) {
            return typeCompare;
        }

        int sortCompare = Integer.compare(defaultInt(a.getSort(), 0), defaultInt(b.getSort(), 0));
        if (sortCompare != 0) {
            return sortCompare;
        }

        return Long.compare(defaultLong(a.getId(), 0L), defaultLong(b.getId(), 0L));
    }

    /**
     * 节点类型排序值。
     *
     * @param nodeType 节点类型
     * @return 排序值
     */
    private int typeOrder(String nodeType) {
        if (NODE_GROUP.equals(nodeType)) {
            return 10;
        }
        if (NODE_BEHAVIOR.equals(nodeType)) {
            return 20;
        }
        if (NODE_CATEGORY.equals(nodeType)) {
            return 30;
        }
        if (NODE_SUB_CATEGORY.equals(nodeType)) {
            return 40;
        }
        if (NODE_ACTION.equals(nodeType)) {
            return 50;
        }
        return 999;
    }

    /**
     * 行为域排序。
     *
     * @param groups 行为域列表
     * @return 排序后的行为域
     */
    private List<BehaviorGroupDefEntity> sortGroups(List<BehaviorGroupDefEntity> groups) {
        return groups.stream()
                .sorted(
                        Comparator.comparingInt((BehaviorGroupDefEntity x) -> groupOrder(x.getGroupCode()))
                                .thenComparingLong(x -> defaultLong(x.getId(), 0L))
                )
                .collect(Collectors.toList());
    }

    /**
     * 行为域顺序。
     *
     * @param groupCode 行为域编码
     * @return 排序值
     */
    private int groupOrder(String groupCode) {
        if ("GATHER".equals(groupCode)) {
            return 10;
        }
        if ("CRAFT".equals(groupCode)) {
            return 20;
        }
        if ("TRADE".equals(groupCode)) {
            return 30;
        }
        if ("QUEST".equals(groupCode)) {
            return 40;
        }
        if ("BATTLE".equals(groupCode)) {
            return 50;
        }
        if ("OTHER".equals(groupCode)) {
            return 60;
        }
        return 999;
    }

    /**
     * 构建简单 meta。
     *
     * @param paramsJson 参数 JSON
     * @return meta
     */
    private Map<String, Object> buildSimpleMeta(String paramsJson) {
        Map<String, Object> meta = new LinkedHashMap<>();
        Object params = parseJson(paramsJson);
        if (params != null) {
            meta.put("params", params);
        }
        return meta;
    }

    /**
     * 解析 JSON。
     *
     * @param json JSON 字符串
     * @return 解析结果
     */
    private Object parseJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Object>() {});
        } catch (Exception e) {
            return json;
        }
    }

    /**
     * 判断逻辑删除。
     *
     * @param deleteFlag 删除标记
     * @return 是否删除
     */
    private boolean isDeleted(Integer deleteFlag) {
        return deleteFlag != null && deleteFlag == 1;
    }

    /**
     * 空值兜底整型。
     *
     * @param val        原值
     * @param defaultVal 默认值
     * @return 结果
     */
    private Integer defaultInt(Integer val, Integer defaultVal) {
        return val == null ? defaultVal : val;
    }

    /**
     * 空值兜底长整型。
     *
     * @param val        原值
     * @param defaultVal 默认值
     * @return 结果
     */
    private Long defaultLong(Long val, Long defaultVal) {
        return val == null ? defaultVal : val;
    }
}