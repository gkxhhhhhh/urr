package com.urr.api.game;

import com.urr.api.game.dto.GatherTaskOperateMode;
import com.urr.api.game.dto.GatherTaskPanelResponse;
import com.urr.api.game.dto.OperateGatherTaskRequest;
import com.urr.api.game.dto.OperateGatherTaskResponse;
import com.urr.api.game.dto.QueryGatherTaskPanelRequest;
import com.urr.api.game.dto.StopGatherTaskRequest;
import com.urr.api.game.dto.StopGatherTaskResponse;
import com.urr.app.action.task.command.EnqueueGatherTaskCommand;
import com.urr.app.action.task.command.StartGatherTaskCommand;
import com.urr.app.action.task.command.StopGatherTaskCommand;
import com.urr.app.action.task.query.QueryGatherTaskPanelQuery;
import com.urr.app.action.task.result.QueryGatherTaskPanelResult;
import com.urr.app.action.task.result.StartGatherTaskResult;
import com.urr.app.action.task.result.StopGatherTaskResult;
import com.urr.domain.action.task.ActionTaskConstants;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 采集任务 Controller 组装器。
 *
 * 说明：
 * 1. 这里只做 API request/response 和 app command/result 之间的轻量转换。
 * 2. 不承载业务规则。
 */
@Component
public class GatherTaskControllerAssembler {

    /**
     * 把统一写接口请求转换成“立即开始”命令。
     *
     * @param accountId 账号ID
     * @param request 接口请求
     * @return 开始命令
     */
    public StartGatherTaskCommand toStartCommand(Long accountId, OperateGatherTaskRequest request) {
        StartGatherTaskCommand command = new StartGatherTaskCommand();
        command.setAccountId(accountId);
        command.setPlayerId(request.getPlayerId());
        command.setActionCode(request.getActionCode());
        command.setTargetCount(request.getTargetCount());
        return command;
    }

    /**
     * 把统一写接口请求转换成“加入队列”命令。
     *
     * @param accountId 账号ID
     * @param request 接口请求
     * @return 入队命令
     */
    public EnqueueGatherTaskCommand toEnqueueCommand(Long accountId, OperateGatherTaskRequest request) {
        EnqueueGatherTaskCommand command = new EnqueueGatherTaskCommand();
        command.setAccountId(accountId);
        command.setPlayerId(request.getPlayerId());
        command.setActionCode(request.getActionCode());
        command.setTargetCount(request.getTargetCount());
        return command;
    }

    /**
     * 把停止请求转换成停止命令。
     *
     * @param accountId 账号ID
     * @param request 停止请求
     * @return 停止命令
     */
    public StopGatherTaskCommand toStopCommand(Long accountId, StopGatherTaskRequest request) {
        StopGatherTaskCommand command = new StopGatherTaskCommand();
        command.setAccountId(accountId);
        command.setPlayerId(request.getPlayerId());
        return command;
    }

    /**
     * 把面板查询请求转换成查询对象。
     *
     * @param accountId 账号ID
     * @param request 面板请求
     * @return 查询对象
     */
    public QueryGatherTaskPanelQuery toPanelQuery(Long accountId, QueryGatherTaskPanelRequest request) {
        QueryGatherTaskPanelQuery query = new QueryGatherTaskPanelQuery();
        query.setAccountId(accountId);
        query.setPlayerId(request.getPlayerId());
        query.setReadTime(null);
        return query;
    }

