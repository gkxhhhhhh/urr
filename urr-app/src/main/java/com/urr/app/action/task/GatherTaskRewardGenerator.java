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
 * 1. 本次不再硬编码动作到物品的映射。
 * 2. 所有采集奖励配置都在项目启动时从 t_urr_action_def.params_json 读取并缓存。
 * 3. 任务启动时会把关键奖励字段锁进快照；结算时优先用快照，兼容旧任务时再回退缓存。
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
        for (ActionDefEntity action : actionList) {
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
        long quantity = calculateItemQuantity(task, roundIndex, config);
        List<GatherTaskRewardEntry> rewardList = new ArrayList<GatherTaskRewardEntry>();
        if (quantity <= 0L) {
            return rewardList;
        }

        GatherTaskRewardEntry rewardEntry = new GatherTaskRewardEntry();
        rewardEntry.setRewardType(REWARD_TYPE_ITEM);
        rewardEntry.setRewardCode(config.getItemCode());
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
     * 尝试从动作定义构建奖励配置。
     *
     * @param action 动作定义
     * @return 奖励配置；未配置 itemCode 时返回 null
     */
    private ActionRewardConfig tryBuildRewardConfig(ActionDefEntity action) {
        if (action == null || !StringUtils.hasText(action.getActionCode())) {
            return null;
        }

        JsonNode root = readParamsNode(action);
        if (root == null) {
            return null;
        }

        String itemCode = readText(root, "itemCode");
        if (!StringUtils.hasText(itemCode)) {
            return null;
        }

        ActionRewardConfig config = new ActionRewardConfig();
        config.setActionCode(action.getActionCode().trim());
        config.setItemCode(itemCode);
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
        config.setItemCode(snapshot.getItemCode());
        config.setSkillCode(snapshot.getSkillCode());
        config.setExpGain(snapshot.getExpGain());
        config.setCriticalRate(snapshot.getCriticalRate());
        config.setQuantityChance1(snapshot.getQuantityChance1());
        config.setQuantityChance2(snapshot.getQuantityChance2());
        config.setQuantityChance3(snapshot.getQuantityChance3());
        validateRewardConfig(config);
        return config;
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
            extra++;
            remaining = remaining.subtract(FULL_PERCENT);
        }

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return extra;
        }

        long remainderBasisPoint = toBasisPoint(remaining);
        long rollBasisPoint = nextPositiveValue(task.getRewardSeed(), roundIndex, 2) % 10000L;
        if (rollBasisPoint < remainderBasisPoint) {
            extra++;
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
        if (!StringUtils.hasText(config.getItemCode())) {
            throw new IllegalStateException("奖励配置缺少 itemCode");
        }
        if (config.getQuantityChance1() == null || config.getQuantityChance2() == null || config.getQuantityChance3() == null) {
            throw new IllegalStateException("奖励配置缺少基础爆率");
        }
        BigDecimal total = config.getQuantityChance1()
                .add(config.getQuantityChance2())
                .add(config.getQuantityChance3());
        if (total.compareTo(FULL_PERCENT) != 0) {
            throw new IllegalStateException("奖励配置基础爆率之和必须等于100，itemCode=" + config.getItemCode());
        }
        if (config.getCriticalRate() == null) {
            config.setCriticalRate(BigDecimal.ZERO);
        }
        if (config.getExpGain() == null) {
            config.setExpGain(0);
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
        return copy;
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
         * 产出物编码。
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
    }
}
