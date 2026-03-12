package com.urr.app.action.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.urr.domain.action.task.GatherTaskRewardEntry;
import com.urr.domain.action.task.GatherTaskRewardPool;
import com.urr.domain.action.task.PlayerGatherTask;
import com.urr.domain.item.ItemDefEntity;
import com.urr.domain.item.PlayerItemStackEntity;
import com.urr.domain.wallet.WalletEntity;
import com.urr.domain.wallet.WalletFlowEntity;
import com.urr.infra.mapper.ItemDefMapper;
import com.urr.infra.mapper.PlayerItemStackMapper;
import com.urr.infra.mapper.WalletFlowMapper;
import com.urr.infra.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采集收益正式入库适配器。
 *
 * 说明：
 * 1. 这里不是一套新的库存系统，只是把 pending_reward_pool 落到当前仓库已有正式表。
 * 2. ITEM 走玩家堆叠物品表。
 * 3. CURRENCY 走钱包表 + 钱包流水表。
 * 4. 当前只做 stop/replace 所需的最小入库能力。
 */
@Component
@RequiredArgsConstructor
public class GatherTaskRewardMaterializer {

    /**
     * 背包位置。
     */
    private static final int BAG_LOCATION = 1;

    /**
     * 钱包流水原因。
     */
    private static final String WALLET_FLOW_REASON = "GATHER_FLUSH";

    /**
     * 钱包流水关联类型。
     */
    private static final String WALLET_FLOW_REF_TYPE = "GATHER_TASK";

    /**
     * 物品定义 Mapper。
     */
    private final ItemDefMapper itemDefMapper;

    /**
     * 玩家堆叠物品 Mapper。
     */
    private final PlayerItemStackMapper playerItemStackMapper;

    /**
     * 钱包 Mapper。
     */
    private final WalletMapper walletMapper;

    /**
     * 钱包流水 Mapper。
     */
    private final WalletFlowMapper walletFlowMapper;

    /**
     * 把收益池正式写入库存。
     *
     * @param task 采集任务
     * @param rewardPool 待刷收益池
     * @return 实际写入的奖励条目数
     */
    public int materialize(PlayerGatherTask task, GatherTaskRewardPool rewardPool) {
        validateTask(task);
        if (rewardPool == null) {
            return 0;
        }

        int appliedCount = 0;
        List<GatherTaskRewardEntry> rewardList = rewardPool.getSafeRewardList();
        for (int i = 0; i < rewardList.size(); i++) {
            GatherTaskRewardEntry rewardEntry = rewardList.get(i);
            if (rewardEntry == null) {
                continue;
            }

            long quantity = rewardEntry.getQuantity() == null ? 0L : rewardEntry.getQuantity();
            if (quantity <= 0L) {
                continue;
            }

            applyOneReward(task, rewardEntry, quantity);
            appliedCount++;
        }
        return appliedCount;
    }

    /**
     * 应用一条奖励。
     *
     * @param task 采集任务
     * @param rewardEntry 奖励项
     * @param quantity 数量
     */
    private void applyOneReward(PlayerGatherTask task, GatherTaskRewardEntry rewardEntry, long quantity) {
        String rewardType = normalizeText(rewardEntry.getRewardType());
        String rewardCode = normalizeText(rewardEntry.getRewardCode());

        if (!StringUtils.hasText(rewardType)) {
            throw new IllegalArgumentException("奖励类型不能为空，taskId=" + task.getId());
        }
        if (!StringUtils.hasText(rewardCode)) {
            throw new IllegalArgumentException("奖励编码不能为空，taskId=" + task.getId());
        }

        if ("ITEM".equalsIgnoreCase(rewardType)) {
            addItemToBag(task, rewardCode, quantity);
            return;
        }
        if ("CURRENCY".equalsIgnoreCase(rewardType)) {
            addCurrency(task, rewardCode, quantity);
            return;
        }

        throw new IllegalArgumentException("暂不支持的奖励类型，rewardType=" + rewardType + ", taskId=" + task.getId());
    }

    /**
     * 把物品写入玩家背包。
     *
     * @param task 采集任务
     * @param itemCode 物品编码
     * @param quantity 数量
     */
    private void addItemToBag(PlayerGatherTask task, String itemCode, long quantity) {
        ItemDefEntity itemDef = requireItemDef(itemCode);
        validateItemCanUseStackTable(itemDef, task);

        PlayerItemStackEntity stackEntity = findItemStack(task.getPlayerId(), itemDef.getId(), BAG_LOCATION);
        if (stackEntity == null) {
            tryInsertNewItemStack(task, itemDef, quantity);
            return;
        }

        increaseExistingItemStack(stackEntity, quantity, task.getId(), itemCode);
    }

    /**
     * 查询物品定义。
     *
     * @param itemCode 物品编码
     * @return 物品定义
     */
    private ItemDefEntity requireItemDef(String itemCode) {
        ItemDefEntity itemDef = itemDefMapper.selectOne(
                new LambdaQueryWrapper<ItemDefEntity>()
                        .eq(ItemDefEntity::getItemCode, itemCode)
                        .last("limit 1")
        );
        if (itemDef == null) {
            throw new IllegalStateException("物品定义不存在，itemCode=" + itemCode);
        }
        return itemDef;
    }

