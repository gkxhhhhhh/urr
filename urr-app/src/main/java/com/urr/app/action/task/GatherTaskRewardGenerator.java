package com.urr.app.action.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urr.domain.action.ActionDefEntity;
import com.urr.domain.action.task.GatherTaskRewardEntry;
import com.urr.domain.action.task.GatherTaskStatSnapshot;
import com.urr.domain.action.task.PlayerGatherTask;
import com.urr.infra.mapper.ActionDefMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 采集任务奖励生成器。
 *
 * 说明：
 * 1. 不再硬编码动作到物品的映射。
 * 2. 所有采集奖励配置都在项目启动时从 t_urr_action_def.params_json 读取并缓存。
 * 3. 任务启动时会把关键奖励字段锁进快照；结算时优先用快照，兼容旧任务时再回退缓存。
 * 4. 本次新增 rewardList 支持：允许一个动作对应多个候选物品，每轮按 chance 选 1 个。
 */
@Component
@RequiredArgsConstructor
public class GatherTaskRewardGenerator {

    /**
     * 奖励类型：物品。
     */
    public static final String REWARD_TYPE_ITEM = "ITEM";

    /**
     * 百分比 100%。
     */
    private static final BigDecimal FULL_PERCENT = new BigDecimal("100");

    /**
     * 基点分母。
     */
    private static final BigDecimal BASIS_POINT_BASE = new BigDecimal("100");

    /**
     * 动作定义 Mapper。
     */
    private final ActionDefMapper actionDefMapper;

    /**
     * JSON 编解码器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 启动后常驻的奖励配置缓存。
     */
    private final Map<String, ActionRewardConfig> actionRewardConfigCache =
            new ConcurrentHashMap<String, ActionRewardConfig>();

    /**
     * 项目启动时加载奖励配置缓存。
     */
    @PostConstruct
    public void initActionRewardConfigCache() {
        reloadActionRewardConfigCache();
    }

    /**
     * 重新加载奖励配置缓存。
     */
    public void reloadActionRewardConfigCache() {
        List<ActionDefEntity> actionList = actionDefMapper.selectList(
                new LambdaQueryWrapper<ActionDefEntity>()
                        .eq(ActionDefEntity::getStatus, 1)
                        .eq(ActionDefEntity::getDeleteFlag, 0)
        );

        Map<String, ActionRewardConfig> latestMap = new ConcurrentHashMap<String, ActionRewardConfig>();
        for (int i = 0; i < actionList.size(); i += 1) {
            ActionDefEntity action = actionList.get(i);
            ActionRewardConfig config = tryBuildRewardConfig(action);
            if (config == null) {
                continue;
            }
            latestMap.put(config.getActionCode(), config);
        }

        actionRewardConfigCache.clear();
        actionRewardConfigCache.putAll(latestMap);
    }

    /**
     * 生成某一轮的锁定奖励。
     *
     * @param task 采集任务
     * @param roundIndex 轮次序号，建议使用 1-based
     * @return 该轮奖励列表
     */
    public List<GatherTaskRewardEntry> generateRoundRewards(PlayerGatherTask task, long roundIndex) {
        validateTask(task);

        ActionRewardConfig config = requireRewardConfig(task);
        ActionRewardItemConfig rewardItem = rollRewardItem(task, roundIndex, config);
        long quantity = calculateItemQuantity(task, roundIndex, config);

        List<GatherTaskRewardEntry> rewardList = new ArrayList<GatherTaskRewardEntry>();
        if (rewardItem == null || quantity <= 0L) {
            return rewardList;
        }

        GatherTaskRewardEntry rewardEntry = new GatherTaskRewardEntry();
        rewardEntry.setRewardType(REWARD_TYPE_ITEM);
        rewardEntry.setRewardCode(rewardItem.getItemCode());
        rewardEntry.setQuantity(quantity);
        rewardList.add(rewardEntry);
        return rewardList;
    }

