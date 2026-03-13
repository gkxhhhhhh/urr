package com.urr.app.market;

import com.urr.app.market.result.MarketCancelOrderResult;
import com.urr.app.market.result.MarketCreateOrderResult;
import com.urr.app.market.result.MarketInventoryResult;
import com.urr.app.market.result.MarketOrderPageResult;
import com.urr.app.market.result.MarketTradePageResult;
import com.urr.app.market.result.MarketTradeResult;

/**
 * 市场应用服务。
 */
public interface MarketAppService {

    /**
     * 查询可挂牌库存。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @return 可挂牌库存
     */
    MarketInventoryResult queryTradableInventory(Long accountId, Long playerId);

    /**
     * 查询市场列表。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @param orderTypeCode 订单类型
     * @param pageNo 页码
     * @param pageSize 页大小
     * @return 分页结果
     */
    MarketOrderPageResult queryMarketOrders(Long accountId, Long playerId, String orderTypeCode, Integer pageNo, Integer pageSize);

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
    MarketOrderPageResult queryMyOrders(Long accountId, Long playerId, String orderTypeCode, Integer pageNo, Integer pageSize);

    /**
     * 查询我的成交记录。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @param pageNo 页码
     * @param pageSize 页大小
     * @return 分页结果
     */
    MarketTradePageResult queryMyTrades(Long accountId, Long playerId, Integer pageNo, Integer pageSize);

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
    MarketCreateOrderResult createSellOrder(Long accountId, Long playerId, Long itemId, Long qty, Long priceEach, Long expireMinutes);

    /**
     * 购买卖单。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @param orderId 订单ID
     * @param qty 成交数量
     * @return 成交结果
     */
    MarketTradeResult buySellOrder(Long accountId, Long playerId, Long orderId, Long qty);

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
    MarketCreateOrderResult createBuyOrder(Long accountId, Long playerId, Long itemId, Long qty, Long priceEach, Long expireMinutes);

    /**
     * 卖给买单。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @param orderId 订单ID
     * @param qty 成交数量
     * @return 成交结果
     */
    MarketTradeResult sellToBuyOrder(Long accountId, Long playerId, Long orderId, Long qty);

    /**
     * 取消订单。
     *
     * @param accountId 账号ID
     * @param playerId 玩家ID
     * @param orderId 订单ID
     * @return 取消结果
     */
    MarketCancelOrderResult cancelOrder(Long accountId, Long playerId, Long orderId);
}