    /**
     * 把 app 写结果转换成接口层响应。
     *
     * @param mode 操作模式
     * @param request 接口请求
     * @param result app 结果
     * @return 接口响应
     */
    public OperateGatherTaskResponse toOperateResponse(GatherTaskOperateMode mode,
                                                       OperateGatherTaskRequest request,
                                                       StartGatherTaskResult result) {
        OperateGatherTaskResponse response = new OperateGatherTaskResponse();
        response.setMode(mode == null ? null : mode.getCode());
        response.setPlayerId(result == null ? null : result.getPlayerId());
        response.setActionCode(result == null ? null : result.getActionCode());
        response.setTargetCount(request == null ? null : request.getTargetCount());
        response.setInfiniteTarget(isInfiniteTarget(request == null ? null : request.getTargetCount()));
        response.setTaskId(result == null ? null : result.getTaskId());
        response.setQueueId(result == null ? null : result.getQueueId());
        response.setStatus(result == null ? null : result.getStatus());
        response.setQueued(result != null && Boolean.TRUE.equals(result.getQueued()));
        response.setQueuePosition(result == null ? null : result.getQueuePosition());
        response.setReplacedTaskId(result == null ? null : result.getReplacedTaskId());
        return response;
    }

    /**
     * 把停止结果转换成接口层响应。
     *
     * @param result 停止结果
     * @return 停止响应
     */
    public StopGatherTaskResponse toStopResponse(StopGatherTaskResult result) {
        StopGatherTaskResponse response = new StopGatherTaskResponse();
        response.setSuccess(Boolean.TRUE);
        response.setTaskId(result == null ? null : result.getTaskId());
        response.setPlayerId(result == null ? null : result.getPlayerId());
        response.setStatus(result == null ? null : result.getStatus());
        response.setStopReason(result == null ? null : result.getStopReason());
        response.setCompletedCount(result == null ? null : result.getCompletedCount());
        response.setFlushedCount(result == null ? null : result.getFlushedCount());
        response.setStopTime(result == null ? null : result.getStopTime());

        StopGatherTaskResponse.FlushInfo flushInfo = new StopGatherTaskResponse.FlushInfo();
        flushInfo.setFlushedRoundCount(result == null ? null : result.getFlushedRoundCount());
        flushInfo.setAppliedRewardEntryCount(result == null ? null : result.getAppliedRewardEntryCount());
        flushInfo.setRewardFlushed(result != null && Boolean.TRUE.equals(result.getRewardFlushed()));
        response.setFlush(flushInfo);

        return response;
    }

    /**
     * 把面板 app 结果转换成接口层响应。
     *
     * @param result 面板 app 结果
     * @return 面板响应
     */
    public GatherTaskPanelResponse toPanelResponse(QueryGatherTaskPanelResult result) {
        if (result == null) {
            return null;
        }

        GatherTaskPanelResponse response = new GatherTaskPanelResponse();
        response.setPlayerId(result.getPlayerId());
        response.setReadTime(result.getReadTime());
        response.setHasRunningTask(result.getHasRunningTask());
        response.setQueueSize(result.getQueueSize());
        response.setRunningTask(toRunningTask(result.getCurrentTask()));
        response.setQueueList(toQueueList(result.getQueueList()));
        response.setPendingRewardPool(toPendingRewardPool(result.getPendingRewardPool()));
        response.setDisplayInventory(toInventory(result.getDisplayInventory()));
        return response;
    }

    /**
     * 转换运行中任务视图。
     *
     * @param view app 运行中任务视图
     * @return 接口运行中任务视图
     */
    private GatherTaskPanelResponse.RunningTask toRunningTask(QueryGatherTaskPanelResult.RuntimeView view) {
        if (view == null) {
            return null;
        }

        GatherTaskPanelResponse.RunningTask task = new GatherTaskPanelResponse.RunningTask();
        task.setTaskId(view.getTaskId());
        task.setActionCode(view.getActionCode());
        task.setActionName(view.getActionName());
        task.setStatus(view.getStatus());
        task.setTargetCount(view.getTargetCount());
        task.setInfiniteTarget(view.getInfiniteTarget());
        task.setCompletedCount(view.getCompletedCount());
        task.setFlushedCount(view.getFlushedCount());
        task.setRemainingCount(view.getRemainingCount());
        task.setCurrentSegmentStart(view.getCurrentSegmentStart());
        task.setCurrentSegmentEnd(view.getCurrentSegmentEnd());
        task.setSegmentSize(view.getSegmentSize());
        task.setStartTime(view.getStartTime());
        task.setLastSettleTime(view.getLastSettleTime());
        task.setOfflineExpireAt(view.getOfflineExpireAt());
        task.setSnapshot(toSnapshot(view.getSnapshot()));
        task.setCurrentSegment(toSegment(view.getCurrentSegment()));
        task.setPendingRewardPool(toPendingRewardPool(view.getPendingRewardPool()));
        return task;
    }