    /**
     * 查询并校验某个动作的奖励配置。
     *
     * @param actionCode 动作编码
     * @return 奖励配置
     */
    public ActionRewardConfig requireActionRewardConfig(String actionCode) {
        if (!StringUtils.hasText(actionCode)) {
            throw new IllegalArgumentException("actionCode不能为空");
        }

        ActionRewardConfig config = actionRewardConfigCache.get(actionCode.trim());
        if (config == null) {
            throw new IllegalStateException("当前采集动作缺少奖励配置，actionCode=" + actionCode);
        }
        return copyConfig(config);
    }

    /**
     * 解析任务对应的技能编码。
     *
     * @param task 采集任务
     * @return 技能编码
     */
    public String resolveSkillCode(PlayerGatherTask task) {
        ActionRewardConfig config = requireRewardConfig(task);
        return config.getSkillCode();
    }

    /**
     * 解析任务对应的每轮经验。
     *
     * @param task 采集任务
     * @return 每轮经验
     */
    public int resolveExpGain(PlayerGatherTask task) {
        ActionRewardConfig config = requireRewardConfig(task);
        return config.getExpGain() == null ? 0 : config.getExpGain();
    }

    /**
     * 构建快照用的 rewardListJson。
     *
     * @param config 奖励配置
     * @return rewardListJson
     */
    public String buildSnapshotRewardListJson(ActionRewardConfig config) {
        if (config == null) {
            return null;
        }

        try {
            List<ActionRewardItemConfig> rewardItems = copyRewardItemList(config.getRewardItems());
            if (rewardItems.isEmpty() && StringUtils.hasText(config.getItemCode())) {
                ActionRewardItemConfig itemConfig = new ActionRewardItemConfig();
                itemConfig.setItemCode(config.getItemCode());
                itemConfig.setChance(FULL_PERCENT);
                rewardItems.add(itemConfig);
            }

            if (rewardItems.isEmpty()) {
                return null;
            }

            return objectMapper.writeValueAsString(rewardItems);
        } catch (IOException e) {
            throw new IllegalStateException("构建 rewardListJson 失败", e);
        }
    }

    /**
     * 尝试从动作定义构建奖励配置。
     *
     * @param action 动作定义
     * @return 奖励配置；未配置奖励时返回 null
     */
    private ActionRewardConfig tryBuildRewardConfig(ActionDefEntity action) {
        if (action == null || !StringUtils.hasText(action.getActionCode())) {
            return null;
        }

        JsonNode root = readParamsNode(action);
        if (root == null) {
            return null;
        }

        List<ActionRewardItemConfig> rewardItems = readRewardItemConfigsFromRoot(root);
        if (rewardItems.isEmpty()) {
            return null;
        }

        ActionRewardConfig config = new ActionRewardConfig();
        config.setActionCode(action.getActionCode().trim());
        config.setRewardItems(rewardItems);
        config.setSkillCode(readText(root, "skillCode"));
        config.setExpGain(readInteger(root, "expGain"));
        config.setCriticalRate(readDecimal(root, "criticalRate"));
        config.setQuantityChance1(readDecimal(root, "quantityChance1"));
        config.setQuantityChance2(readDecimal(root, "quantityChance2"));
        config.setQuantityChance3(readDecimal(root, "quantityChance3"));
        validateRewardConfig(config);
        return config;
    }

    /**
     * 读取动作 params_json。
     *
     * @param action 动作定义
     * @return JSON 根节点
     */
    private JsonNode readParamsNode(ActionDefEntity action) {
        if (action == null || !StringUtils.hasText(action.getParamsJson())) {
            return null;
        }

        try {
            return objectMapper.readTree(action.getParamsJson());
        } catch (IOException e) {
            throw new IllegalStateException("动作 params_json 解析失败，actionCode=" + action.getActionCode(), e);
        }
    }

    /**
     * 从任务中解析奖励配置。
     *
     * @param task 采集任务
     * @return 奖励配置
     */
    private ActionRewardConfig requireRewardConfig(PlayerGatherTask task) {
        ActionRewardConfig snapshotConfig = buildConfigFromSnapshot(task == null ? null : task.getStatSnapshot());
        if (snapshotConfig != null) {
            return snapshotConfig;
        }
        return requireActionRewardConfig(task.getActionCode());
    }

