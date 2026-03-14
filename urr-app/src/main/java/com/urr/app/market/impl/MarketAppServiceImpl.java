package com.urr.app.market.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.urr.app.market.MarketAppService;
import com.urr.app.market.result.MarketCancelOrderResult;
import com.urr.app.market.result.MarketCreateOrderResult;
import com.urr.app.market.result.MarketInventoryItemView;
import com.urr.app.market.result.MarketInventoryResult;
import com.urr.app.market.result.MarketOrderPageResult;
import com.urr.app.market.result.MarketOrderView;
import com.urr.app.market.result.MarketTradePageResult;
import com.urr.app.market.result.MarketTradeResult;
import com.urr.app.market.result.MarketTradeView;
import com.urr.app.market.result.MarketItemCatalogResult;
import com.urr.app.market.result.MarketItemCatalogView;
import com.urr.domain.item.ItemDefEntity;
import com.urr.domain.item.PlayerItemStackEntity;
import com.urr.domain.market.MarketOrderEntity;
import com.urr.domain.market.MarketOrderStatusEnum;
import com.urr.domain.market.MarketOrderTypeEnum;
import com.urr.domain.market.MarketTradeEntity;
import com.urr.domain.player.PlayerEntity;
import com.urr.domain.wallet.WalletEntity;
import com.urr.domain.wallet.WalletFlowEntity;
import com.urr.infra.mapper.ItemDefMapper;
import com.urr.infra.mapper.MarketOrderMapper;
import com.urr.infra.mapper.MarketTradeMapper;
import com.urr.infra.mapper.PlayerItemStackMapper;
import com.urr.infra.mapper.PlayerMapper;
import com.urr.infra.mapper.WalletFlowMapper;
import com.urr.infra.mapper.WalletMapper;
import com.urr.commons.exception.BizException;
import com.urr.commons.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 市场应用服务实现。
 */
@Service
@RequiredArgsConstructor
public class MarketAppServiceImpl implements MarketAppService {

    /**
     * 背包正常位。
     */
    private static final int BAG_LOCATION = 1;

    /**
     * 背包冻结位。
     */
    private static final int FROZEN_LOCATION = 3;

    /**
     * 固定币种。
     */
    private static final String CURRENCY_GOLD = "GOLD";

    /**
     * 创建人默认值。
     */
    private static final String SYSTEM_USER = "-1";

    /**
     * 物品 Mapper。
     */
    private final ItemDefMapper itemDefMapper;

    /**
     * 玩家背包 Mapper。
     */
    private final PlayerItemStackMapper playerItemStackMapper;

    /**
     * 玩家 Mapper。
     */
    private final PlayerMapper playerMapper;

    /**
     * 钱包 Mapper。
     */
    private final WalletMapper walletMapper;

    /**
     * 钱包流水 Mapper。
     */
    private final WalletFlowMapper walletFlowMapper;

    /**
     * 市场订单 Mapper。
     */
    private final MarketOrderMapper marketOrderMapper;

    /**
     * 市场成交 Mapper。
     */
    private final MarketTradeMapper marketTradeMapper;

    /**
     * 查询可挂牌库存。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @return 可挂牌库存
     */
    @Override
    public MarketInventoryResult queryTradableInventory(Long accountId, Long playerId) {
        PlayerEntity player = loadPlayer(accountId, playerId);

        LambdaQueryWrapper<PlayerItemStackEntity> stackQuery = new LambdaQueryWrapper<>();
        stackQuery.eq(PlayerItemStackEntity::getPlayerId, player.getId());
        stackQuery.eq(PlayerItemStackEntity::getServerId, player.getServerId());
        stackQuery.eq(PlayerItemStackEntity::getLocation, BAG_LOCATION);
        stackQuery.gt(PlayerItemStackEntity::getQty, 0L);
        List<PlayerItemStackEntity> stacks = playerItemStackMapper.selectList(stackQuery);
        if (stacks == null || stacks.isEmpty()) {
            return new MarketInventoryResult();
        }

        Map<Long, ItemDefEntity> itemMap = loadItemMap(extractItemIdsFromStacks(stacks));

        List<MarketInventoryItemView> resultList = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i++) {
            PlayerItemStackEntity stack = stacks.get(i);
            ItemDefEntity itemDef = itemMap.get(stack.getItemId());
            if (!canTradeInMarket(itemDef)) {
                continue;
            }

            MarketInventoryItemView view = new MarketInventoryItemView();
            view.setItemId(stack.getItemId());
            view.setItemName(itemDef.getNameZh());
            view.setBagQty(stack.getQty());
            resultList.add(view);
        }