    /**
     * 校验当前物品能否走堆叠物品表。
     *
     * @param itemDef 物品定义
     * @param task 采集任务
     */
    private void validateItemCanUseStackTable(ItemDefEntity itemDef, PlayerGatherTask task) {
        if (itemDef.getStackable() != null && itemDef.getStackable() == 0) {
            throw new IllegalStateException("当前最小 flush 仅支持堆叠物品，itemCode=" + itemDef.getItemCode() + ", taskId=" + task.getId());
        }
    }

    /**
     * 查询玩家物品堆叠记录。
     *
     * @param playerId 玩家ID
     * @param itemId 物品ID
     * @param location 位置
     * @return 堆叠记录
     */
    private PlayerItemStackEntity findItemStack(Long playerId, Long itemId, Integer location) {
        return playerItemStackMapper.selectOne(
                new LambdaQueryWrapper<PlayerItemStackEntity>()
                        .eq(PlayerItemStackEntity::getPlayerId, playerId)
                        .eq(PlayerItemStackEntity::getItemId, itemId)
                        .eq(PlayerItemStackEntity::getLocation, location)
                        .last("limit 1")
        );
    }

    /**
     * 尝试插入一条新的玩家物品堆叠记录。
     *
     * @param task 采集任务
     * @param itemDef 物品定义
     * @param quantity 数量
     */
    private void tryInsertNewItemStack(PlayerGatherTask task, ItemDefEntity itemDef, long quantity) {
        PlayerItemStackEntity stackEntity = new PlayerItemStackEntity();
        stackEntity.setPlayerId(task.getPlayerId());
        stackEntity.setServerId(task.getServerId());
        stackEntity.setItemId(itemDef.getId());
        stackEntity.setQty(quantity);
        stackEntity.setLocation(BAG_LOCATION);
        stackEntity.setRemarks(buildItemRemarks(task.getId(), itemDef.getItemCode(), quantity));

        try {
            int rows = playerItemStackMapper.insert(stackEntity);
            if (rows != 1) {
                throw new IllegalStateException("插入玩家物品堆叠记录失败，taskId=" + task.getId() + ", itemCode=" + itemDef.getItemCode());
            }
        } catch (DuplicateKeyException e) {
            PlayerItemStackEntity current = findItemStack(task.getPlayerId(), itemDef.getId(), BAG_LOCATION);
            if (current == null) {
                throw new IllegalStateException("玩家物品堆叠记录重复后仍未读到记录，taskId=" + task.getId() + ", itemCode=" + itemDef.getItemCode(), e);
            }
            increaseExistingItemStack(current, quantity, task.getId(), itemDef.getItemCode());
        }
    }

    /**
     * 增加已有玩家物品堆叠数量。
     *
     * @param stackEntity 当前堆叠记录
     * @param quantity 增加数量
     * @param taskId 任务ID
     * @param itemCode 物品编码
     */
    private void increaseExistingItemStack(PlayerItemStackEntity stackEntity, long quantity, Long taskId, String itemCode) {
        long oldQty = stackEntity.getQty() == null ? 0L : stackEntity.getQty();
        stackEntity.setQty(oldQty + quantity);
        stackEntity.setRemarks(buildItemRemarks(taskId, itemCode, quantity));

        int rows = playerItemStackMapper.updateById(stackEntity);
        if (rows != 1) {
            throw new IllegalStateException("更新玩家物品堆叠记录失败，taskId=" + taskId + ", itemCode=" + itemCode);
        }
    }

    /**
     * 把货币写入钱包并补流水。
     *
     * @param task 采集任务
     * @param currencyCode 币种
     * @param quantity 数量
     */
    private void addCurrency(PlayerGatherTask task, String currencyCode, long quantity) {
        long balanceAfter = increaseWalletBalance(task, currencyCode, quantity);
        insertWalletFlow(task, currencyCode, quantity, balanceAfter);
    }

    /**
     * 增加钱包余额。
     *
     * @param task 采集任务
     * @param currencyCode 币种
     * @param quantity 数量
     * @return 变更后余额
     */
    private long increaseWalletBalance(PlayerGatherTask task, String currencyCode, long quantity) {
        WalletEntity walletEntity = findWallet(task.getPlayerId(), currencyCode);
        if (walletEntity == null) {
            return tryInsertNewWallet(task, currencyCode, quantity);
        }

        return increaseExistingWallet(walletEntity, currencyCode, quantity, task.getId());
    }

    /**
     * 查询钱包。
     *
     * @param playerId 玩家ID
     * @param currencyCode 币种
     * @return 钱包记录
     */
    private WalletEntity findWallet(Long playerId, String currencyCode) {
        return walletMapper.selectOne(
                new LambdaQueryWrapper<WalletEntity>()
                        .eq(WalletEntity::getPlayerId, playerId)
                        .eq(WalletEntity::getCurrencyCode, currencyCode)
                        .last("limit 1")
        );
    }