    /**
     * 从快照构建奖励配置。
     *
     * @param snapshot 采集快照
     * @return 奖励配置
     */
    private ActionRewardConfig buildConfigFromSnapshot(GatherTaskStatSnapshot snapshot) {
        if (snapshot == null || !snapshot.hasLockedRewardConfig()) {
            return null;
        }

        ActionRewardConfig config = new ActionRewardConfig();
        config.setSkillCode(snapshot.getSkillCode());
        config.setExpGain(snapshot.getExpGain());
        config.setCriticalRate(snapshot.getCriticalRate());
        config.setQuantityChance1(snapshot.getQuantityChance1());
        config.setQuantityChance2(snapshot.getQuantityChance2());
        config.setQuantityChance3(snapshot.getQuantityChance3());

        List<ActionRewardItemConfig> rewardItems = readRewardItemConfigsFromSnapshot(snapshot);
        config.setRewardItems(rewardItems);

        if (rewardItems.size() == 1) {
            config.setItemCode(rewardItems.get(0).getItemCode());
        }

        validateRewardConfig(config);
        return config;
    }

    /**
     * 从快照读取奖励列表。
     *
     * @param snapshot 采集快照
     * @return 奖励列表
     */
    private List<ActionRewardItemConfig> readRewardItemConfigsFromSnapshot(GatherTaskStatSnapshot snapshot) {
        List<ActionRewardItemConfig> result = new ArrayList<ActionRewardItemConfig>();
        if (snapshot == null) {
            return result;
        }

        if (StringUtils.hasText(snapshot.getRewardListJson())) {
            try {
                JsonNode root = objectMapper.readTree(snapshot.getRewardListJson());
                if (root != null && root.isArray()) {
                    for (int i = 0; i < root.size(); i += 1) {
                        JsonNode itemNode = root.get(i);
                        ActionRewardItemConfig itemConfig = buildRewardItemConfig(itemNode);
                        if (itemConfig != null) {
                            result.add(itemConfig);
                        }
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("采集快照 rewardListJson 解析失败", e);
            }
        }

        if (!result.isEmpty()) {
            return result;
        }

        if (StringUtils.hasText(snapshot.getItemCode())) {
            ActionRewardItemConfig itemConfig = new ActionRewardItemConfig();
            itemConfig.setItemCode(snapshot.getItemCode().trim());
            itemConfig.setChance(FULL_PERCENT);
            result.add(itemConfig);
        }

        return result;
    }

    /**
     * 从动作 params 中读取奖励列表。
     *
     * 支持两种格式：
     * 1. 单采：itemCode
     * 2. 混采：rewardList:[{itemCode,chance}]
     *
     * @param root JSON 根节点
     * @return 奖励列表
     */
    private List<ActionRewardItemConfig> readRewardItemConfigsFromRoot(JsonNode root) {
        List<ActionRewardItemConfig> result = new ArrayList<ActionRewardItemConfig>();
        if (root == null) {
            return result;
        }

        JsonNode rewardListNode = root.get("rewardList");
        if (rewardListNode != null && rewardListNode.isArray()) {
            for (int i = 0; i < rewardListNode.size(); i += 1) {
                JsonNode itemNode = rewardListNode.get(i);
                ActionRewardItemConfig itemConfig = buildRewardItemConfig(itemNode);
                if (itemConfig != null) {
                    result.add(itemConfig);
                }
            }
            return result;
        }

        String itemCode = readText(root, "itemCode");
        if (StringUtils.hasText(itemCode)) {
            ActionRewardItemConfig itemConfig = new ActionRewardItemConfig();
            itemConfig.setItemCode(itemCode);
            itemConfig.setChance(FULL_PERCENT);
            result.add(itemConfig);
        }

        return result;
    }

    /**
     * 构建单条奖励项配置。
     *
     * @param itemNode 奖励项 JSON
     * @return 奖励项配置
     */
    private ActionRewardItemConfig buildRewardItemConfig(JsonNode itemNode) {
        if (itemNode == null || itemNode.isNull()) {
            return null;
        }

        String itemCode = readText(itemNode, "itemCode");
        if (!StringUtils.hasText(itemCode)) {
            itemCode = readText(itemNode, "rewardCode");
        }
        if (!StringUtils.hasText(itemCode)) {
            return null;
        }

        BigDecimal chance = readDecimal(itemNode, "chance");
        if (chance == null) {
            chance = readDecimal(itemNode, "weight");
        }
        if (chance == null) {
            chance = readDecimal(itemNode, "rate");
        }
        if (chance == null) {
            chance = readDecimal(itemNode, "probability");
        }

        ActionRewardItemConfig config = new ActionRewardItemConfig();
        config.setItemCode(itemCode);
        config.setChance(chance);
        return config;
    }

    /**
     * 按轮次选择本轮产物。
     *
     * @param task 采集任务
     * @param roundIndex 轮次
     * @param config 奖励配置
     * @return 选中的奖励项
     */
    private ActionRewardItemConfig rollRewardItem(PlayerGatherTask task, long roundIndex, ActionRewardConfig config) {
        List<ActionRewardItemConfig> rewardItems = config.getRewardItems();
        if (rewardItems == null || rewardItems.isEmpty()) {
            return null;
        }

        if (rewardItems.size() == 1) {
            return rewardItems.get(0);
        }

        long rollBasisPoint = nextPositiveValue(task.getRewardSeed(), roundIndex, 3) % 10000L;
        long cumulative = 0L;
        for (int i = 0; i < rewardItems.size(); i += 1) {
            ActionRewardItemConfig itemConfig = rewardItems.get(i);
            cumulative += toBasisPoint(itemConfig.getChance());
            if (rollBasisPoint < cumulative) {
                return itemConfig;
            }
        }

        return rewardItems.get(rewardItems.size() - 1);
    }

    /**
     * 计算一轮采集的物品数量。
     *
     * 规则：
     * 1. 先按 quantityChance1/2/3 算基础数量。
     * 2. 再按 criticalRate 算额外数量。
     * 3. 暴击率允许溢出：150% = 保底 +1，再用 50% 继续判定额外 +1。
     *
     * @param task 采集任务
     * @param roundIndex 轮次序号
     * @param config 奖励配置
     * @return 本轮产出数量
     */
    private long calculateItemQuantity(PlayerGatherTask task, long roundIndex, ActionRewardConfig config) {
        long baseQuantity = rollBaseQuantity(task, roundIndex, config);
        long criticalExtra = rollCriticalExtra(task, roundIndex, config);
        return baseQuantity + criticalExtra;
    }

    /**
     * 计算基础产出数量。
     *
     * @param task 采集任务
     * @param roundIndex 轮次序号
     * @param config 奖励配置
     * @return 基础数量
     */
    private long rollBaseQuantity(PlayerGatherTask task, long roundIndex, ActionRewardConfig config) {
        long rollBasisPoint = nextPositiveValue(task.getRewardSeed(), roundIndex, 1) % 10000L;
        long chance3 = toBasisPoint(config.getQuantityChance3());
        long chance2 = toBasisPoint(config.getQuantityChance2());

        if (rollBasisPoint < chance3) {
            return 3L;
        }
        if (rollBasisPoint < chance3 + chance2) {
            return 2L;
        }
        return 1L;
    }

    /**
     * 计算暴击额外数量。
     *
     * @param task 采集任务
     * @param roundIndex 轮次序号
     * @param config 奖励配置
     * @return 额外数量
     */
    private long rollCriticalExtra(PlayerGatherTask task, long roundIndex, ActionRewardConfig config) {
        BigDecimal criticalRate = config.getCriticalRate();
        if (criticalRate == null || criticalRate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        long extra = 0L;
        BigDecimal remaining = criticalRate;
        while (remaining.compareTo(FULL_PERCENT) >= 0) {
            extra += 1L;
            remaining = remaining.subtract(FULL_PERCENT);
        }

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return extra;
        }

        long remainderBasisPoint = toBasisPoint(remaining);
        long rollBasisPoint = nextPositiveValue(task.getRewardSeed(), roundIndex, 2) % 10000L;
        if (rollBasisPoint < remainderBasisPoint) {
            extra += 1L;
        }
        return extra;
    }

    /**
     * 把百分比转换成基点。
     *
     * @param percent 百分比数值
     * @return 基点整数
     */
    private long toBasisPoint(BigDecimal percent) {
        if (percent == null) {
            return 0L;
        }
        return percent.multiply(BASIS_POINT_BASE).longValue();
    }

    /**
     * 读取字符串字段。
     *
     * @param root JSON 根节点
     * @param field 字段名
     * @return 字符串值
     */
    private String readText(JsonNode root, String field) {
        JsonNode valueNode = root == null ? null : root.get(field);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }

        String value = valueNode.asText();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 读取整数配置。
     *
     * @param root JSON 根节点
     * @param field 字段名
     * @return 整数值
     */
    private Integer readInteger(JsonNode root, String field) {
        JsonNode valueNode = root == null ? null : root.get(field);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }

        if (valueNode.isInt() || valueNode.isLong()) {
            return valueNode.asInt();
        }

        String text = valueNode.asText();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Integer.valueOf(text.trim());
    }

    /**
     * 读取小数字段。
     *
     * @param root JSON 根节点
     * @param field 字段名
     * @return 小数值
     */
    private BigDecimal readDecimal(JsonNode root, String field) {
        JsonNode valueNode = root == null ? null : root.get(field);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }

        String text = valueNode.asText();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return new BigDecimal(text.trim());
    }

