package com.urr.api.game;

import com.urr.api.auth.AuthRequired;
import com.urr.api.game.dto.CancelMarketOrderRequest;
import com.urr.api.game.dto.CreateBuyOrderRequest;
import com.urr.api.game.dto.CreateSellOrderRequest;
import com.urr.api.game.dto.MarketTradeRequest;
import com.urr.api.game.dto.QueryMarketOrdersRequest;
import com.urr.app.market.MarketAppService;
import com.urr.commons.api.ResponseData;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 市场一期 Controller。
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
     * 查询可挂牌库存。
     *
     * @param playerId 玩家ID
     * @return 可挂牌库存
     */
    @GetMapping("/inventory")
    public ResponseData queryInventory(Long playerId) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(marketAppService.queryTradableInventory(accountId, playerId));
    }

    /**
     * 查询市场列表。
     *
     * @param request 查询请求
     * @return 市场列表
     */
    @GetMapping("/orders")
    public ResponseData queryOrders(@Valid QueryMarketOrdersRequest request) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.queryMarketOrders(
                        accountId,
                        request.getPlayerId(),
                        request.getOrderType(),
                        request.getPageNo(),
                        request.getPageSize()
                )
        );
    }

    /**
     * 查询我的订单。
     *
     * @param request 查询请求
     * @return 我的订单
     */
    @GetMapping("/my-orders")
    public ResponseData queryMyOrders(@Valid QueryMarketOrdersRequest request) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.queryMyOrders(
                        accountId,
                        request.getPlayerId(),
                        request.getOrderType(),
                        request.getPageNo(),
                        request.getPageSize()
                )
        );
    }

    /**
     * 查询我的成交记录。
     *
     * @param playerId 玩家ID
     * @param pageNo 页码
     * @param pageSize 页大小
     * @return 我的成交记录
     */
    @GetMapping("/my-trades")
    public ResponseData queryMyTrades(Long playerId, Integer pageNo, Integer pageSize) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(marketAppService.queryMyTrades(accountId, playerId, pageNo, pageSize));
    }

    /**
     * 创建卖单。
     *
     * @param request 创建请求
     * @return 创建结果
     */
    @PostMapping("/sell-orders")
    public ResponseData createSellOrder(@RequestBody @Valid CreateSellOrderRequest request) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.createSellOrder(
                        accountId,
                        request.getPlayerId(),
                        request.getItemId(),
                        request.getQty(),
                        request.getPriceEach(),
                        request.getExpireMinutes()
                )
        );
    }

    /**
     * 购买卖单。
     *
     * @param orderId 订单ID
     * @param request 成交请求
     * @return 成交结果
     */
    @PostMapping("/sell-orders/{orderId}/buy")
    public ResponseData buySellOrder(@PathVariable("orderId") Long orderId, @RequestBody @Valid MarketTradeRequest request) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.buySellOrder(
                        accountId,
                        request.getPlayerId(),
                        orderId,
                        request.getQty()
                )
        );
    }

    /**
     * 创建买单。
     *
     * @param request 创建请求
     * @return 创建结果
     */
    @PostMapping("/buy-orders")
    public ResponseData createBuyOrder(@RequestBody @Valid CreateBuyOrderRequest request) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.createBuyOrder(
                        accountId,
                        request.getPlayerId(),
                        request.getItemId(),
                        request.getQty(),
                        request.getPriceEach(),
                        request.getExpireMinutes()
                )
        );
    }

    /**
     * 卖给买单。
     *
     * @param orderId 订单ID
     * @param request 成交请求
     * @return 成交结果
     */
    @PostMapping("/buy-orders/{orderId}/sell")
    public ResponseData sellToBuyOrder(@PathVariable("orderId") Long orderId, @RequestBody @Valid MarketTradeRequest request) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.sellToBuyOrder(
                        accountId,
                        request.getPlayerId(),
                        orderId,
                        request.getQty()
                )
        );
    }

    /**
     * 取消订单。
     *
     * @param orderId 订单ID
     * @param request 取消请求
     * @return 取消结果
     */
    @PostMapping("/orders/{orderId}/cancel")
    public ResponseData cancelOrder(@PathVariable("orderId") Long orderId, @RequestBody @Valid CancelMarketOrderRequest request) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                marketAppService.cancelOrder(
                        accountId,
                        request.getPlayerId(),
                        orderId
                )
        );
    }
}