    /**
     * 转换快照视图。
     *
     * @param view app 快照视图
     * @return 接口快照视图
     */
    private GatherTaskPanelResponse.Snapshot toSnapshot(QueryGatherTaskPanelResult.SnapshotView view) {
        if (view == null) {
            return null;
        }

        GatherTaskPanelResponse.Snapshot snapshot = new GatherTaskPanelResponse.Snapshot();
        snapshot.setGatherLevel(view.getGatherLevel());
        snapshot.setGatherDurationMs(view.getGatherDurationMs());
        snapshot.setGatherEfficiency(view.getGatherEfficiency());
        snapshot.setOfflineMinutesLimit(view.getOfflineMinutesLimit());
        return snapshot;
    }

    /**
     * 转换当前分段视图。
     *
     * @param view app 分段视图
     * @return 接口分段视图
     */
    private GatherTaskPanelResponse.Segment toSegment(QueryGatherTaskPanelResult.SegmentView view) {
        if (view == null) {
            return null;
        }

        GatherTaskPanelResponse.Segment segment = new GatherTaskPanelResponse.Segment();
        segment.setSegmentStart(view.getSegmentStart());
        segment.setSegmentEnd(view.getSegmentEnd());
        segment.setSegmentSize(view.getSegmentSize());
        segment.setRewardSeed(view.getRewardSeed());
        segment.setLockedRoundCount(view.getLockedRoundCount());
        segment.setCompletedCountInSegment(view.getCompletedCountInSegment());
        segment.setRemainingCountInSegment(view.getRemainingCountInSegment());
        return segment;
    }

    /**
     * 转换 pending_reward_pool 视图。
     *
     * @param view app pending 视图
     * @return 接口 pending 视图
     */
    private GatherTaskPanelResponse.PendingRewardPool toPendingRewardPool(QueryGatherTaskPanelResult.PendingRewardPoolView view) {
        if (view == null) {
            return null;
        }

        GatherTaskPanelResponse.PendingRewardPool pendingRewardPool = new GatherTaskPanelResponse.PendingRewardPool();
        pendingRewardPool.setTaskId(view.getTaskId());
        pendingRewardPool.setCompletedCount(view.getCompletedCount());
        pendingRewardPool.setFlushedCount(view.getFlushedCount());
        pendingRewardPool.setPendingRoundCount(view.getPendingRoundCount());
        pendingRewardPool.setHasPending(view.getHasPending());
        pendingRewardPool.setRewardList(toRewardEntryList(view.getRewardList()));
        return pendingRewardPool;
    }

    /**
     * 转换队列列表。
     *
     * @param viewList app 队列列表
     * @return 接口队列列表
     */
    private List<GatherTaskPanelResponse.QueueItem> toQueueList(List<QueryGatherTaskPanelResult.QueueItemView> viewList) {
        List<GatherTaskPanelResponse.QueueItem> responseList = new ArrayList<>();
        if (viewList == null || viewList.isEmpty()) {
            return responseList;
        }

        for (int i = 0; i < viewList.size(); i++) {
            QueryGatherTaskPanelResult.QueueItemView view = viewList.get(i);
            GatherTaskPanelResponse.QueueItem item = new GatherTaskPanelResponse.QueueItem();
            item.setQueueId(view.getQueueId());
            item.setTaskId(view.getTaskId());
            item.setActionCode(view.getActionCode());
            item.setActionName(view.getActionName());
            item.setStatus(view.getStatus());
            item.setQueuePosition(view.getQueuePosition());
            item.setTargetCount(view.getTargetCount());
            item.setInfiniteTarget(view.getInfiniteTarget());
            responseList.add(item);
        }

        return responseList;
    }