        MarketInventoryResult result = new MarketInventoryResult();
        result.setCount(resultList.size());
        result.setList(resultList);
        return result;
    }


    @Override
    public MarketItemCatalogResult queryTradableItems(Long accountId, Long playerId) {
        PlayerEntity player = loadPlayer(accountId, playerId);

        LambdaQueryWrapper<ItemDefEntity> query = new LambdaQueryWrapper<>();
        query.eq(ItemDefEntity::getTradeable, 1);
        query.eq(ItemDefEntity::getStackable, 1);
        query.and(wrapper -> wrapper.isNull(ItemDefEntity::getBindType).or().eq(ItemDefEntity::getBindType, 0));
        query.orderByAsc(ItemDefEntity::getItemType)
                .orderByAsc(ItemDefEntity::getId);

        List<ItemDefEntity> items = itemDefMapper.selectList(query);
        List<MarketItemCatalogView> resultList = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemDefEntity item = items.get(i);
            if (!canTradeInMarket(item)) {
                continue;
            }

            MarketItemCatalogView view = new MarketItemCatalogView();
            view.setItemId(item.getId());
            view.setItemCode(item.getItemCode());
            view.setItemName(item.getNameZh());
            view.setItemType(item.getItemType());
            view.setRarity(item.getRarity());
            view.setMetaJson(item.getMetaJson());
            resultList.add(view);
        }

        MarketItemCatalogResult result = new MarketItemCatalogResult();
        result.setCount((long) resultList.size());
        result.setList(resultList);
        return result;
    }

    /**
     * 查询市场列表。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @param orderTypeCode 订单类型
     * @param pageNo 页码
     * @param pageSize 页大小
     * @return 市场分页结果
     */
    @Override
    public MarketOrderPageResult queryMarketOrders(Long accountId, Long playerId, String orderTypeCode, Long itemId, Integer pageNo, Integer pageSize) {
        PlayerEntity player = loadPlayer(accountId, playerId);
        MarketOrderTypeEnum orderType = requireOrderType(orderTypeCode);

        Page<MarketOrderEntity> page = new Page<>(normalizePageNo(pageNo), normalizePageSize(pageSize));

        LambdaQueryWrapper<MarketOrderEntity> query = new LambdaQueryWrapper<>();
        query.eq(MarketOrderEntity::getServerId, player.getServerId());
        query.eq(MarketOrderEntity::getOrderType, orderType.getDbValue());
        query.in(MarketOrderEntity::getStatus,
                MarketOrderStatusEnum.LISTED.getDbValue(),
                MarketOrderStatusEnum.PARTIAL.getDbValue());
        query.gt(MarketOrderEntity::getQtyRemain, 0L);
        query.and(wrapper -> wrapper.isNull(MarketOrderEntity::getExpireTime)
                .or()
                .gt(MarketOrderEntity::getExpireTime, LocalDateTime.now()));
        if (itemId != null) {
            validatePositive(itemId, "itemId必须大于0");
            query.eq(MarketOrderEntity::getItemId, itemId);
        }
        if (MarketOrderTypeEnum.SELL.equals(orderType)) {
            query.orderByAsc(MarketOrderEntity::getPriceEach)
                    .orderByAsc(MarketOrderEntity::getCreateTime);
        } else {
            query.orderByDesc(MarketOrderEntity::getPriceEach)
                    .orderByAsc(MarketOrderEntity::getCreateTime);
        }

        Page<MarketOrderEntity> orderPage = marketOrderMapper.selectPage(page, query);
        return buildOrderPageResult(orderPage.getTotal(), orderPage.getRecords());
    }

    /**
     * 查询我的订单。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @param orderTypeCode 订单类型
     * @param pageNo 页码
     * @param pageSize 页大小
     * @return 分页结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketOrderPageResult queryMyOrders(Long accountId, Long playerId, String orderTypeCode, Integer pageNo, Integer pageSize) {
        PlayerEntity player = loadPlayer(accountId, playerId);
        MarketOrderTypeEnum orderType = requireOrderType(orderTypeCode);

        Page<MarketOrderEntity> page = new Page<>(normalizePageNo(pageNo), normalizePageSize(pageSize));
        LambdaQueryWrapper<MarketOrderEntity> query = new LambdaQueryWrapper<>();
        query.eq(MarketOrderEntity::getServerId, player.getServerId());
        query.eq(MarketOrderEntity::getOrderType, orderType.getDbValue());

        if (MarketOrderTypeEnum.SELL.equals(orderType)) {
            query.eq(MarketOrderEntity::getSellerId, player.getId());
        } else {
            query.eq(MarketOrderEntity::getBuyerId, player.getId());
        }

        query.orderByDesc(MarketOrderEntity::getCreateTime);
        Page<MarketOrderEntity> orderPage = marketOrderMapper.selectPage(page, query);

        List<MarketOrderEntity> records = orderPage.getRecords();
        for (int i = 0; i < records.size(); i++) {
            expireOrderIfNeeded(records.get(i));
        }

        Page<MarketOrderEntity> refreshedPage = marketOrderMapper.selectPage(page, query);
        return buildOrderPageResult(refreshedPage.getTotal(), refreshedPage.getRecords());
    }

    /**
     * 查询我的成交记录。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @param pageNo 页码
     * @param pageSize 页大小
     * @return 分页结果
     */
    @Override
    public MarketTradePageResult queryMyTrades(Long accountId, Long playerId, Integer pageNo, Integer pageSize) {
        PlayerEntity player = loadPlayer(accountId, playerId);

        Page<MarketTradeEntity> page = new Page<>(normalizePageNo(pageNo), normalizePageSize(pageSize));
        LambdaQueryWrapper<MarketTradeEntity> query = new LambdaQueryWrapper<>();
        query.eq(MarketTradeEntity::getServerId, player.getServerId());
        query.and(wrapper -> wrapper.eq(MarketTradeEntity::getBuyerId, player.getId())
                .or()
                .eq(MarketTradeEntity::getSellerId, player.getId()));
        query.orderByDesc(MarketTradeEntity::getTradeTime);

        Page<MarketTradeEntity> tradePage = marketTradeMapper.selectPage(page, query);
        return buildTradePageResult(tradePage.getTotal(), tradePage.getRecords());
    }

    /**
     * 创建卖单。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @param itemId 物品ID
     * @param qty 数量
     * @param priceEach 单价
     * @param expireMinutes 过期分钟数
     * @return 创建结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketCreateOrderResult createSellOrder(Long accountId, Long playerId, Long itemId, Long qty, Long priceEach, Long expireMinutes) {
        validatePositive(qty, "卖单数量必须大于0");
        validatePositive(priceEach, "卖单单价必须大于0");
        validateOptionalPositive(expireMinutes, "过期分钟数必须大于0");

        PlayerEntity player = loadPlayer(accountId, playerId);
        ItemDefEntity itemDef = loadTradableItem(itemId);

        decreaseStack(player.getId(), player.getServerId(), itemDef.getId(), BAG_LOCATION, qty);
        increaseStack(player.getId(), player.getServerId(), itemDef.getId(), FROZEN_LOCATION, qty);

        MarketOrderEntity order = new MarketOrderEntity();
        order.setServerId(player.getServerId());
        order.setOrderType(MarketOrderTypeEnum.SELL.getDbValue());
        order.setSellerId(player.getId());
        order.setBuyerId(null);
        order.setItemId(itemDef.getId());
        order.setEquipInstanceId(null);
        order.setQtyTotal(qty);
        order.setQtyRemain(qty);
        order.setPriceEach(priceEach);
        order.setCurrencyCode(CURRENCY_GOLD);
        order.setFeeRateBp(0);
        order.setStatus(MarketOrderStatusEnum.LISTED.getDbValue());
        order.setExpireTime(buildExpireTime(expireMinutes));
        order.setCreateUser(SYSTEM_USER);
        order.setUpdateUser(SYSTEM_USER);

        marketOrderMapper.insert(order);

        MarketCreateOrderResult result = new MarketCreateOrderResult();
        result.setOrderId(order.getId());
        result.setOrderType(MarketOrderTypeEnum.SELL.getCode());
        result.setStatus(MarketOrderStatusEnum.LISTED.getCode());
        result.setQtyTotal(order.getQtyTotal());
        result.setQtyRemain(order.getQtyRemain());
        return result;
    }

    /**
     * 购买卖单。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @param orderId 订单ID
     * @param qty 成交数量
     * @return 成交结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketTradeResult buySellOrder(Long accountId, Long playerId, Long orderId, Long qty) {
        validatePositive(qty, "购买数量必须大于0");

        PlayerEntity buyer = loadPlayer(accountId, playerId);
        MarketOrderEntity order = loadOrder(orderId);
        expireOrderIfNeeded(order);
        ensureOpenOrder(order, MarketOrderTypeEnum.SELL, buyer.getServerId());

        if (buyer.getId().equals(order.getSellerId())) {
            throwConflict("不能购买自己的卖单");
        }
        if (qty.longValue() > order.getQtyRemain().longValue()) {
            throwConflict("购买数量超过剩余数量");
        }

        Long totalAmount = safeMultiply(qty, order.getPriceEach());

        deductWallet(buyer.getId(), buyer.getServerId(), totalAmount, "MARKET_BUY", "ORDER", order.getId());
        creditWallet(order.getSellerId(), buyer.getServerId(), totalAmount, "MARKET_SELL", "ORDER", order.getId());

        decreaseStack(order.getSellerId(), buyer.getServerId(), order.getItemId(), FROZEN_LOCATION, qty);
        increaseStack(buyer.getId(), buyer.getServerId(), order.getItemId(), BAG_LOCATION, qty);

        MarketTradeEntity trade = createTrade(order, order.getSellerId(), buyer.getId(), qty, totalAmount);
        updateOrderAfterTrade(order, qty);

        MarketTradeResult result = new MarketTradeResult();
        result.setTradeId(trade.getId());
        result.setOrderId(order.getId());
        result.setOrderStatus(MarketOrderStatusEnum.fromDbValue(order.getStatus()).getCode());
        result.setTradeQty(qty);
        result.setTotalAmount(totalAmount);
        return result;
    }

    /**
     * 创建买单。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @param itemId 物品ID
     * @param qty 数量
     * @param priceEach 单价
     * @param expireMinutes 过期分钟数
     * @return 创建结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketCreateOrderResult createBuyOrder(Long accountId, Long playerId, Long itemId, Long qty, Long priceEach, Long expireMinutes) {
        validatePositive(qty, "求购数量必须大于0");
        validatePositive(priceEach, "求购单价必须大于0");
        validateOptionalPositive(expireMinutes, "过期分钟数必须大于0");

        PlayerEntity buyer = loadPlayer(accountId, playerId);
        ItemDefEntity itemDef = loadTradableItem(itemId);

        MarketOrderEntity order = new MarketOrderEntity();
        order.setServerId(buyer.getServerId());
        order.setOrderType(MarketOrderTypeEnum.BUY.getDbValue());
        order.setSellerId(null);
        order.setBuyerId(buyer.getId());
        order.setItemId(itemDef.getId());
        order.setEquipInstanceId(null);
        order.setQtyTotal(qty);
        order.setQtyRemain(qty);
        order.setPriceEach(priceEach);
        order.setCurrencyCode(CURRENCY_GOLD);
        order.setFeeRateBp(0);
        order.setStatus(MarketOrderStatusEnum.LISTED.getDbValue());
        order.setExpireTime(buildExpireTime(expireMinutes));
        order.setCreateUser(SYSTEM_USER);
        order.setUpdateUser(SYSTEM_USER);

        marketOrderMapper.insert(order);

        Long holdAmount = safeMultiply(qty, priceEach);
        deductWallet(buyer.getId(), buyer.getServerId(), holdAmount, "MARKET_BUY", "ORDER", order.getId());

        MarketCreateOrderResult result = new MarketCreateOrderResult();
        result.setOrderId(order.getId());
        result.setOrderType(MarketOrderTypeEnum.BUY.getCode());
        result.setStatus(MarketOrderStatusEnum.LISTED.getCode());
        result.setQtyTotal(order.getQtyTotal());
        result.setQtyRemain(order.getQtyRemain());
        return result;
    }

    /**
     * 卖给买单。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @param orderId 订单ID
     * @param qty 成交数量
     * @return 成交结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketTradeResult sellToBuyOrder(Long accountId, Long playerId, Long orderId, Long qty) {
        validatePositive(qty, "出售数量必须大于0");

        PlayerEntity seller = loadPlayer(accountId, playerId);
        MarketOrderEntity order = loadOrder(orderId);
        expireOrderIfNeeded(order);
        ensureOpenOrder(order, MarketOrderTypeEnum.BUY, seller.getServerId());

        if (seller.getId().equals(order.getBuyerId())) {
            throwConflict("不能卖给自己的买单");
        }
        if (qty.longValue() > order.getQtyRemain().longValue()) {
            throwConflict("出售数量超过剩余数量");
        }

        decreaseStack(seller.getId(), seller.getServerId(), order.getItemId(), BAG_LOCATION, qty);
        increaseStack(order.getBuyerId(), seller.getServerId(), order.getItemId(), BAG_LOCATION, qty);

        Long totalAmount = safeMultiply(qty, order.getPriceEach());
        creditWallet(seller.getId(), seller.getServerId(), totalAmount, "MARKET_SELL", "ORDER", order.getId());

        MarketTradeEntity trade = createTrade(order, seller.getId(), order.getBuyerId(), qty, totalAmount);
        updateOrderAfterTrade(order, qty);

        MarketTradeResult result = new MarketTradeResult();
        result.setTradeId(trade.getId());
        result.setOrderId(order.getId());
        result.setOrderStatus(MarketOrderStatusEnum.fromDbValue(order.getStatus()).getCode());
        result.setTradeQty(qty);
        result.setTotalAmount(totalAmount);
        return result;
    }

    /**
     * 取消订单。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @param orderId 订单ID
     * @return 取消结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketCancelOrderResult cancelOrder(Long accountId, Long playerId, Long orderId) {
        PlayerEntity player = loadPlayer(accountId, playerId);
        MarketOrderEntity order = loadOrder(orderId);
        expireOrderIfNeeded(order);

        MarketOrderTypeEnum orderType = MarketOrderTypeEnum.fromDbValue(order.getOrderType());
        ensureOpenOrder(order, orderType, player.getServerId());

        if (MarketOrderTypeEnum.SELL.equals(orderType)) {
            if (!player.getId().equals(order.getSellerId())) {
                throwConflict("只能取消自己的订单");
            }
        } else {
            if (!player.getId().equals(order.getBuyerId())) {
                throwConflict("只能取消自己的订单");
            }
        }

        Long remainQty = order.getQtyRemain();
        Long returnedGoldAmount = 0L;
        Long returnedItemQty = 0L;

        if (remainQty != null && remainQty.longValue() > 0L) {
            if (MarketOrderTypeEnum.SELL.equals(orderType)) {
                decreaseStack(player.getId(), player.getServerId(), order.getItemId(), FROZEN_LOCATION, remainQty);
                increaseStack(player.getId(), player.getServerId(), order.getItemId(), BAG_LOCATION, remainQty);
                returnedItemQty = remainQty;
            } else {
                returnedGoldAmount = safeMultiply(remainQty, order.getPriceEach());
                creditWallet(player.getId(), player.getServerId(), returnedGoldAmount, "MARKET_BUY_REFUND", "ORDER", order.getId());
            }
        }

        order.setStatus(MarketOrderStatusEnum.CANCELLED.getDbValue());
        order.setUpdateUser(SYSTEM_USER);
        if (marketOrderMapper.updateById(order) <= 0) {
            throwConflict("订单状态已变化，请刷新后重试");
        }

        MarketCancelOrderResult result = new MarketCancelOrderResult();
        result.setOrderId(order.getId());
        result.setStatus(MarketOrderStatusEnum.CANCELLED.getCode());
        result.setReturnedGoldAmount(returnedGoldAmount);
        result.setReturnedItemQty(returnedItemQty);
        return result;
    }

    /**
     * 加载并校验玩家归属。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @return 玩家
     */
    private PlayerEntity loadPlayer(Long accountId, Long playerId) {
        validatePositive(playerId, "playerId必须大于0");
        PlayerEntity player = playerMapper.selectById(playerId);
        if (player == null) {
            throwNotFound("玩家不存在");
        }
        if (!player.getAccountId().equals(accountId)) {
            throwConflict("玩家不属于当前账号");
        }
        return player;
    }

    /**
     * 加载订单。
     *
     * @param orderId 订单ID
     * @return 订单
     */
    private MarketOrderEntity loadOrder(Long orderId) {
        validatePositive(orderId, "orderId必须大于0");
        MarketOrderEntity order = marketOrderMapper.selectById(orderId);
        if (order == null) {
            throwNotFound("订单不存在");
        }
        return order;
    }

    /**
     * 校验订单仍可操作。
     *
     * @param order 订单
     * @param orderType 订单类型
     * @param serverId 区服ID
     */
    private void ensureOpenOrder(MarketOrderEntity order, MarketOrderTypeEnum orderType, Integer serverId) {
        if (!serverId.equals(order.getServerId())) {
            throwConflict("订单不属于当前角色所在区服");
        }
        if (orderType.getDbValue() != order.getOrderType()) {
            throwParamInvalid("订单类型不匹配");
        }
        if (order.getQtyRemain() == null || order.getQtyRemain().longValue() <= 0L) {
            throwConflict("订单已无剩余数量");
        }
        MarketOrderStatusEnum status = MarketOrderStatusEnum.fromDbValue(order.getStatus());
        if (MarketOrderStatusEnum.CANCELLED.equals(status)) {
            throwConflict("订单已取消，请勿重复操作");
        }
        if (MarketOrderStatusEnum.COMPLETED.equals(status)) {
            throwConflict("订单已完成，请刷新后重试");
        }
        if (MarketOrderStatusEnum.EXPIRED.equals(status)) {
            throwConflict("订单已过期，请刷新后重试");
        }
        if (!MarketOrderStatusEnum.LISTED.equals(status) && !MarketOrderStatusEnum.PARTIAL.equals(status)) {
            throwConflict("当前订单状态不可操作");
        }
    }

    /**
     * 如有必要，懒处理订单过期。
     *
     * @param order 订单
     */
    private void expireOrderIfNeeded(MarketOrderEntity order) {
        if (order == null) {
            return;
        }
        MarketOrderStatusEnum status = MarketOrderStatusEnum.fromDbValue(order.getStatus());
        if (!MarketOrderStatusEnum.LISTED.equals(status) && !MarketOrderStatusEnum.PARTIAL.equals(status)) {
            return;
        }
        if (order.getExpireTime() == null || !order.getExpireTime().isBefore(LocalDateTime.now())) {
            return;
        }

        Long remainQty = order.getQtyRemain() == null ? 0L : order.getQtyRemain();
        MarketOrderTypeEnum orderType = MarketOrderTypeEnum.fromDbValue(order.getOrderType());

        if (remainQty.longValue() > 0L) {
            if (MarketOrderTypeEnum.SELL.equals(orderType)) {
                decreaseStack(order.getSellerId(), order.getServerId(), order.getItemId(), FROZEN_LOCATION, remainQty);
                increaseStack(order.getSellerId(), order.getServerId(), order.getItemId(), BAG_LOCATION, remainQty);
            } else {
                Long refundAmount = safeMultiply(remainQty, order.getPriceEach());
                creditWallet(order.getBuyerId(), order.getServerId(), refundAmount, "MARKET_BUY_REFUND", "ORDER", order.getId());
            }
        }

        order.setStatus(MarketOrderStatusEnum.EXPIRED.getDbValue());
        order.setUpdateUser(SYSTEM_USER);
        if (marketOrderMapper.updateById(order) <= 0) {
            throwConflict("订单状态已变化，请刷新后重试");
        }
    }

    /**
     * 更新订单成交后的剩余量与状态。
     *
     * @param order 订单
     * @param qty 本次成交数量
     */
    private void updateOrderAfterTrade(MarketOrderEntity order, Long qty) {
        long remain = order.getQtyRemain().longValue() - qty.longValue();
        order.setQtyRemain(remain);
        if (remain <= 0L) {
            order.setStatus(MarketOrderStatusEnum.COMPLETED.getDbValue());
        } else {
            order.setStatus(MarketOrderStatusEnum.PARTIAL.getDbValue());
        }
        order.setUpdateUser(SYSTEM_USER);
        if (marketOrderMapper.updateById(order) <= 0) {
            throwConflict("订单状态已变化，请刷新后重试");
        }
    }

    /**
     * 创建成交记录。
     *
     * @param order 订单
     * @param sellerId 卖家ID
     * @param buyerId 买家ID
     * @param qty 数量
     * @param totalAmount 总额
     * @return 成交记录
     */
    private MarketTradeEntity createTrade(MarketOrderEntity order, Long sellerId, Long buyerId, Long qty, Long totalAmount) {
        MarketTradeEntity trade = new MarketTradeEntity();
        trade.setServerId(order.getServerId());
        trade.setOrderId(order.getId());
        trade.setSellerId(sellerId);
        trade.setBuyerId(buyerId);
        trade.setItemId(order.getItemId());
        trade.setEquipInstanceId(null);
        trade.setQty(qty);
        trade.setPriceEach(order.getPriceEach());
        trade.setCurrencyCode(order.getCurrencyCode());
        trade.setFeeAmount(0L);
        trade.setTotalAmount(totalAmount);
        trade.setTradeTime(LocalDateTime.now());
        trade.setCreateUser(SYSTEM_USER);
        trade.setUpdateUser(SYSTEM_USER);
        marketTradeMapper.insert(trade);
        return trade;
    }

    /**
     * 加载并校验可交易堆叠物。
     *
     * @param itemId 物品ID
     * @return 物品定义
     */
    private ItemDefEntity loadTradableItem(Long itemId) {
        validatePositive(itemId, "itemId必须大于0");
        ItemDefEntity itemDef = itemDefMapper.selectById(itemId);
        if (itemDef == null) {
            throwNotFound("物品不存在");
        }
        if (!canTradeInMarket(itemDef)) {
            throwConflict("该物品当前不允许进入市场");
        }
        return itemDef;
    }

    /**
     * 判断物品是否允许进入市场。
     *
     * @param itemDef 物品定义
     * @return 是否允许
     */
    private boolean canTradeInMarket(ItemDefEntity itemDef) {
        if (itemDef == null) {
            return false;
        }
        if (itemDef.getStackable() == null || itemDef.getStackable().intValue() != 1) {
            return false;
        }
        if (itemDef.getTradeable() == null || itemDef.getTradeable().intValue() != 1) {
            return false;
        }
        return itemDef.getBindType() == null || itemDef.getBindType().intValue() == 0;
    }

    /**
     * 扣减某个位置的堆叠数量。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @param itemId 物品ID
     * @param location 位置
     * @param qty 数量
     */
    private void decreaseStack(Long playerId, Integer serverId, Long itemId, Integer location, Long qty) {
        PlayerItemStackEntity stack = findStack(playerId, serverId, itemId, location);
        if (stack == null || stack.getQty() == null || stack.getQty().longValue() < qty.longValue()) {
            throwConflict("库存不足");
        }

        stack.setQty(stack.getQty() - qty);
        stack.setUpdateUser(SYSTEM_USER);
        if (playerItemStackMapper.updateById(stack) <= 0) {
            throwConflict("库存已变化，请刷新后重试");
        }
    }

    /**
     * 增加某个位置的堆叠数量。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @param itemId 物品ID
     * @param location 位置
     * @param qty 数量
     */
    private void increaseStack(Long playerId, Integer serverId, Long itemId, Integer location, Long qty) {
        PlayerItemStackEntity stack = findStack(playerId, serverId, itemId, location);
        if (stack == null) {
            PlayerItemStackEntity entity = new PlayerItemStackEntity();
            entity.setPlayerId(playerId);
            entity.setServerId(serverId);
            entity.setItemId(itemId);
            entity.setQty(qty);
            entity.setLocation(location);
            entity.setCreateUser(SYSTEM_USER);
            entity.setUpdateUser(SYSTEM_USER);
            playerItemStackMapper.insert(entity);
            return;
        }

        stack.setQty(stack.getQty() + qty);
        stack.setUpdateUser(SYSTEM_USER);
        if (playerItemStackMapper.updateById(stack) <= 0) {
            throwConflict("库存已变化，请刷新后重试");
        }
    }

    /**
     * 查找指定位置的堆叠行。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @param itemId 物品ID
     * @param location 位置
     * @return 堆叠行
     */
    private PlayerItemStackEntity findStack(Long playerId, Integer serverId, Long itemId, Integer location) {
        LambdaQueryWrapper<PlayerItemStackEntity> query = new LambdaQueryWrapper<>();
        query.eq(PlayerItemStackEntity::getPlayerId, playerId);
        query.eq(PlayerItemStackEntity::getServerId, serverId);
        query.eq(PlayerItemStackEntity::getItemId, itemId);
        query.eq(PlayerItemStackEntity::getLocation, location);
        return playerItemStackMapper.selectOne(query);
    }

    /**
     * 扣减钱包余额并写流水。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @param amount 金额
     * @param reason 原因
     * @param refType 关联类型
     * @param refId 关联ID
     */
    private void deductWallet(Long playerId, Integer serverId, Long amount, String reason, String refType, Long refId) {
        WalletEntity wallet = requireWallet(playerId, serverId);
        if (wallet.getBalance() == null || wallet.getBalance().longValue() < amount.longValue()) {
            throwConflict("金币不足");
        }

        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdateUser(SYSTEM_USER);
        if (walletMapper.updateById(wallet) <= 0) {
            throwConflict("余额已变化，请刷新后重试");
        }

        insertWalletFlow(playerId, serverId, -amount, wallet.getBalance(), reason, refType, refId);
    }

    /**
     * 增加钱包余额并写流水。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @param amount 金额
     * @param reason 原因
     * @param refType 关联类型
     * @param refId 关联ID
     */
    private void creditWallet(Long playerId, Integer serverId, Long amount, String reason, String refType, Long refId) {
        WalletEntity wallet = findWallet(playerId, serverId);
        if (wallet == null) {
            WalletEntity entity = new WalletEntity();
            entity.setPlayerId(playerId);
            entity.setServerId(serverId);
            entity.setCurrencyCode(CURRENCY_GOLD);
            entity.setBalance(amount);
            entity.setCreateUser(SYSTEM_USER);
            entity.setUpdateUser(SYSTEM_USER);
            walletMapper.insert(entity);
            insertWalletFlow(playerId, serverId, amount, amount, reason, refType, refId);
            return;
        }

        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdateUser(SYSTEM_USER);
        if (walletMapper.updateById(wallet) <= 0) {
            throwConflict("余额已变化，请刷新后重试");
        }

        insertWalletFlow(playerId, serverId, amount, wallet.getBalance(), reason, refType, refId);
    }

    /**
     * 查找钱包。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @return 钱包
     */
    private WalletEntity findWallet(Long playerId, Integer serverId) {
        LambdaQueryWrapper<WalletEntity> query = new LambdaQueryWrapper<>();
        query.eq(WalletEntity::getPlayerId, playerId);
        query.eq(WalletEntity::getServerId, serverId);
        query.eq(WalletEntity::getCurrencyCode, CURRENCY_GOLD);
        return walletMapper.selectOne(query);
    }

    /**
     * 强制要求钱包存在。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @return 钱包
     */
    private WalletEntity requireWallet(Long playerId, Integer serverId) {
        WalletEntity wallet = findWallet(playerId, serverId);
        if (wallet == null) {
            throwConflict("金币不足");
        }
        return wallet;
    }

    /**
     * 插入钱包流水。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @param delta 变动额
     * @param balanceAfter 变动后余额
     * @param reason 原因
     * @param refType 关联类型
     * @param refId 关联ID
     */
    private void insertWalletFlow(Long playerId, Integer serverId, Long delta, Long balanceAfter, String reason, String refType, Long refId) {
        WalletFlowEntity flow = new WalletFlowEntity();
        flow.setPlayerId(playerId);
        flow.setServerId(serverId);
        flow.setCurrencyCode(CURRENCY_GOLD);
        flow.setDelta(delta);
        flow.setBalanceAfter(balanceAfter);
        flow.setReason(reason);
        flow.setRefType(refType);
        flow.setRefId(refId);
        flow.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        flow.setFlowTime(LocalDateTime.now());
        flow.setCreateUser(SYSTEM_USER);
        flow.setUpdateUser(SYSTEM_USER);
        walletFlowMapper.insert(flow);
    }

    /**
     * 组装订单分页结果。
     *
     * @param total 总数
     * @param records 记录
     * @return 分页结果
     */
    private MarketOrderPageResult buildOrderPageResult(Long total, List<MarketOrderEntity> records) {
        MarketOrderPageResult result = new MarketOrderPageResult();
        result.setTotal(total == null ? 0L : total);

        if (records == null || records.isEmpty()) {
            result.setList(Collections.emptyList());
            return result;
        }

        Map<Long, PlayerEntity> playerMap = loadPlayerMap(extractPlayerIdsFromOrders(records));
        Map<Long, ItemDefEntity> itemMap = loadItemMap(extractItemIdsFromOrders(records));

        List<MarketOrderView> list = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            MarketOrderEntity order = records.get(i);
            MarketOrderView view = new MarketOrderView();
            view.setOrderId(order.getId());
            view.setOrderType(MarketOrderTypeEnum.fromDbValue(order.getOrderType()).getCode());
            view.setStatus(MarketOrderStatusEnum.fromDbValue(order.getStatus()).getCode());
            view.setSellerId(order.getSellerId());
            view.setBuyerId(order.getBuyerId());
            view.setItemId(order.getItemId());
            view.setQtyTotal(order.getQtyTotal());
            view.setQtyRemain(order.getQtyRemain());
            view.setPriceEach(order.getPriceEach());
            view.setTotalAmount(safeMultiply(order.getQtyRemain(), order.getPriceEach()));
            view.setCurrencyCode(order.getCurrencyCode());
            view.setFeeRateBp(order.getFeeRateBp());
            view.setExpireTime(order.getExpireTime());
            view.setCreateTime(order.getCreateTime());

            PlayerEntity seller = playerMap.get(order.getSellerId());
            PlayerEntity buyer = playerMap.get(order.getBuyerId());
            ItemDefEntity item = itemMap.get(order.getItemId());

            view.setSellerName(seller == null ? "" : seller.getNickname());
            view.setBuyerName(buyer == null ? "" : buyer.getNickname());
            view.setItemName(item == null ? "" : item.getNameZh());

            list.add(view);
        }

        result.setList(list);
        return result;
    }

    /**
     * 组装成交分页结果。
     *
     * @param total 总数
     * @param records 记录
     * @return 分页结果
     */
    private MarketTradePageResult buildTradePageResult(Long total, List<MarketTradeEntity> records) {
        MarketTradePageResult result = new MarketTradePageResult();
        result.setTotal(total == null ? 0L : total);

        if (records == null || records.isEmpty()) {
            result.setList(Collections.emptyList());
            return result;
        }

        Map<Long, PlayerEntity> playerMap = loadPlayerMap(extractPlayerIdsFromTrades(records));
        Map<Long, ItemDefEntity> itemMap = loadItemMap(extractItemIdsFromTrades(records));

        List<MarketTradeView> list = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            MarketTradeEntity trade = records.get(i);
            MarketTradeView view = new MarketTradeView();
            view.setTradeId(trade.getId());
            view.setOrderId(trade.getOrderId());
            view.setSellerId(trade.getSellerId());
            view.setBuyerId(trade.getBuyerId());
            view.setItemId(trade.getItemId());
            view.setQty(trade.getQty());
            view.setPriceEach(trade.getPriceEach());
            view.setTotalAmount(trade.getTotalAmount());
            view.setCurrencyCode(trade.getCurrencyCode());
            view.setTradeTime(trade.getTradeTime());

            PlayerEntity seller = playerMap.get(trade.getSellerId());
            PlayerEntity buyer = playerMap.get(trade.getBuyerId());
            ItemDefEntity item = itemMap.get(trade.getItemId());

            view.setSellerName(seller == null ? "" : seller.getNickname());
            view.setBuyerName(buyer == null ? "" : buyer.getNickname());
            view.setItemName(item == null ? "" : item.getNameZh());

            list.add(view);
        }

        result.setList(list);
        return result;
    }

    /**
     * 读取订单涉及的玩家映射。
     *
     * @param ids 玩家ID集合
     * @return 玩家映射
     */
    private Map<Long, PlayerEntity> loadPlayerMap(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PlayerEntity> list = playerMapper.selectBatchIds(ids);
        Map<Long, PlayerEntity> map = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            map.put(list.get(i).getId(), list.get(i));
        }
        return map;
    }

    /**
     * 读取物品映射。
     *
     * @param ids 物品ID集合
     * @return 物品映射
     */
    private Map<Long, ItemDefEntity> loadItemMap(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ItemDefEntity> list = itemDefMapper.selectBatchIds(ids);
        Map<Long, ItemDefEntity> map = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            map.put(list.get(i).getId(), list.get(i));
        }
        return map;
    }

    /**
     * 提取订单涉及的玩家ID。
     *
     * @param orders 订单列表
     * @return 玩家ID列表
     */
    private List<Long> extractPlayerIdsFromOrders(List<MarketOrderEntity> orders) {
        Map<Long, Long> idMap = new LinkedHashMap<>();
        for (int i = 0; i < orders.size(); i++) {
            MarketOrderEntity order = orders.get(i);
            if (order.getSellerId() != null) {
                idMap.put(order.getSellerId(), order.getSellerId());
            }
            if (order.getBuyerId() != null) {
                idMap.put(order.getBuyerId(), order.getBuyerId());
            }
        }
        return new ArrayList<>(idMap.values());
    }

    /**
     * 提取成交涉及的玩家ID。
     *
     * @param trades 成交列表
     * @return 玩家ID列表
     */
    private List<Long> extractPlayerIdsFromTrades(List<MarketTradeEntity> trades) {
        Map<Long, Long> idMap = new LinkedHashMap<>();
        for (int i = 0; i < trades.size(); i++) {
            MarketTradeEntity trade = trades.get(i);
            idMap.put(trade.getSellerId(), trade.getSellerId());
            idMap.put(trade.getBuyerId(), trade.getBuyerId());
        }
        return new ArrayList<>(idMap.values());
    }

    /**
     * 提取订单涉及的物品ID。
     *
     * @param orders 订单列表
     * @return 物品ID列表
     */
    private List<Long> extractItemIdsFromOrders(List<MarketOrderEntity> orders) {
        Map<Long, Long> idMap = new LinkedHashMap<>();
        for (int i = 0; i < orders.size(); i++) {
            idMap.put(orders.get(i).getItemId(), orders.get(i).getItemId());
        }
        return new ArrayList<>(idMap.values());
    }

    /**
     * 提取成交涉及的物品ID。
     *
     * @param trades 成交列表
     * @return 物品ID列表
     */
    private List<Long> extractItemIdsFromTrades(List<MarketTradeEntity> trades) {
        Map<Long, Long> idMap = new LinkedHashMap<>();
        for (int i = 0; i < trades.size(); i++) {
            idMap.put(trades.get(i).getItemId(), trades.get(i).getItemId());
        }
        return new ArrayList<>(idMap.values());
    }

    /**
     * 提取堆叠涉及的物品ID。
     *
     * @param stacks 堆叠列表
     * @return 物品ID列表
     */
    private List<Long> extractItemIdsFromStacks(List<PlayerItemStackEntity> stacks) {
        Map<Long, Long> idMap = new LinkedHashMap<>();
        for (int i = 0; i < stacks.size(); i++) {
            idMap.put(stacks.get(i).getItemId(), stacks.get(i).getItemId());
        }
        return new ArrayList<>(idMap.values());
    }

    /**
     * 构造过期时间。
     *
     * @param expireMinutes 过期分钟数
     * @return 过期时间
     */
    private LocalDateTime buildExpireTime(Long expireMinutes) {
        if (expireMinutes == null || expireMinutes.longValue() <= 0L) {
            return null;
        }
        return LocalDateTime.now().plusMinutes(expireMinutes);
    }

    /**
     * 校验订单类型。
     *
     * @param orderTypeCode 类型编码
     * @return 枚举
     */
    private MarketOrderTypeEnum requireOrderType(String orderTypeCode) {
        MarketOrderTypeEnum orderType = MarketOrderTypeEnum.fromCode(orderTypeCode);
        if (orderType == null) {
            throw new IllegalArgumentException("orderType 只能是 SELL 或 BUY");
        }
        return orderType;
    }

    /**
     * 规范化页码。
     *
     * @param pageNo 页码
     * @return 页码
     */
    private long normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo.intValue() <= 0) {
            return 1L;
        }
        return pageNo.longValue();
    }

    /**
     * 规范化页大小。
     *
     * @param pageSize 页大小
     * @return 页大小
     */
    private long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize.intValue() <= 0) {
            return 20L;
        }
        if (pageSize.intValue() > 100) {
            return 100L;
        }
        return pageSize.longValue();
    }

    /**
     * 校验正整数。
     *
     * @param value 值
     * @param message 错误消息
     */
    private void validatePositive(Long value, String message) {
        if (value == null || value.longValue() <= 0L) {
            throwParamInvalid(message);
        }
    }

    /**
     * 校验可选正整数。
     *
     * @param value 值
     * @param message 错误消息
     */
    private void validateOptionalPositive(Long value, String message) {
        if (value != null && value.longValue() <= 0L) {
            throwParamInvalid(message);
        }
    }

    /**
     * 抛出参数错误。
     *
     * @param message 错误消息
     */
    private void throwParamInvalid(String message) {
        throw new BizException(ErrorCode.PARAM_INVALID, message);
    }

    /**
     * 抛出资源不存在错误。
     *
     * @param message 错误消息
     */
    private void throwNotFound(String message) {
        throw new BizException(ErrorCode.NOT_FOUND, message);
    }

    /**
     * 抛出业务冲突错误。
     *
     * @param message 错误消息
     */
    private void throwConflict(String message) {
        throw new BizException(ErrorCode.CONFLICT, message);
    }

    /**
     * 安全乘法。
     *
     * @param left 左值
     * @param right 右值
     * @return 乘积
     */
    private Long safeMultiply(Long left, Long right) {
        if (left == null || right == null) {
            return 0L;
        }
        return left * right;
    }
}