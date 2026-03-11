package com.urr.domain.action.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ActionTreeResponseDTO {

    private Long playerId;

    /**
     * 树版本，前端可用于缓存和兼容判断
     */
    private Integer treeVersion = 1;

    /**
     * 根节点列表，目前就是 GROUP 节点
     */
    private List<TreeNode> nodes = new ArrayList<>();

    @Data
    public static class TreeNode {

        /**
         * 原表主键
         */
        private Long id;

        /**
         * 唯一节点Key，前端可直接当 tree key 用
         * 例如 GROUP:1 / BEHAVIOR:3 / ACTION:7
         */
        private String nodeKey;

        /**
         * 父节点Key，根节点为 null
         */
        private String parentNodeKey;

        /**
         * GROUP / BEHAVIOR / CATEGORY / SUB_CATEGORY / ACTION
         */
        private String nodeType;

        /**
         * 业务编码
         */
        private String code;

        /**
         * 展示名称
         */
        private String name;

        /**
         * 排序值
         */
        private Integer sort;

        /**
         * 状态：1启用 0禁用
         */
        private Integer status;

        /**
         * 是否已解锁（动作按玩家等级算，父节点按子节点汇总）
         */
        private Boolean unlocked;

        /**
         * 是否可展开
         */
        private Boolean expandable;

        /**
         * 是否叶子节点
         */
        private Boolean leaf;

        /**
         * 特殊类型，例如 BATTLE / MODULE
         */
        private String specialType;

        /**
         * 从根到当前节点的 nodeKey 路径
         */
        private List<String> pathNodeKeys = new ArrayList<>();

        /**
         * 从根到当前节点的业务 code 路径
         */
        private List<String> pathCodes = new ArrayList<>();

        /**
         * 从根到当前节点的名称路径
         */
        private List<String> pathNames = new ArrayList<>();

        /**
         * 扩展信息
         */
        private Map<String, Object> meta = new LinkedHashMap<>();

        /**
         * 子节点（混合类型）
         */
        private List<TreeNode> children = new ArrayList<>();
    }
}