    /**
     * 转换展示库存。
     *
     * @param view app 展示库存
     * @return 接口展示库存
     */
    private GatherTaskPanelResponse.Inventory toInventory(QueryGatherTaskPanelResult.InventoryView view) {
        if (view == null) {
            return null;
        }

        GatherTaskPanelResponse.Inventory inventory = new GatherTaskPanelResponse.Inventory();
        inventory.setEntryCount(view.getEntryCount());
        inventory.setEntryList(toInventoryEntryList(view.getEntryList()));
        return inventory;
    }

    /**
     * 转换奖励项列表。
     *
     * @param viewList app 奖励项列表
     * @return 接口奖励项列表
     */
    private List<GatherTaskPanelResponse.RewardEntry> toRewardEntryList(List<QueryGatherTaskPanelResult.RewardEntryView> viewList) {
        List<GatherTaskPanelResponse.RewardEntry> responseList = new ArrayList<>();
        if (viewList == null || viewList.isEmpty()) {
            return responseList;
        }

        for (int i = 0; i < viewList.size(); i++) {
            QueryGatherTaskPanelResult.RewardEntryView view = viewList.get(i);
            GatherTaskPanelResponse.RewardEntry entry = new GatherTaskPanelResponse.RewardEntry();
            entry.setRewardType(view.getRewardType());
            entry.setRewardCode(view.getRewardCode());
            entry.setRewardName(view.getRewardName());
            entry.setQuantity(view.getQuantity());
            responseList.add(entry);
        }

        return responseList;
    }

    /**
     * 转换展示库存项列表。
     *
     * @param viewList app 展示库存项列表
     * @return 接口展示库存项列表
     */
    private List<GatherTaskPanelResponse.InventoryEntry> toInventoryEntryList(List<QueryGatherTaskPanelResult.InventoryEntryView> viewList) {
        List<GatherTaskPanelResponse.InventoryEntry> responseList = new ArrayList<>();
        if (viewList == null || viewList.isEmpty()) {
            return responseList;
        }

        for (int i = 0; i < viewList.size(); i++) {
            QueryGatherTaskPanelResult.InventoryEntryView view = viewList.get(i);
            GatherTaskPanelResponse.InventoryEntry entry = new GatherTaskPanelResponse.InventoryEntry();
            entry.setRewardType(view.getRewardType());
            entry.setRewardCode(view.getRewardCode());
            entry.setRewardName(view.getRewardName());
            entry.setFormalQuantity(view.getFormalQuantity());
            entry.setPendingQuantity(view.getPendingQuantity());
            entry.setDisplayQuantity(view.getDisplayQuantity());
            entry.setItemId(view.getItemId());
            entry.setEquipInstanceId(view.getEquipInstanceId());
            entry.setItemType(view.getItemType());
            entry.setItemLevel(view.getItemLevel());
            entry.setStrengthenLevel(view.getStrengthenLevel());
            entry.setEquipCategory(view.getEquipCategory());
            entry.setBaseAttack(view.getBaseAttack());
            entry.setCurrentAttack(view.getCurrentAttack());
            responseList.add(entry);
        }

        return responseList;
    }

    /**
     * 判断当前目标轮次是否为无限次数。
     *
     * @param targetCount 目标轮次
     * @return true-无限次数，false-有限次数
     */
    private boolean isInfiniteTarget(Long targetCount) {
        if (targetCount == null) {
            return false;
        }
        return targetCount.longValue() == ActionTaskConstants.INFINITE_TARGET_COUNT;
    }
}