    /**
     * 校验奖励配置。
     *
     * @param config 奖励配置
     */
    private void validateRewardConfig(ActionRewardConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("奖励配置不能为空");
        }

        List<ActionRewardItemConfig> rewardItems = config.getRewardItems();
        if (rewardItems == null) {
            rewardItems = new ArrayList<ActionRewardItemConfig>();
            config.setRewardItems(rewardItems);
        }

        if (rewardItems.isEmpty() && StringUtils.hasText(config.getItemCode())) {
            ActionRewardItemConfig itemConfig = new ActionRewardItemConfig();
            itemConfig.setItemCode(config.getItemCode().trim());
            itemConfig.setChance(FULL_PERCENT);
            rewardItems.add(itemConfig);
        }

        if (rewardItems.isEmpty()) {
            throw new IllegalStateException("奖励配置缺少 rewardList/itemCode");
        }

        if (rewardItems.size() == 1 && rewardItems.get(0).getChance() == null) {
            rewardItems.get(0).setChance(FULL_PERCENT);
        }

        BigDecimal rewardChanceTotal = BigDecimal.ZERO;
        for (int i = 0; i < rewardItems.size(); i += 1) {
            ActionRewardItemConfig itemConfig = rewardItems.get(i);
            if (itemConfig == null || !StringUtils.hasText(itemConfig.getItemCode())) {
                throw new IllegalStateException("奖励配置缺少 itemCode");
            }
            if (itemConfig.getChance() == null) {
                throw new IllegalStateException("奖励配置缺少 chance，itemCode=" + itemConfig.getItemCode());
            }
            rewardChanceTotal = rewardChanceTotal.add(itemConfig.getChance());
        }

