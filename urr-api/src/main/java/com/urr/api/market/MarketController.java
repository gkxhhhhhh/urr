package com.urr.api.market;

import com.urr.api.auth.AuthRequired;
import com.urr.api.market.dto.MarketCancelOrderReq;
import com.urr.api.market.dto.MarketCreateOrderReq;
import com.urr.api.market.dto.MarketOrderPageQueryReq;
import com.urr.api.market.dto.MarketTradeActionReq;
import com.urr.api.market.dto.MarketTradePageQueryReq;
import com.urr.app.market.MarketAppService;
import com.urr.app.market.result.MarketCancelOrderResult;
import com.urr.app.market.result.MarketCreateOrderResult;
import com.urr.app.market.result.MarketInventoryResult;
import com.urr.app.market.result.MarketItemCatalogResult;
import com.urr.app.market.result.MarketOrderPageResult;
import com.urr.app.market.result.MarketTradePageResult;
import com.urr.app.market.result.MarketTradeResult;
import com.urr.commons.api.ResponseData;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 市场接口 Controller。
 *
 * 说明：
 * 1. 只负责市场页自己的查询和写操作。
 * 2. 不回收首页快照、战斗、职业动作等其它模块。
 * 3. 路径直接对齐前端当前 src/api/market.ts 已经在使用的接口。
 */
@RestController
@RequestMapping("/api/game/market")
@RequiredArgsConstructor
@Validated
public class MarketController {

    /**
     * 市场应用服务。
     */
    private final MarketAppService marketAppService;

    /**
     * 查询可交易物品目录。
     *
     * @param playerId 角色ID
     * @return 物品目录
     */
    @GetMapping("/items")
    public ResponseData<MarketItemCatalogResult> items(@RequestParam("playerId") Long playerId) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.queryTradableItems(accountId, playerId)
        );
    }

    /**
     * 查询当前角色可挂牌库存。
     *
     * @param playerId 角色ID
     * @return 可挂牌库存
     */
    @GetMapping("/inventory")
    public ResponseData<MarketInventoryResult> inventory(@RequestParam("playerId") Long playerId) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.queryTradableInventory(accountId, playerId)
        );
    }

    /**
     * 查询市场订单列表。
     *
     * @param req 查询请求
     * @return 订单分页结果
     */
    @GetMapping("/orders")
    public ResponseData<MarketOrderPageResult> orders(@Valid MarketOrderPageQueryReq req) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.queryMarketOrders(
                        accountId,
                        req.getPlayerId(),
                        req.getOrderType(),
                        req.getItemId(),
                        req.getPageNo(),
                        req.getPageSize()
                )
        );
    }

    /**
     * 查询我的订单列表。
     *
     * @param req 查询请求
     * @return 我的订单分页结果
     */
    @GetMapping("/my-orders")
    public ResponseData<MarketOrderPageResult> myOrders(@Valid MarketOrderPageQueryReq req) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.queryMyOrders(
                        accountId,
                        req.getPlayerId(),
                        req.getOrderType(),
                        req.getPageNo(),
                        req.getPageSize()
                )
        );
    }

    /**
     * 查询我的成交记录。
     *
     * @param req 查询请求
     * @return 我的成交分页结果
     */
    @GetMapping("/my-trades")
    public ResponseData<MarketTradePageResult> myTrades(@Valid MarketTradePageQueryReq req) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.queryMyTrades(
                        accountId,
                        req.getPlayerId(),
                        req.getPageNo(),
                        req.getPageSize()
                )
        );
    }

    /**
     * 创建卖单。
     *
     * @param req 创建请求
     * @return 创建结果
     */
    @PostMapping("/sell-orders")
    public ResponseData<MarketCreateOrderResult> createSellOrder(@RequestBody @Valid MarketCreateOrderReq req) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.createSellOrder(
                        accountId,
                        req.getPlayerId(),
                        req.getItemId(),
                        req.getQty(),
                        req.getPriceEach(),
                        req.getExpireMinutes()
                )
        );
    }

    /**
     * 购买卖单。
     *
     * @param orderId 订单ID
     * @param req 成交请求
     * @return 成交结果
     */
    @PostMapping("/sell-orders/{orderId}/buy")
    public ResponseData<MarketTradeResult> buySellOrder(
            @PathVariable("orderId") Long orderId,
            @RequestBody @Valid MarketTradeActionReq req
    ) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.buySellOrder(
                        accountId,
                        req.getPlayerId(),
                        orderId,
                        req.getQty()
                )
        );
    }

    /**
     * 创建买单。
     *
     * @param req 创建请求
     * @return 创建结果
     */
    @PostMapping("/buy-orders")
    public ResponseData<MarketCreateOrderResult> createBuyOrder(@RequestBody @Valid MarketCreateOrderReq req) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.createBuyOrder(
                        accountId,
                        req.getPlayerId(),
                        req.getItemId(),
                        req.getQty(),
                        req.getPriceEach(),
                        req.getExpireMinutes()
                )
        );
    }

    /**
     * 卖给买单。
     *
     * @param orderId 订单ID
     * @param req 成交请求
     * @return 成交结果
     */
    @PostMapping("/buy-orders/{orderId}/sell")
    public ResponseData<MarketTradeResult> sellToBuyOrder(
            @PathVariable("orderId") Long orderId,
            @RequestBody @Valid MarketTradeActionReq req
    ) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.sellToBuyOrder(
                        accountId,
                        req.getPlayerId(),
                        orderId,
                        req.getQty()
                )
        );
    }

    /**
     * 取消订单。
     *
     * @param orderId 订单ID
     * @param req 取消请求
     * @return 取消结果
     */
    @PostMapping("/orders/{orderId}/cancel")
    public ResponseData<MarketCancelOrderResult> cancelOrder(
            @PathVariable("orderId") Long orderId,
            @RequestBody @Valid MarketCancelOrderReq req
    ) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.cancelOrder(
                        accountId,
                        req.getPlayerId(),
                        orderId
                )
        );
    }
}