    /**
     * 尝试插入一条新钱包。
     *
     * @param task 采集任务
     * @param currencyCode 币种
     * @param quantity 数量
     * @return 变更后余额
     */
    private long tryInsertNewWallet(PlayerGatherTask task, String currencyCode, long quantity) {
        WalletEntity walletEntity = new WalletEntity();
        walletEntity.setPlayerId(task.getPlayerId());
        walletEntity.setServerId(task.getServerId());
        walletEntity.setCurrencyCode(currencyCode);
        walletEntity.setBalance(quantity);
        walletEntity.setRemarks(buildWalletRemarks(task.getId(), currencyCode, quantity));

        try {
            int rows = walletMapper.insert(walletEntity);
            if (rows != 1) {
                throw new IllegalStateException("插入钱包失败，taskId=" + task.getId() + ", currencyCode=" + currencyCode);
            }
            return quantity;
        } catch (DuplicateKeyException e) {
            WalletEntity current = findWallet(task.getPlayerId(), currencyCode);
            if (current == null) {
                throw new IllegalStateException("钱包记录重复后仍未读到记录，taskId=" + task.getId() + ", currencyCode=" + currencyCode, e);
            }
            return increaseExistingWallet(current, currencyCode, quantity, task.getId());
        }
    }

    /**
     * 增加已有钱包余额。
     *
     * @param walletEntity 钱包记录
     * @param currencyCode 币种
     * @param quantity 增加数量
     * @param taskId 任务ID
     * @return 变更后余额
     */
    private long increaseExistingWallet(WalletEntity walletEntity, String currencyCode, long quantity, Long taskId) {
        long oldBalance = walletEntity.getBalance() == null ? 0L : walletEntity.getBalance();
        long newBalance = oldBalance + quantity;
        walletEntity.setBalance(newBalance);
        walletEntity.setRemarks(buildWalletRemarks(taskId, currencyCode, quantity));

        int rows = walletMapper.updateById(walletEntity);
        if (rows != 1) {
            throw new IllegalStateException("更新钱包失败，taskId=" + taskId + ", currencyCode=" + currencyCode);
        }
        return newBalance;
    }

    /**
     * 插入钱包流水。
     *
     * @param task 采集任务
     * @param currencyCode 币种
     * @param quantity 增加数量
     * @param balanceAfter 变更后余额
     */
    private void insertWalletFlow(PlayerGatherTask task, String currencyCode, long quantity, long balanceAfter) {
        WalletFlowEntity flowEntity = new WalletFlowEntity();
        flowEntity.setPlayerId(task.getPlayerId());
        flowEntity.setServerId(task.getServerId());
        flowEntity.setCurrencyCode(currencyCode);
        flowEntity.setDelta(quantity);
        flowEntity.setBalanceAfter(balanceAfter);
        flowEntity.setReason(WALLET_FLOW_REASON);
        flowEntity.setRefType(WALLET_FLOW_REF_TYPE);
        flowEntity.setRefId(task.getId());
        flowEntity.setRequestId(buildWalletRequestId(task.getId(), currencyCode, task.getSafeCompletedCount()));
        flowEntity.setFlowTime(LocalDateTime.now());
        flowEntity.setRemarks(buildWalletRemarks(task.getId(), currencyCode, quantity));

        int rows = walletFlowMapper.insert(flowEntity);
        if (rows != 1) {
            throw new IllegalStateException("插入钱包流水失败，taskId=" + task.getId() + ", currencyCode=" + currencyCode);
        }
    }

    /**
     * 构建物品备注。
     *
     * @param taskId 任务ID
     * @param itemCode 物品编码
     * @param quantity 数量
     * @return 备注
     */
    private String buildItemRemarks(Long taskId, String itemCode, long quantity) {
        return "gather_flush taskId=" + taskId + ", itemCode=" + itemCode + ", qty=" + quantity;
    }

    /**
     * 构建钱包备注。
     *
     * @param taskId 任务ID
     * @param currencyCode 币种
     * @param quantity 数量
     * @return 备注
     */
    private String buildWalletRemarks(Long taskId, String currencyCode, long quantity) {
        return "gather_flush taskId=" + taskId + ", currencyCode=" + currencyCode + ", delta=" + quantity;
    }

    /**
     * 构建钱包流水请求号。
     *
     * @param taskId 任务ID
     * @param currencyCode 币种
     * @param completedCount 当前 completedCount
     * @return 请求号
     */
    private String buildWalletRequestId(Long taskId, String currencyCode, long completedCount) {
        return "gather-flush-" + taskId + "-" + currencyCode + "-" + completedCount;
    }

    /**
     * 规范化文本。
     *
     * @param value 原值
     * @return 规范化后的值
     */
    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 校验采集任务。
     *
     * @param task 采集任务
     */
    private void validateTask(PlayerGatherTask task) {
        if (task == null) {
            throw new IllegalArgumentException("采集任务不能为空");
        }
        if (task.getId() == null) {
            throw new IllegalArgumentException("taskId不能为空");
        }
        if (task.getPlayerId() == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
        if (task.getServerId() == null) {
            throw new IllegalArgumentException("serverId不能为空");
        }
    }
}