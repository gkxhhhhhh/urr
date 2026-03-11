package com.urr.app.game.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urr.app.game.ActionTreeAppService;
import com.urr.domain.action.*;
import com.urr.domain.action.dto.ActionTreeResponseDTO;
import com.urr.domain.player.PlayerEntity;
import com.urr.infra.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActionTreeAppServiceImpl implements ActionTreeAppService {

    private static final String NODE_GROUP = "GROUP";
    private static final String NODE_BEHAVIOR = "BEHAVIOR";
    private static final String NODE_CATEGORY = "CATEGORY";
    private static final String NODE_SUB_CATEGORY = "SUB_CATEGORY";
    private static final String NODE_ACTION = "ACTION";

    private static final String SPECIAL_BATTLE = "BATTLE";
    private static final String SPECIAL_MODULE = "MODULE";

    private final PlayerMapper playerMapper;
    private final ActionDefMapper actionDefMapper;
    private final BehaviorGroupDefMapper behaviorGroupDefMapper;
    private final BehaviorDefMapper behaviorDefMapper;
    private final CategoryDefMapper categoryDefMapper;
    private final SubCategoryDefMapper subCategoryDefMapper;
    private final ObjectMapper objectMapper;

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

        Map<Long, ActionTreeResponseDTO.TreeNode> groupNodeById = new LinkedHashMap<>();
        Map<Long, ActionTreeResponseDTO.TreeNode> behaviorNodeById = new LinkedHashMap<>();
        Map<Long, ActionTreeResponseDTO.TreeNode> categoryNodeById = new LinkedHashMap<>();
        Map<Long, ActionTreeResponseDTO.TreeNode> subCategoryNodeById = new LinkedHashMap<>();

        // 1. GROUP
        for (BehaviorGroupDefEntity group : sortGroups(groups)) {
            ActionTreeResponseDTO.TreeNode node = buildGroupNode(group);
            groupNodeById.put(group.getId(), node);
        }

        // 2. BEHAVIOR -> GROUP
        List<BehaviorDefEntity> sortedBehaviors = new ArrayList<>(behaviors);
        sortedBehaviors.sort(Comparator
                .comparingInt((BehaviorDefEntity x) -> groupOrder(x.getGroupCode()))
                .thenComparingLong(x -> defaultLong(x.getId(), 0L)));

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

        // 3. CATEGORY -> BEHAVIOR
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

        // 4. SUB_CATEGORY 先建节点
        for (SubCategoryDefEntity subCategory : subCategories) {
            ActionTreeResponseDTO.TreeNode node = buildSubCategoryNode(subCategory);
            subCategoryNodeById.put(subCategory.getId(), node);
        }

        // 5. SUB_CATEGORY 挂到 CATEGORY 或父 SUB_CATEGORY
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

        // 6. ACTION -> SUB_CATEGORY / CATEGORY / BEHAVIOR
        for (ActionDefEntity action : actions) {
            ActionTreeResponseDTO.TreeNode node = buildActionNode(action, playerLevel);

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

    private PlayerEntity requirePlayer(Long playerId) {
        PlayerEntity player = playerMapper.selectById(playerId);
        if (player == null || isDeleted(player.getDeleteFlag())) {
            throw new IllegalArgumentException("角色不存在");
        }
        return player;
    }

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

    private ActionTreeResponseDTO.TreeNode buildActionNode(ActionDefEntity entity, int playerLevel) {
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

        node.setUnlocked(playerLevel >= defaultInt(entity.getMinPlayerLevel(), 1));
        node.setExpandable(false);
        node.setLeaf(true);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("behaviorId", entity.getBehaviorId());
        meta.put("categoryId", defaultLong(entity.getCategoryId(), 0L));
        meta.put("subCategoryId", defaultLong(entity.getSubCategoryId(), 0L));
        meta.put("actionKind", entity.getActionKind());
        meta.put("baseDurationMs", defaultInt(entity.getBaseDurationMs(), 0));
        meta.put("baseEnergyCost", defaultInt(entity.getBaseEnergyCost(), 0));
        meta.put("minPlayerLevel", defaultInt(entity.getMinPlayerLevel(), 1));
        meta.put("minSkillLevel", defaultInt(entity.getMinSkillLevel(), 0));
        meta.put("durationScaleRule", entity.getDurationScaleRule());
        meta.put("rewardScaleRule", entity.getRewardScaleRule());
        meta.put("unlockCondition", parseJson(entity.getUnlockConditionJson()));
        meta.put("params", parseJson(entity.getParamsJson()));
        node.setMeta(meta);

        return node;
    }

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

    private int typeOrder(String nodeType) {
        if (NODE_GROUP.equals(nodeType)) return 10;
        if (NODE_BEHAVIOR.equals(nodeType)) return 20;
        if (NODE_CATEGORY.equals(nodeType)) return 30;
        if (NODE_SUB_CATEGORY.equals(nodeType)) return 40;
        if (NODE_ACTION.equals(nodeType)) return 50;
        return 999;
    }

    private List<BehaviorGroupDefEntity> sortGroups(List<BehaviorGroupDefEntity> groups) {
        return groups.stream()
                .sorted(Comparator
                        .comparingInt((BehaviorGroupDefEntity x) -> groupOrder(x.getGroupCode()))
                        .thenComparingLong(x -> defaultLong(x.getId(), 0L)))
                .collect(Collectors.toList());
    }

    private int groupOrder(String groupCode) {
        if ("GATHER".equals(groupCode)) return 10;
        if ("CRAFT".equals(groupCode)) return 20;
        if ("TRADE".equals(groupCode)) return 30;
        if ("QUEST".equals(groupCode)) return 40;
        if ("BATTLE".equals(groupCode)) return 50;
        if ("OTHER".equals(groupCode)) return 60;
        return 999;
    }

    private Map<String, Object> buildSimpleMeta(String paramsJson) {
        Map<String, Object> meta = new LinkedHashMap<>();
        Object params = parseJson(paramsJson);
        if (params != null) {
            meta.put("params", params);
        }
        return meta;
    }

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

    private boolean isDeleted(Integer deleteFlag) {
        return deleteFlag != null && deleteFlag == 1;
    }

    private Integer defaultInt(Integer val, Integer defaultVal) {
        return val == null ? defaultVal : val;
    }

    private Long defaultLong(Long val, Long defaultVal) {
        return val == null ? defaultVal : val;
    }
}