        if (rewardChanceTotal.compareTo(FULL_PERCENT) != 0) {
            throw new IllegalStateException("奖励配置 rewardList 概率之和必须等于100");
        }

        if (config.getQuantityChance1() == null || config.getQuantityChance2() == null || config.getQuantityChance3() == null) {
            throw new IllegalStateException("奖励配置缺少基础爆率");
        }

        BigDecimal quantityChanceTotal = config.getQuantityChance1()
                .add(config.getQuantityChance2())
                .add(config.getQuantityChance3());
        if (quantityChanceTotal.compareTo(FULL_PERCENT) != 0) {
            throw new IllegalStateException("奖励配置基础爆率之和必须等于100");
        }

        if (config.getCriticalRate() == null) {
            config.setCriticalRate(BigDecimal.ZERO);
        }
        if (config.getExpGain() == null) {
            config.setExpGain(0);
        }

        if (rewardItems.size() == 1) {
            config.setItemCode(rewardItems.get(0).getItemCode());
        } else {
            config.setItemCode(null);
        }
    }

    /**
     * 复制一份奖励配置，避免缓存对象被外部误改。
     *
     * @param source 原配置
     * @return 新配置
     */
    private ActionRewardConfig copyConfig(ActionRewardConfig source) {
        ActionRewardConfig copy = new ActionRewardConfig();
        copy.setActionCode(source.getActionCode());
        copy.setItemCode(source.getItemCode());
        copy.setSkillCode(source.getSkillCode());
        copy.setExpGain(source.getExpGain());
        copy.setCriticalRate(source.getCriticalRate());
        copy.setQuantityChance1(source.getQuantityChance1());
        copy.setQuantityChance2(source.getQuantityChance2());
        copy.setQuantityChance3(source.getQuantityChance3());
        copy.setRewardItems(copyRewardItemList(source.getRewardItems()));
        return copy;
    }

    /**
     * 深拷贝奖励项列表。
     *
     * @param source 原列表
     * @return 新列表
     */
    private List<ActionRewardItemConfig> copyRewardItemList(List<ActionRewardItemConfig> source) {
        List<ActionRewardItemConfig> result = new ArrayList<ActionRewardItemConfig>();
        if (source == null || source.isEmpty()) {
            return result;
        }

        for (int i = 0; i < source.size(); i += 1) {
            ActionRewardItemConfig sourceItem = source.get(i);
            if (sourceItem == null) {
                continue;
            }
            ActionRewardItemConfig copyItem = new ActionRewardItemConfig();
            copyItem.setItemCode(sourceItem.getItemCode());
            copyItem.setChance(sourceItem.getChance());
            result.add(copyItem);
        }
        return result;
    }

    /**
     * 生成固定的正整数随机值。
     *
     * @param rewardSeed 奖励种子
     * @param roundIndex 轮次序号
     * @param salt 附加扰动
     * @return 正整数值
     */
    private long nextPositiveValue(Long rewardSeed, long roundIndex, long salt) {
        long seed = rewardSeed == null ? 1L : rewardSeed.longValue();
        long value = seed + roundIndex * 1103515245L + 12345L + salt * 2654435761L;
        value ^= (value << 13);
        value ^= (value >>> 7);
        value ^= (value << 17);
        return value & Long.MAX_VALUE;
    }

    /**
     * 校验采集任务是否具备奖励生成所需的最小字段。
     *
     * @param task 采集任务
     */
    private void validateTask(PlayerGatherTask task) {
        if (task == null) {
            throw new IllegalArgumentException("采集任务不能为空");
        }
        if (!StringUtils.hasText(task.getActionCode())) {
            throw new IllegalArgumentException("采集任务 actionCode 不能为空");
        }
        if (task.getStatSnapshot() == null) {
            throw new IllegalArgumentException("采集任务 statSnapshot 不能为空");
        }
    }

    /**
     * 动作奖励配置。
     */
    @Data
    public static class ActionRewardConfig {

        /**
         * 动作编码。
         */
        private String actionCode;

        /**
         * 单采产出物编码。
         * 为兼容旧逻辑保留；混采时允许为空。
         */
        private String itemCode;

        /**
         * 绑定技能编码。
         */
        private String skillCode;

        /**
         * 每轮经验。
         */
        private Integer expGain;

        /**
         * 暴击率。
         */
        private BigDecimal criticalRate;

        /**
         * 产出1个的概率。
         */
        private BigDecimal quantityChance1;

        /**
         * 产出2个的概率。
         */
        private BigDecimal quantityChance2;

        /**
         * 产出3个的概率。
         */
        private BigDecimal quantityChance3;

        /**
         * 候选奖励项列表。
         */
        private List<ActionRewardItemConfig> rewardItems;
    }

    /**
     * 单条候选奖励项配置。
     */
    @Data
    public static class ActionRewardItemConfig {

        /**
         * 物品编码。
         */
        private String itemCode;

        /**
         * 被抽中的概率。
         * 单位：百分比。
         */
        private BigDecimal chance;
    }
}