/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : urr

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 28/02/2026 10:39:58
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_urr_account
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_account`;
CREATE TABLE `t_urr_account`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '账号(唯一)',
  `password_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码hash',
  `salt` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '盐(可选)',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态 1正常 2封禁',
  `register_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '注册IP',
  `last_login_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最近登录IP',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最近登录时间',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_account`(`account` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_last_login_time`(`last_login_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '账号表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_account
-- ----------------------------
INSERT INTO `t_urr_account` VALUES (1, '1', '$2a$10$pX2MMYiu9iaWDgGUf5RMuuorGaqWJGpPcQ2e6o.aYKhxWs8HuCFA6', NULL, 1, '0:0:0:0:0:0:0:1', '0:0:0:0:0:0:0:1', '2026-02-27 10:21:17', NULL, '-1', '2026-02-26 18:12:31', '-1', '2026-02-26 18:12:31', 0);
INSERT INTO `t_urr_account` VALUES (2, 'guestohn0dsokpge58cryjxr', '$2a$10$6atObQgyeJaTRyTDJBV7gObMRyNZcuxxCtxAhzAf72aBM/IpOWvKy', NULL, 1, '0:0:0:0:0:0:0:1', '0:0:0:0:0:0:0:1', '2026-02-27 11:03:58', NULL, '-1', '2026-02-26 19:42:13', '-1', '2026-02-26 19:42:13', 0);

-- ----------------------------
-- Table structure for t_urr_action_def
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_action_def`;
CREATE TABLE `t_urr_action_def`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `behavior_id` bigint UNSIGNED NOT NULL COMMENT '所属行为ID(必选)',
  `category_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '所属类型ID(可选，0表示无)',
  `sub_category_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '所属动作类型ID(可选，0表示无)',
  `action_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '动作编码(全局唯一)',
  `action_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '动作名称',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `action_kind` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'LOOP' COMMENT '动作类型: LOOP循环 INSTANT即时 MODULE模块',
  `base_duration_ms` int NOT NULL COMMENT '基础耗时(ms/每轮)',
  `base_energy_cost` int NOT NULL DEFAULT 0 COMMENT '基础体力消耗(每轮)',
  `min_player_level` int NOT NULL DEFAULT 1 COMMENT '最低玩家等级',
  `min_skill_level` int NOT NULL DEFAULT 0 COMMENT '最低技能等级(可选)',
  `unlock_condition_json` json NULL COMMENT '解锁条件',
  `duration_scale_rule` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '耗时缩放规则Key',
  `reward_scale_rule` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '收益缩放规则Key',
  `params_json` json NULL COMMENT '扩展(风险/地点/推荐战力等)',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_action_code`(`action_code` ASC) USING BTREE,
  INDEX `idx_behavior_cat_sub`(`behavior_id` ASC, `category_id` ASC, `sub_category_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '动作定义(必选层)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_action_def
-- ----------------------------
INSERT INTO `t_urr_action_def` VALUES (1, 1, 1, 0, 'CRAFT_SEWING_BODY_ROUGH_CLOTH_ARMOR', '粗糙布甲', 1, 'INSTANT', 0, 0, 1, 0, NULL, NULL, NULL, '{\"note\": \"绑定缝纫配方/队列执行\", \"entry\": \"recipe\"}', 0, NULL, '-1', '2026-02-27 18:40:42', '-1', '2026-02-27 18:40:42', 0);
INSERT INTO `t_urr_action_def` VALUES (2, 1, 1, 0, 'CRAFT_SEWING_BODY_COTTON_CLOTH_ARMOR', '棉布布甲', 1, 'INSTANT', 0, 0, 1, 0, NULL, NULL, NULL, '{\"note\": \"绑定缝纫配方/队列执行\", \"entry\": \"recipe\"}', 0, NULL, '-1', '2026-02-27 18:40:42', '-1', '2026-02-27 18:40:42', 0);
INSERT INTO `t_urr_action_def` VALUES (3, 1, 1, 0, 'CRAFT_SEWING_BODY_HERITAGE_CLOTH_ARMOR', '传承布甲', 1, 'INSTANT', 0, 0, 1, 0, NULL, NULL, NULL, '{\"note\": \"绑定缝纫配方/队列执行\", \"entry\": \"recipe\"}', 0, NULL, '-1', '2026-02-27 18:40:42', '-1', '2026-02-27 18:40:42', 0);
INSERT INTO `t_urr_action_def` VALUES (4, 2, 0, 0, 'GATHER_PICKING_EGG', '鸡蛋', 1, 'LOOP', 30000, 0, 1, 0, NULL, NULL, NULL, '{\"kind\": \"food\", \"note\": \"示例：每30秒一轮\"}', 0, NULL, '-1', '2026-02-27 18:40:54', '-1', '2026-02-27 18:40:54', 0);
INSERT INTO `t_urr_action_def` VALUES (5, 2, 0, 0, 'GATHER_PICKING_COTTON', '棉花', 1, 'LOOP', 45000, 0, 1, 0, NULL, NULL, NULL, '{\"kind\": \"material\", \"note\": \"示例：每45秒一轮\"}', 0, NULL, '-1', '2026-02-27 18:40:54', '-1', '2026-02-27 18:40:54', 0);
INSERT INTO `t_urr_action_def` VALUES (6, 2, 0, 0, 'GATHER_PICKING_COFFEE_BEAN', '咖啡豆', 1, 'LOOP', 60000, 0, 2, 0, NULL, NULL, NULL, '{\"kind\": \"food\", \"note\": \"示例：每60秒一轮\"}', 0, NULL, '-1', '2026-02-27 18:40:54', '-1', '2026-02-27 18:40:54', 0);
INSERT INTO `t_urr_action_def` VALUES (7, 3, 2, 1, 'BATTLE_COMBAT_REGION_A_PLANET_PATROL', '治安巡逻', 1, 'INSTANT', 0, 5, 1, 0, NULL, NULL, NULL, '{\"note\": \"示例：消耗体力进入战斗/任务\", \"planet\": \"A\", \"mission\": \"patrol\", \"dungeon_code\": \"D_A_PATROL\", \"encounter_code\": \"E_A_PATROL\"}', 0, NULL, '-1', '2026-02-27 18:41:00', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_action_def` VALUES (8, 3, 2, 1, 'BATTLE_COMBAT_REGION_A_PLANET_SUPPRESS_REBELS', '镇压叛军', 1, 'INSTANT', 0, 8, 3, 0, NULL, NULL, NULL, '{\"planet\": \"A\", \"mission\": \"rebels\", \"dungeon_code\": \"D_A_REBELS\", \"encounter_code\": \"E_A_REBELS\"}', 0, NULL, '-1', '2026-02-27 18:41:00', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_action_def` VALUES (9, 3, 2, 2, 'BATTLE_COMBAT_REGION_B_PLANET_SLIME', '史莱姆', 1, 'INSTANT', 0, 10, 5, 0, NULL, NULL, NULL, '{\"enemy\": \"slime\", \"planet\": \"B\", \"dungeon_code\": \"D_B_SLIME\", \"encounter_code\": \"E_B_SLIME\"}', 0, NULL, '-1', '2026-02-27 18:41:00', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_action_def` VALUES (10, 3, 2, 2, 'BATTLE_COMBAT_REGION_B_PLANET_GOBLIN', '哥布林', 1, 'INSTANT', 0, 12, 6, 0, NULL, NULL, NULL, '{\"enemy\": \"goblin\", \"planet\": \"B\", \"dungeon_code\": \"D_B_GOBLIN\", \"encounter_code\": \"E_B_GOBLIN\"}', 0, NULL, '-1', '2026-02-27 18:41:00', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_action_def` VALUES (11, 3, 2, 2, 'BATTLE_COMBAT_REGION_B_PLANET_CORE', '星球核心', 1, 'INSTANT', 0, 20, 10, 0, NULL, NULL, NULL, '{\"boss\": \"core\", \"note\": \"偏BOSS/关底\", \"planet\": \"B\", \"dungeon_code\": \"D_B_CORE\", \"encounter_code\": \"E_B_PLANET_CORE\"}', 0, NULL, '-1', '2026-02-27 18:41:00', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_action_def` VALUES (12, 3, 2, 3, 'BATTLE_COMBAT_TEAM_LOBBY', '组队大厅', 1, 'MODULE', 0, 0, 1, 0, NULL, NULL, NULL, '{\"note\": \"独立开发模块入口\", \"route\": \"/team\", \"module\": \"team\"}', 0, NULL, '-1', '2026-02-27 18:41:08', '-1', '2026-02-27 18:41:08', 0);

-- ----------------------------
-- Table structure for t_urr_action_reward_def
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_action_reward_def`;
CREATE TABLE `t_urr_action_reward_def`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `action_id` bigint UNSIGNED NOT NULL COMMENT '动作ID',
  `reward_group` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '掉落组',
  `reward_mode` tinyint UNSIGNED NOT NULL COMMENT '1固定 2概率 3权重(组内抽1) 4区间随机 5次数随机',
  `item_id` bigint UNSIGNED NOT NULL COMMENT '物品定义ID',
  `min_qty` int NOT NULL DEFAULT 0,
  `max_qty` int NOT NULL DEFAULT 0,
  `prob_per_million` int NOT NULL DEFAULT 1000000 COMMENT '百万分比概率',
  `weight` int NOT NULL DEFAULT 0 COMMENT '权重(用于mode=3)',
  `roll_times_min` int NOT NULL DEFAULT 1 COMMENT '次数随机最小(mode=5)',
  `roll_times_max` int NOT NULL DEFAULT 1 COMMENT '次数随机最大(mode=5)',
  `scale_rule` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单条产出缩放规则Key',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1,
  `version` int NOT NULL DEFAULT 0,
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_action`(`action_id` ASC) USING BTREE,
  INDEX `idx_action_group`(`action_id` ASC, `reward_group` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '动作掉落表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_action_reward_def
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_behavior_def
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_behavior_def`;
CREATE TABLE `t_urr_behavior_def`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属行为域编码',
  `behavior_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '行为编码(域内唯一) 如 BIO_SAMPLE/FOOD_PICK/BATTLE_MISSION',
  `behavior_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '行为名称',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `settle_granularity_sec` int NOT NULL DEFAULT 1 COMMENT '建议结算粒度(秒)',
  `allow_parallel` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否允许并行(1是0否)',
  `params_json` json NULL COMMENT '扩展',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_group_behavior`(`group_code` ASC, `behavior_code` ASC) USING BTREE,
  INDEX `idx_group_status`(`group_code` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '行为定义(十几个)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_behavior_def
-- ----------------------------
INSERT INTO `t_urr_behavior_def` VALUES (1, 'CRAFT', 'SEWING', '缝纫', 1, 1, 1, NULL, 0, NULL, '-1', '2026-02-27 18:40:42', '-1', '2026-02-27 18:40:42', 0);
INSERT INTO `t_urr_behavior_def` VALUES (2, 'GATHER', 'PICKING', '采摘', 1, 1, 0, NULL, 0, NULL, '-1', '2026-02-27 18:40:54', '-1', '2026-02-27 18:40:54', 0);
INSERT INTO `t_urr_behavior_def` VALUES (3, 'BATTLE', 'COMBAT', '战斗', 1, 1, 0, NULL, 0, NULL, '-1', '2026-02-27 18:41:00', '-1', '2026-02-27 18:41:00', 0);

-- ----------------------------
-- Table structure for t_urr_behavior_group_def
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_behavior_group_def`;
CREATE TABLE `t_urr_behavior_group_def`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '行为域编码 GATHER/CRAFT/TRADE/QUEST/BATTLE/OTHER',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '行为域名称',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `params_json` json NULL COMMENT '扩展',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_group_code`(`group_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '行为域定义(六大类)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_behavior_group_def
-- ----------------------------
INSERT INTO `t_urr_behavior_group_def` VALUES (1, 'GATHER', '采集', 1, NULL, 0, NULL, '-1', '2026-02-27 18:40:32', '-1', '2026-02-27 18:40:32', 0);
INSERT INTO `t_urr_behavior_group_def` VALUES (2, 'CRAFT', '制造', 1, NULL, 0, NULL, '-1', '2026-02-27 18:40:32', '-1', '2026-02-27 18:40:32', 0);
INSERT INTO `t_urr_behavior_group_def` VALUES (3, 'BATTLE', '战斗', 1, NULL, 0, NULL, '-1', '2026-02-27 18:40:32', '-1', '2026-02-27 18:40:32', 0);

-- ----------------------------
-- Table structure for t_urr_category_def
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_category_def`;
CREATE TABLE `t_urr_category_def`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `behavior_id` bigint UNSIGNED NOT NULL COMMENT '所属行为ID',
  `category_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型编码(行为内唯一) 如 NORMAL/BOSS/ROUTE',
  `category_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型名称',
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1,
  `params_json` json NULL,
  `version` int NOT NULL DEFAULT 0,
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_behavior_category`(`behavior_id` ASC, `category_code` ASC) USING BTREE,
  INDEX `idx_behavior_status`(`behavior_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '类型(可选层)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_category_def
-- ----------------------------
INSERT INTO `t_urr_category_def` VALUES (1, 1, 'BODY', '身体', 10, 1, NULL, 0, NULL, '-1', '2026-02-27 18:40:42', '-1', '2026-02-27 18:40:42', 0);
INSERT INTO `t_urr_category_def` VALUES (2, 3, 'REGION', '战斗区域', 10, 1, NULL, 0, NULL, '-1', '2026-02-27 18:41:00', '-1', '2026-02-27 18:41:00', 0);

-- ----------------------------
-- Table structure for t_urr_cfg_kv
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_cfg_kv`;
CREATE TABLE `t_urr_cfg_kv`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cfg_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置key',
  `cfg_value` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置value(JSON/文本)',
  `cfg_desc` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `version` int NOT NULL DEFAULT 0 COMMENT '版本(可用于灰度/热更)',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_cfg_key`(`cfg_key` ASC) USING BTREE,
  INDEX `idx_update_time`(`update_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通用配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_cfg_kv
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_craft_queue
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_craft_queue`;
CREATE TABLE `t_urr_craft_queue`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '队列ID',
  `player_id` bigint UNSIGNED NOT NULL COMMENT '玩家ID',
  `server_id` int NOT NULL DEFAULT 1 COMMENT '区服ID',
  `recipe_id` bigint UNSIGNED NOT NULL COMMENT '配方ID',
  `batch_count` int NOT NULL DEFAULT 1 COMMENT '批量次数',
  `state` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态 1排队 2制作中 3完成 4取消 5失败',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `finish_time` datetime NULL DEFAULT NULL COMMENT '预计/实际完成时间',
  `last_calc_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上次结算点',
  `cost_snapshot` json NOT NULL COMMENT '消耗快照(防配置变更)',
  `output_snapshot` json NOT NULL COMMENT '产出快照(防配置变更)',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_server_player`(`server_id` ASC, `player_id` ASC) USING BTREE,
  INDEX `idx_player_state`(`player_id` ASC, `state` ASC) USING BTREE,
  INDEX `idx_finish_time`(`finish_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '制造队列(离线可结算)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_craft_queue
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_dungeon_def
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_dungeon_def`;
CREATE TABLE `t_urr_dungeon_def`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '副本ID',
  `dungeon_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '副本编码(唯一)',
  `name_zh` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '中文名',
  `name_en` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '英文名',
  `min_level` int NOT NULL DEFAULT 1 COMMENT '最低等级',
  `energy_cost` int NOT NULL DEFAULT 1 COMMENT '体力消耗(每次)',
  `battle_time_ms` int NOT NULL DEFAULT 30000 COMMENT '单次战斗时长(ms)(离线折算)',
  `drop_table_json` json NULL COMMENT '掉落表(JSON/引用)',
  `meta_json` json NULL COMMENT '扩展(难度、区域、怪物池等)',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dungeon_code`(`dungeon_code` ASC) USING BTREE,
  INDEX `idx_min_level`(`min_level` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '副本定义' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_dungeon_def
-- ----------------------------
INSERT INTO `t_urr_dungeon_def` VALUES (1, 'D_A_PATROL', 'A星球·治安巡逻', NULL, 1, 5, 30000, '{\"desc\": \"掉落按波次结算\", \"mode\": \"wave_reward\"}', '{\"type\": \"mission\", \"planet\": \"A\", \"encounter\": \"simple\", \"monster_pool\": [\"M_SLIME\"], \"encounter_code\": \"E_A_PATROL\"}', NULL, '-1', '2026-02-28 09:34:54', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_dungeon_def` VALUES (2, 'D_A_REBELS', 'A星球·镇压叛军', NULL, 3, 8, 45000, '{\"desc\": \"掉落按波次结算\", \"mode\": \"wave_reward\"}', '{\"type\": \"mission\", \"planet\": \"A\", \"encounter\": \"simple\", \"monster_pool\": [\"M_GOBLIN\"], \"encounter_code\": \"E_A_REBELS\"}', NULL, '-1', '2026-02-28 09:34:54', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_dungeon_def` VALUES (3, 'D_B_SLIME', 'B星球·史莱姆', NULL, 5, 10, 30000, '{\"desc\": \"掉落按波次结算\", \"mode\": \"wave_reward\"}', '{\"type\": \"hunt\", \"planet\": \"B\", \"monster_pool\": [\"M_SLIME\"], \"encounter_code\": \"E_B_SLIME\"}', NULL, '-1', '2026-02-28 09:34:54', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_dungeon_def` VALUES (4, 'D_B_GOBLIN', 'B星球·哥布林', NULL, 6, 12, 30000, '{\"desc\": \"掉落按波次结算\", \"mode\": \"wave_reward\"}', '{\"type\": \"hunt\", \"planet\": \"B\", \"monster_pool\": [\"M_GOBLIN\"], \"encounter_code\": \"E_B_GOBLIN\"}', NULL, '-1', '2026-02-28 09:34:54', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_dungeon_def` VALUES (5, 'D_B_CORE', 'B星球·星球核心', NULL, 10, 20, 90000, '{\"desc\": \"掉落按波次结算\", \"mode\": \"wave_reward\"}', '{\"boss\": \"B_PLANET_CORE_GUARDIAN\", \"type\": \"boss_chain\", \"planet\": \"B\", \"encounter_id\": 1, \"encounter_code\": \"E_B_PLANET_CORE\"}', NULL, '-1', '2026-02-28 09:34:54', '-1', '2026-02-28 10:38:32', 0);

-- ----------------------------
-- Table structure for t_urr_dungeon_run_log
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_dungeon_run_log`;
CREATE TABLE `t_urr_dungeon_run_log`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '运行记录ID',
  `player_id` bigint UNSIGNED NOT NULL COMMENT '玩家ID',
  `server_id` int NOT NULL DEFAULT 1 COMMENT '区服ID',
  `dungeon_id` bigint UNSIGNED NOT NULL COMMENT '副本ID',
  `stage` int NOT NULL DEFAULT 1 COMMENT '关卡/层数',
  `result` tinyint UNSIGNED NOT NULL COMMENT '结果 1胜利 2失败 3中断',
  `energy_cost` int NOT NULL DEFAULT 0 COMMENT '体力消耗',
  `battle_time_ms` int NOT NULL DEFAULT 0 COMMENT '用时',
  `reward_json` json NULL COMMENT '奖励(含掉落明细)',
  `seed` bigint NULL DEFAULT NULL COMMENT '随机种子(可追责/复现)',
  `run_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_server_player_time`(`server_id` ASC, `player_id` ASC, `run_time` ASC) USING BTREE,
  INDEX `idx_dungeon_time`(`dungeon_id` ASC, `run_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '副本运行日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_dungeon_run_log
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_encounter_def
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_encounter_def`;
CREATE TABLE `t_urr_encounter_def`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '遭遇ID',
  `encounter_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '遭遇编码(唯一)',
  `name_zh` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `min_level` int NOT NULL DEFAULT 1 COMMENT '建议最低等级',
  `recommended_power` bigint NOT NULL DEFAULT 0 COMMENT '建议战力',
  `meta_json` json NULL COMMENT '扩展(战场环境、特殊规则等)',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_encounter_code`(`encounter_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '遭遇/关卡脚本定义' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_encounter_def
-- ----------------------------
INSERT INTO `t_urr_encounter_def` VALUES (1, 'E_B_PLANET_CORE', 'B星球·星球核心', 10, 500, '{\"rule\": \"waves_then_boss\"}', 0, NULL, '-1', '2026-02-28 09:34:46', '-1', '2026-02-28 09:34:46', 0);
INSERT INTO `t_urr_encounter_def` VALUES (2, 'E_A_PATROL', 'A星球·治安巡逻', 1, 80, '{\"style\": \"mission\", \"planet\": \"A\"}', 0, NULL, '-1', '2026-02-28 10:38:32', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_encounter_def` VALUES (3, 'E_A_REBELS', 'A星球·镇压叛军', 3, 150, '{\"style\": \"mission\", \"planet\": \"A\"}', 0, NULL, '-1', '2026-02-28 10:38:32', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_encounter_def` VALUES (4, 'E_B_SLIME', 'B星球·史莱姆', 5, 220, '{\"style\": \"hunt\", \"planet\": \"B\"}', 0, NULL, '-1', '2026-02-28 10:38:32', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_encounter_def` VALUES (5, 'E_B_GOBLIN', 'B星球·哥布林', 6, 260, '{\"style\": \"hunt\", \"planet\": \"B\"}', 0, NULL, '-1', '2026-02-28 10:38:32', '-1', '2026-02-28 10:38:32', 0);

-- ----------------------------
-- Table structure for t_urr_encounter_wave
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_encounter_wave`;
CREATE TABLE `t_urr_encounter_wave`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '波次ID',
  `encounter_id` bigint UNSIGNED NOT NULL COMMENT '遭遇ID',
  `wave_no` int NOT NULL COMMENT '第几波(从1开始)',
  `monsters_json` json NOT NULL COMMENT '本波怪物列表: [{monster_code, count, level_override?, hp_mul?, atk_mul?}]',
  `reward_json` json NULL COMMENT '本波奖励配置(每波结算)',
  `meta_json` json NULL COMMENT '扩展(入场台词/掉落修正/镜头等)',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_encounter_wave`(`encounter_id` ASC, `wave_no` ASC) USING BTREE,
  INDEX `idx_encounter`(`encounter_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '遭遇波次配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_encounter_wave
-- ----------------------------
INSERT INTO `t_urr_encounter_wave` VALUES (1, 1, 1, '[{\"count\": 3, \"monster_code\": \"M_SLIME\"}]', '{\"rolls\": [{\"max\": 40, \"min\": 20, \"type\": \"currency\", \"currency\": \"GOLD\", \"prob_per_million\": 1000000}, {\"max\": 2, \"min\": 1, \"type\": \"item\", \"item_code\": \"FOOD_EGG\", \"prob_per_million\": 200000}], \"pick_one_by_weight\": []}', '{\"desc\": \"B星球核心 第1波：史莱姆\"}', 0, NULL, '-1', '2026-02-28 09:34:46', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_encounter_wave` VALUES (2, 1, 2, '[{\"count\": 2, \"monster_code\": \"M_GOBLIN\"}]', '{\"rolls\": [{\"max\": 60, \"min\": 30, \"type\": \"currency\", \"currency\": \"GOLD\", \"prob_per_million\": 1000000}, {\"max\": 2, \"min\": 1, \"type\": \"item\", \"item_code\": \"MAT_COTTON\", \"prob_per_million\": 250000}], \"pick_one_by_weight\": []}', '{\"desc\": \"B星球核心 第2波：哥布林\"}', 0, NULL, '-1', '2026-02-28 09:34:46', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_encounter_wave` VALUES (3, 1, 3, '[{\"count\": 1, \"monster_code\": \"M_WILD_BOAR\"}, {\"count\": 1, \"monster_code\": \"M_WILD_WOLF\"}]', '{\"rolls\": [{\"max\": 100, \"min\": 50, \"type\": \"currency\", \"currency\": \"GOLD\", \"prob_per_million\": 1000000}, {\"max\": 2, \"min\": 1, \"type\": \"item\", \"item_code\": \"FOOD_COFFEE_BEAN\", \"prob_per_million\": 300000}], \"pick_one_by_weight\": [{\"max\": 3, \"min\": 1, \"type\": \"item\", \"weight\": 60, \"item_code\": \"FOOD_EGG\"}, {\"max\": 2, \"min\": 1, \"type\": \"item\", \"weight\": 40, \"item_code\": \"MAT_COTTON\"}]}', '{\"desc\": \"B星球核心 第3波：野猪+野狼\"}', 0, NULL, '-1', '2026-02-28 09:34:46', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_encounter_wave` VALUES (4, 1, 4, '[{\"count\": 1, \"monster_code\": \"B_PLANET_CORE_GUARDIAN\"}]', '{\"rolls\": [{\"max\": 400, \"min\": 200, \"type\": \"currency\", \"currency\": \"GOLD\", \"prob_per_million\": 1000000}, {\"max\": 3, \"min\": 1, \"type\": \"item\", \"item_code\": \"FOOD_COFFEE_BEAN\", \"prob_per_million\": 500000}, {\"max\": 5, \"min\": 1, \"type\": \"item\", \"item_code\": \"FOOD_EGG\", \"prob_per_million\": 500000}], \"pick_one_by_weight\": [{\"max\": 800, \"min\": 500, \"type\": \"currency\", \"weight\": 40, \"currency\": \"GOLD\"}, {\"max\": 10, \"min\": 5, \"type\": \"item\", \"weight\": 60, \"item_code\": \"MAT_COTTON\"}]}', '{\"desc\": \"B星球核心 第4波：Boss 星核守卫者\"}', 0, NULL, '-1', '2026-02-28 09:34:46', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_encounter_wave` VALUES (5, 2, 1, '[{\"count\": 2, \"monster_code\": \"M_CITY_THUG\"}]', '{\"rolls\": [{\"max\": 20, \"min\": 10, \"type\": \"currency\", \"currency\": \"GOLD\", \"prob_per_million\": 1000000}, {\"max\": 2, \"min\": 1, \"type\": \"item\", \"item_code\": \"FOOD_EGG\", \"prob_per_million\": 200000}], \"pick_one_by_weight\": []}', '{\"desc\": \"A星治安巡逻 第1波\"}', 0, NULL, '-1', '2026-02-28 10:38:32', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_encounter_wave` VALUES (6, 3, 1, '[{\"count\": 2, \"monster_code\": \"M_REBEL_SCOUT\"}]', '{\"rolls\": [{\"max\": 30, \"min\": 15, \"type\": \"currency\", \"currency\": \"GOLD\", \"prob_per_million\": 1000000}, {\"max\": 1, \"min\": 1, \"type\": \"item\", \"item_code\": \"MAT_COTTON\", \"prob_per_million\": 150000}], \"pick_one_by_weight\": []}', '{\"desc\": \"A星镇压叛军 第1波\"}', 0, NULL, '-1', '2026-02-28 10:38:32', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_encounter_wave` VALUES (7, 3, 2, '[{\"count\": 2, \"monster_code\": \"M_REBEL_SOLDIER\"}]', '{\"rolls\": [{\"max\": 40, \"min\": 20, \"type\": \"currency\", \"currency\": \"GOLD\", \"prob_per_million\": 1000000}, {\"max\": 2, \"min\": 1, \"type\": \"item\", \"item_code\": \"MAT_COTTON\", \"prob_per_million\": 250000}], \"pick_one_by_weight\": [{\"max\": 1, \"min\": 1, \"type\": \"item\", \"weight\": 100, \"item_code\": \"FOOD_COFFEE_BEAN\"}]}', '{\"desc\": \"A星镇压叛军 第2波\"}', 0, NULL, '-1', '2026-02-28 10:38:32', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_encounter_wave` VALUES (8, 4, 1, '[{\"count\": 3, \"monster_code\": \"M_SLIME\"}]', '{\"rolls\": [{\"max\": 60, \"min\": 30, \"type\": \"currency\", \"currency\": \"GOLD\", \"prob_per_million\": 1000000}, {\"max\": 1, \"min\": 1, \"type\": \"item\", \"item_code\": \"FOOD_COFFEE_BEAN\", \"prob_per_million\": 150000}], \"pick_one_by_weight\": []}', '{\"desc\": \"B星史莱姆 第1波\"}', 0, NULL, '-1', '2026-02-28 10:38:32', '-1', '2026-02-28 10:38:32', 0);
INSERT INTO `t_urr_encounter_wave` VALUES (9, 5, 1, '[{\"count\": 2, \"monster_code\": \"M_GOBLIN\"}]', '{\"rolls\": [{\"max\": 80, \"min\": 40, \"type\": \"currency\", \"currency\": \"GOLD\", \"prob_per_million\": 1000000}, {\"max\": 2, \"min\": 1, \"type\": \"item\", \"item_code\": \"MAT_COTTON\", \"prob_per_million\": 200000}], \"pick_one_by_weight\": []}', '{\"desc\": \"B星哥布林 第1波\"}', 0, NULL, '-1', '2026-02-28 10:38:32', '-1', '2026-02-28 10:38:32', 0);

-- ----------------------------
-- Table structure for t_urr_gather_profile
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_gather_profile`;
CREATE TABLE `t_urr_gather_profile`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `skill_id` bigint UNSIGNED NOT NULL COMMENT '采集技能ID',
  `level_from` int NOT NULL COMMENT '等级起',
  `level_to` int NOT NULL COMMENT '等级止(含)',
  `base_interval_ms` int NOT NULL DEFAULT 60000 COMMENT '基础产出周期(ms)',
  `base_yield` decimal(18, 6) NOT NULL DEFAULT 1.000000 COMMENT '每周期基础产出(可为小数)',
  `yield_item_id` bigint UNSIGNED NOT NULL COMMENT '产出物品定义ID',
  `extra_drop_json` json NULL COMMENT '额外掉落(概率表/保底等)',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_skill_level`(`skill_id` ASC, `level_from` ASC, `level_to` ASC) USING BTREE,
  INDEX `idx_yield_item`(`yield_item_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '采集产出配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_gather_profile
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_item_def
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_item_def`;
CREATE TABLE `t_urr_item_def`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '物品定义ID',
  `item_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '物品编码(唯一)',
  `name_zh` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '中文名',
  `name_en` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '英文名',
  `item_type` tinyint UNSIGNED NOT NULL COMMENT '类型 1资源 2消耗品 3材料 4装备 5任务物 6货币',
  `rarity` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '稀有度 1白 2绿 3蓝 4紫 5橙',
  `stackable` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否可堆叠 1是 0否(装备通常0)',
  `max_stack` int NOT NULL DEFAULT 9999 COMMENT '最大堆叠数',
  `bind_type` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '绑定 0不绑定 1拾取绑定 2装备绑定',
  `tradeable` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否可交易 1是 0否',
  `sell_price` bigint NOT NULL DEFAULT 0 COMMENT 'NPC回收价(可选)',
  `meta_json` json NULL COMMENT '扩展(装备部位/基础属性/图标等)',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_item_code`(`item_code` ASC) USING BTREE,
  INDEX `idx_item_type`(`item_type` ASC) USING BTREE,
  INDEX `idx_tradeable`(`tradeable` ASC) USING BTREE,
  INDEX `idx_rarity`(`rarity` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '物品定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_item_def
-- ----------------------------
INSERT INTO `t_urr_item_def` VALUES (1, 'FOOD_EGG', '鸡蛋', 'Egg', 1, 1, 1, 9999, 0, 1, 2, '{\"use\": \"ingredient\", \"category\": \"food\"}', NULL, '-1', '2026-02-27 19:00:22', '-1', '2026-02-27 19:00:22', 0);
INSERT INTO `t_urr_item_def` VALUES (2, 'MAT_COTTON', '棉花', 'Cotton', 3, 1, 1, 9999, 0, 1, 3, '{\"category\": \"textile\"}', NULL, '-1', '2026-02-27 19:00:22', '-1', '2026-02-27 19:00:22', 0);
INSERT INTO `t_urr_item_def` VALUES (3, 'FOOD_COFFEE_BEAN', '咖啡豆', 'Coffee Bean', 1, 1, 1, 9999, 0, 1, 4, '{\"use\": \"ingredient\", \"category\": \"food\"}', NULL, '-1', '2026-02-27 19:00:22', '-1', '2026-02-27 19:00:22', 0);
INSERT INTO `t_urr_item_def` VALUES (4, 'CUR_GOLD', '金币', 'Gold', 6, 1, 1, 999999999, 0, 0, 0, '{\"currency_code\": \"GOLD\"}', NULL, '-1', '2026-02-27 19:00:22', '-1', '2026-02-27 19:00:22', 0);

-- ----------------------------
-- Table structure for t_urr_mail
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_mail`;
CREATE TABLE `t_urr_mail`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '邮件ID',
  `server_id` int NOT NULL DEFAULT 1 COMMENT '区服ID',
  `player_id` bigint UNSIGNED NOT NULL COMMENT '玩家ID',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '正文',
  `attachment_json` json NULL COMMENT '附件(JSON: item_id/装备/货币等)',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态 1未读 2已读 3已领取 4已删除',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间',
  `send_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_server_player_status`(`server_id` ASC, `player_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_send_time`(`send_time` ASC) USING BTREE,
  INDEX `idx_expire_time`(`expire_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '邮件/发奖' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_mail
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_market_order
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_market_order`;
CREATE TABLE `t_urr_market_order`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `server_id` int NOT NULL DEFAULT 1 COMMENT '区服ID',
  `order_type` tinyint UNSIGNED NOT NULL COMMENT '类型 1出售单(挂卖) 2求购单(挂买)(可选)',
  `seller_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '卖家玩家ID(出售单必填)',
  `buyer_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '买家玩家ID(求购单必填)',
  `item_id` bigint UNSIGNED NOT NULL COMMENT '物品定义ID',
  `equip_instance_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '装备实例ID(卖装备时填)',
  `qty_total` bigint NOT NULL DEFAULT 1 COMMENT '总数量',
  `qty_remain` bigint NOT NULL DEFAULT 1 COMMENT '剩余数量',
  `price_each` bigint NOT NULL COMMENT '单价(以 GOLD 为例)',
  `currency_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'GOLD' COMMENT '币种',
  `fee_rate_bp` int NOT NULL DEFAULT 500 COMMENT '手续费(基点，500=5%)',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态 1挂单 2部分成交 3已完成 4取消 5过期',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间(可选)',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_server_status`(`server_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_item_price`(`item_id` ASC, `price_each` ASC) USING BTREE,
  INDEX `idx_seller_time`(`seller_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_expire_time`(`expire_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '市场订单(挂单)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_market_order
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_market_trade
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_market_trade`;
CREATE TABLE `t_urr_market_trade`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '成交ID',
  `server_id` int NOT NULL DEFAULT 1 COMMENT '区服ID',
  `order_id` bigint UNSIGNED NOT NULL COMMENT '订单ID',
  `seller_id` bigint UNSIGNED NOT NULL COMMENT '卖家ID',
  `buyer_id` bigint UNSIGNED NOT NULL COMMENT '买家ID',
  `item_id` bigint UNSIGNED NOT NULL COMMENT '物品定义ID',
  `equip_instance_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '装备实例ID(若交易装备)',
  `qty` bigint NOT NULL DEFAULT 1 COMMENT '成交数量',
  `price_each` bigint NOT NULL COMMENT '单价',
  `currency_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'GOLD' COMMENT '币种',
  `fee_amount` bigint NOT NULL DEFAULT 0 COMMENT '手续费金额',
  `total_amount` bigint NOT NULL COMMENT '成交总额',
  `trade_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '成交时间',
  `seed` bigint NULL DEFAULT NULL COMMENT '随机种子(可选)',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_server_order`(`server_id` ASC, `order_id` ASC) USING BTREE,
  INDEX `idx_buyer_time`(`buyer_id` ASC, `trade_time` ASC) USING BTREE,
  INDEX `idx_seller_time`(`seller_id` ASC, `trade_time` ASC) USING BTREE,
  INDEX `idx_item_time`(`item_id` ASC, `trade_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '市场成交记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_market_trade
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_monster_def
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_monster_def`;
CREATE TABLE `t_urr_monster_def`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '怪物ID',
  `monster_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '怪物编码(唯一)',
  `name_zh` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '中文名',
  `name_en` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '英文名',
  `rarity` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '稀有度 1白 2绿 3蓝 4紫 5橙',
  `level` int NOT NULL DEFAULT 1 COMMENT '等级(模板推荐值)',
  `hp` bigint NOT NULL DEFAULT 10 COMMENT '生命',
  `atk` bigint NOT NULL DEFAULT 1 COMMENT '攻击',
  `def` bigint NOT NULL DEFAULT 0 COMMENT '防御',
  `spd` int NOT NULL DEFAULT 100 COMMENT '速度(回合/行动条用)',
  `meta_json` json NULL COMMENT '扩展(技能、元素、AI等)',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_monster_code`(`monster_code` ASC) USING BTREE,
  INDEX `idx_level`(`level` ASC) USING BTREE,
  INDEX `idx_rarity`(`rarity` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '怪物定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_monster_def
-- ----------------------------
INSERT INTO `t_urr_monster_def` VALUES (1, 'M_SLIME', '史莱姆', NULL, 1, 5, 80, 12, 2, 90, '{\"tags\": [\"slime\"]}', 0, NULL, '-1', '2026-02-28 09:34:34', '-1', '2026-02-28 09:34:34', 0);
INSERT INTO `t_urr_monster_def` VALUES (2, 'M_GOBLIN', '哥布林', NULL, 1, 6, 110, 16, 3, 105, '{\"tags\": [\"goblin\"]}', 0, NULL, '-1', '2026-02-28 09:34:34', '-1', '2026-02-28 09:34:34', 0);
INSERT INTO `t_urr_monster_def` VALUES (3, 'M_WILD_BOAR', '野猪', NULL, 2, 8, 180, 22, 5, 95, '{\"tags\": [\"beast\"]}', 0, NULL, '-1', '2026-02-28 09:34:34', '-1', '2026-02-28 09:34:34', 0);
INSERT INTO `t_urr_monster_def` VALUES (4, 'M_WILD_WOLF', '野狼', NULL, 2, 8, 160, 26, 4, 120, '{\"tags\": [\"beast\"]}', 0, NULL, '-1', '2026-02-28 09:34:34', '-1', '2026-02-28 09:34:34', 0);
INSERT INTO `t_urr_monster_def` VALUES (5, 'B_PLANET_CORE_GUARDIAN', '星核守卫者', NULL, 4, 12, 1200, 80, 25, 110, '{\"tags\": [\"boss\", \"core\"]}', 0, NULL, '-1', '2026-02-28 09:34:34', '-1', '2026-02-28 09:34:34', 0);
INSERT INTO `t_urr_monster_def` VALUES (6, 'M_CITY_THUG', '滋事暴徒', 'City Thug', 1, 2, 70, 10, 1, 95, '{\"tags\": [\"humanoid\"]}', 0, NULL, '-1', '2026-02-28 10:22:28', '-1', '2026-02-28 10:22:28', 0);
INSERT INTO `t_urr_monster_def` VALUES (7, 'M_REBEL_SCOUT', '叛军斥候', 'Rebel Scout', 2, 4, 130, 18, 4, 110, '{\"tags\": [\"humanoid\", \"rebel\"]}', 0, NULL, '-1', '2026-02-28 10:22:28', '-1', '2026-02-28 10:22:28', 0);
INSERT INTO `t_urr_monster_def` VALUES (8, 'M_REBEL_SOLDIER', '叛军士兵', 'Rebel Soldier', 2, 5, 170, 22, 6, 100, '{\"tags\": [\"humanoid\", \"rebel\"]}', 0, NULL, '-1', '2026-02-28 10:22:28', '-1', '2026-02-28 10:22:28', 0);

-- ----------------------------
-- Table structure for t_urr_player
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_player`;
CREATE TABLE `t_urr_player`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '玩家ID',
  `account_id` bigint UNSIGNED NOT NULL COMMENT '账号ID',
  `server_id` int NOT NULL DEFAULT 1 COMMENT '区服ID',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '昵称',
  `type` int NOT NULL COMMENT '1:标准玩家 3:铁牛玩家',
  `avatar` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  `level` int NOT NULL DEFAULT 1 COMMENT '等级',
  `exp` bigint NOT NULL DEFAULT 0 COMMENT '经验',
  `power` bigint NOT NULL DEFAULT 0 COMMENT '战力(可异步计算/缓存)',
  `energy` int NOT NULL DEFAULT 100 COMMENT '体力(副本/行动消耗)',
  `energy_update_time` datetime NULL DEFAULT NULL COMMENT '体力最后刷新时间(回体用)',
  `last_online_time` datetime NULL DEFAULT NULL COMMENT '最近在线时间',
  `last_settle_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次离线/在线结算时间(核心)',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_server_nickname`(`server_id` ASC, `nickname` ASC) USING BTREE,
  INDEX `idx_account_server`(`account_id` ASC, `server_id` ASC) USING BTREE,
  INDEX `idx_server_level`(`server_id` ASC, `level` ASC) USING BTREE,
  INDEX `idx_last_settle_time`(`last_settle_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '玩家表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_player
-- ----------------------------
INSERT INTO `t_urr_player` VALUES (1, 2, 1, 'test', 1, NULL, 1, 0, 0, 100, NULL, NULL, '2026-02-27 10:20:37', 0, NULL, '-1', '2026-02-27 10:20:37', '-1', '2026-02-27 10:20:37', 0);
INSERT INTO `t_urr_player` VALUES (2, 2, 1, '铁人1', 3, NULL, 1, 0, 0, 100, NULL, NULL, '2026-02-27 10:23:40', 0, NULL, '-1', '2026-02-27 10:23:40', '-1', '2026-02-27 10:23:40', 0);
INSERT INTO `t_urr_player` VALUES (3, 2, 1, '铁人2', 3, NULL, 1, 0, 0, 100, NULL, NULL, '2026-02-27 10:23:46', 0, NULL, '-1', '2026-02-27 10:23:46', '-1', '2026-02-27 10:23:46', 0);
INSERT INTO `t_urr_player` VALUES (4, 2, 1, '铁人34', 3, NULL, 1, 0, 0, 100, NULL, NULL, '2026-02-27 10:23:52', 0, NULL, '-1', '2026-02-27 10:23:52', '-1', '2026-02-27 10:23:52', 0);

-- ----------------------------
-- Table structure for t_urr_player_activity
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_player_activity`;
CREATE TABLE `t_urr_player_activity`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `player_id` bigint UNSIGNED NOT NULL COMMENT '玩家ID',
  `server_id` int NOT NULL DEFAULT 1 COMMENT '区服ID',
  `behavior_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '行为ID(新体系)',
  `action_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '动作ID(新体系)',
  `category_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '类型ID(可选)',
  `sub_category_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '动作类型ID(可选)',
  `activity_type` tinyint UNSIGNED NOT NULL COMMENT '类型 1采集 2制造 3副本 4挂机战斗 5休息',
  `target_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '目标ID(采集配置/配方/副本等)',
  `state` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态 1进行中 2暂停 3结束',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `last_calc_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上次结算时间(关键)',
  `last_settle_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次结算时间(新体系)',
  `param_json` json NULL COMMENT '参数',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_player`(`player_id` ASC) USING BTREE,
  INDEX `idx_server_player`(`server_id` ASC, `player_id` ASC) USING BTREE,
  INDEX `idx_activity_type`(`activity_type` ASC) USING BTREE,
  INDEX `idx_last_calc_time`(`last_calc_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '玩家当前活动状态' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_player_activity
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_player_dungeon_progress
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_player_dungeon_progress`;
CREATE TABLE `t_urr_player_dungeon_progress`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `player_id` bigint UNSIGNED NOT NULL COMMENT '玩家ID',
  `server_id` int NOT NULL DEFAULT 1 COMMENT '区服ID',
  `dungeon_id` bigint UNSIGNED NOT NULL COMMENT '副本ID',
  `best_stage` int NOT NULL DEFAULT 1 COMMENT '最高通关层/关',
  `best_time_ms` int NOT NULL DEFAULT 0 COMMENT '最好用时(可选)',
  `total_runs` bigint NOT NULL DEFAULT 0 COMMENT '累计次数',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_player_dungeon`(`player_id` ASC, `dungeon_id` ASC) USING BTREE,
  INDEX `idx_server_player`(`server_id` ASC, `player_id` ASC) USING BTREE,
  INDEX `idx_dungeon_id`(`dungeon_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '玩家副本进度' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_player_dungeon_progress
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_player_equip
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_player_equip`;
CREATE TABLE `t_urr_player_equip`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '装备实例ID',
  `player_id` bigint UNSIGNED NOT NULL COMMENT '玩家ID',
  `server_id` int NOT NULL DEFAULT 1 COMMENT '区服ID',
  `item_id` bigint UNSIGNED NOT NULL COMMENT '物品定义ID(装备类)',
  `equip_slot` tinyint UNSIGNED NULL DEFAULT NULL COMMENT '装备槽位(头/身/手/脚/武器等)',
  `level_req` int NOT NULL DEFAULT 1 COMMENT '等级需求',
  `bind_type` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '绑定 0不绑定 1拾取绑定 2装备绑定',
  `bind_player_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '绑定到的玩家(绑定后填)',
  `durability` int NOT NULL DEFAULT 100 COMMENT '耐久(可选)',
  `state` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态 1背包 2已装备 3仓库 4冻结(挂单/制作占用)',
  `attr_json` json NULL COMMENT '随机词条/强化/镶嵌等',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_server_player`(`server_id` ASC, `player_id` ASC) USING BTREE,
  INDEX `idx_item_id`(`item_id` ASC) USING BTREE,
  INDEX `idx_state`(`state` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '玩家装备实例(非堆叠)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_player_equip
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_player_item_stack
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_player_item_stack`;
CREATE TABLE `t_urr_player_item_stack`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `player_id` bigint UNSIGNED NOT NULL COMMENT '玩家ID',
  `server_id` int NOT NULL DEFAULT 1 COMMENT '区服ID(冗余便于分片/索引)',
  `item_id` bigint UNSIGNED NOT NULL COMMENT '物品定义ID',
  `qty` bigint NOT NULL DEFAULT 0 COMMENT '数量',
  `location` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '位置 1背包 2仓库 3冻结(挂单/制作占用)',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_player_item_loc`(`player_id` ASC, `item_id` ASC, `location` ASC) USING BTREE,
  INDEX `idx_server_player`(`server_id` ASC, `player_id` ASC) USING BTREE,
  INDEX `idx_item_id`(`item_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '玩家堆叠物品(资源/材料/道具)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_player_item_stack
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_player_skill
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_player_skill`;
CREATE TABLE `t_urr_player_skill`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `player_id` bigint UNSIGNED NOT NULL COMMENT '玩家ID',
  `server_id` int NOT NULL DEFAULT 1 COMMENT '区服ID',
  `skill_id` bigint UNSIGNED NOT NULL COMMENT '技能定义ID',
  `skill_level` int NOT NULL DEFAULT 1 COMMENT '技能等级',
  `skill_exp` bigint NOT NULL DEFAULT 0 COMMENT '技能经验',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_player_skill`(`player_id` ASC, `skill_id` ASC) USING BTREE,
  INDEX `idx_server_player`(`server_id` ASC, `player_id` ASC) USING BTREE,
  INDEX `idx_skill_id`(`skill_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '玩家技能' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_player_skill
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_rank_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_rank_snapshot`;
CREATE TABLE `t_urr_rank_snapshot`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '快照ID',
  `server_id` int NOT NULL DEFAULT 1 COMMENT '区服ID',
  `rank_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '榜单类型(LEVEL/POWER/WEALTH/DUNGEON等)',
  `snapshot_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
  `data_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '榜单数据(JSON: topN)',
  `version` int NOT NULL DEFAULT 0 COMMENT '版本',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_server_rank_time`(`server_id` ASC, `rank_type` ASC, `snapshot_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '排行榜快照' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_rank_snapshot
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_recipe_def
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_recipe_def`;
CREATE TABLE `t_urr_recipe_def`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '配方ID',
  `recipe_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配方编码(唯一)',
  `name_zh` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '中文名',
  `name_en` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '英文名',
  `craft_skill_id` bigint UNSIGNED NOT NULL COMMENT '制造技能ID',
  `craft_level_req` int NOT NULL DEFAULT 1 COMMENT '技能等级需求',
  `craft_time_ms` int NOT NULL DEFAULT 30000 COMMENT '制造耗时(ms)',
  `cost_json` json NOT NULL COMMENT '消耗材料(JSON: item_id->qty)',
  `output_json` json NOT NULL COMMENT '产出(JSON: item_id->qty 或 装备生成规则)',
  `exp_gain` bigint NOT NULL DEFAULT 0 COMMENT '制造经验',
  `meta_json` json NULL COMMENT '扩展(成功率/暴击/额外产出等)',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_recipe_code`(`recipe_code` ASC) USING BTREE,
  INDEX `idx_craft_skill`(`craft_skill_id` ASC, `craft_level_req` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '制造配方定义' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_recipe_def
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_skill_def
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_skill_def`;
CREATE TABLE `t_urr_skill_def`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '技能定义ID',
  `skill_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '技能编码(唯一)',
  `name_zh` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '中文名',
  `name_en` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '英文名',
  `skill_type` tinyint UNSIGNED NOT NULL COMMENT '类型 1采集 2制造 3战斗 4生活(增益)',
  `max_level` int NOT NULL DEFAULT 100 COMMENT '最大等级',
  `meta_json` json NULL COMMENT '扩展',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_skill_code`(`skill_code` ASC) USING BTREE,
  INDEX `idx_skill_type`(`skill_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '技能定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_skill_def
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_sub_category_def
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_sub_category_def`;
CREATE TABLE `t_urr_sub_category_def`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint UNSIGNED NOT NULL COMMENT '所属类型ID',
  `sub_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '动作类型编码(类型内唯一) 如 A_PLANET/B_PLANET/R1_ROUTE',
  `sub_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '动作类型名称',
  `parent_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '父级ID(0表示一级)',
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1,
  `params_json` json NULL,
  `version` int NOT NULL DEFAULT 0,
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '-1',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_category_sub`(`category_id` ASC, `sub_code` ASC) USING BTREE,
  INDEX `idx_category_parent`(`category_id` ASC, `parent_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '动作类型(可选，多级)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_sub_category_def
-- ----------------------------
INSERT INTO `t_urr_sub_category_def` VALUES (1, 2, 'A_PLANET', 'A星球战斗', 0, 10, 1, NULL, 0, NULL, '-1', '2026-02-27 18:41:00', '-1', '2026-02-27 18:41:00', 0);
INSERT INTO `t_urr_sub_category_def` VALUES (2, 2, 'B_PLANET', 'B星球战斗', 0, 20, 1, NULL, 0, NULL, '-1', '2026-02-27 18:41:00', '-1', '2026-02-27 18:41:00', 0);
INSERT INTO `t_urr_sub_category_def` VALUES (3, 2, 'OTHER', '其他', 0, 999, 1, NULL, 0, NULL, '-1', '2026-02-27 18:41:08', '-1', '2026-02-27 18:41:08', 0);

-- ----------------------------
-- Table structure for t_urr_wallet
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_wallet`;
CREATE TABLE `t_urr_wallet`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `player_id` bigint UNSIGNED NOT NULL COMMENT '玩家ID',
  `server_id` int NOT NULL DEFAULT 1 COMMENT '区服ID',
  `currency_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '币种(例如 GOLD, GEM)',
  `balance` bigint NOT NULL DEFAULT 0 COMMENT '余额',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_player_currency`(`player_id` ASC, `currency_code` ASC) USING BTREE,
  INDEX `idx_server_player`(`server_id` ASC, `player_id` ASC) USING BTREE,
  INDEX `idx_currency`(`currency_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '玩家钱包' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_wallet
-- ----------------------------

-- ----------------------------
-- Table structure for t_urr_wallet_flow
-- ----------------------------
DROP TABLE IF EXISTS `t_urr_wallet_flow`;
CREATE TABLE `t_urr_wallet_flow`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `player_id` bigint UNSIGNED NOT NULL COMMENT '玩家ID',
  `server_id` int NOT NULL DEFAULT 1 COMMENT '区服ID',
  `currency_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '币种',
  `delta` bigint NOT NULL COMMENT '变动(可正可负)',
  `balance_after` bigint NOT NULL COMMENT '变动后余额(便于对账)',
  `reason` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '原因(SETTLE/DUNGEON/CRAFT/MARKET_BUY/MARKET_SELL/GM等)',
  `ref_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '关联类型(ORDER/TRADE/MAIL等)',
  `ref_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '关联ID',
  `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '幂等请求号(可选：防重复扣款)',
  `flow_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  `remarks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '添加人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '-1' COMMENT '修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_flag` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_request_id`(`request_id` ASC) USING BTREE,
  INDEX `idx_server_player_time`(`server_id` ASC, `player_id` ASC, `flow_time` ASC) USING BTREE,
  INDEX `idx_reason_time`(`reason` ASC, `flow_time` ASC) USING BTREE,
  INDEX `idx_ref`(`ref_type` ASC, `ref_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '钱包流水' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_urr_wallet_flow
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
