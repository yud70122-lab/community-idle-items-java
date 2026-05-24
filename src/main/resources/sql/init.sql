-- =============================================
-- 社区闲置物品交易系统 - 数据库初始化脚本
-- 数据库: MySQL 5.7+
-- 字符集: utf8mb4
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------
-- 创建数据库
-- ---------------------------------------------
DROP DATABASE IF EXISTS `community_idle`;
CREATE DATABASE `community_idle` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `community_idle`;

-- ---------------------------------------------
-- 表1: user - 用户表
-- ---------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT        COMMENT '用户ID，主键',
    `openid`            VARCHAR(64)     DEFAULT NULL                   COMMENT '微信openid',
    `session_key`       VARCHAR(128)    DEFAULT NULL                   COMMENT '微信会话密钥',
    `nickname`          VARCHAR(64)     DEFAULT NULL                   COMMENT '用户昵称',
    `avatar`            VARCHAR(255)    DEFAULT NULL                   COMMENT '头像URL',
    `gender`            TINYINT         DEFAULT 0                      COMMENT '性别：0-未知，1-男，2-女',
    `phone`             VARCHAR(20)     DEFAULT NULL                   COMMENT '手机号码',
    `status`            TINYINT         DEFAULT 1                      COMMENT '账号状态：0-禁用，1-正常，2-注销中，3-已注销',
    `cancel_apply_time` DATETIME        DEFAULT NULL                   COMMENT '注销申请时间',
    `cancel_reason`     VARCHAR(255)    DEFAULT NULL                   COMMENT '注销原因',
    `cancel_process_time` DATETIME      DEFAULT NULL                   COMMENT '注销处理时间',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           TINYINT         DEFAULT 0                      COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_phone` (`phone`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------------------------------------
-- 表2: user_address - 用户地址表
-- ---------------------------------------------
DROP TABLE IF EXISTS `user_address`;
CREATE TABLE `user_address` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT        COMMENT '地址ID，主键',
    `user_id`           BIGINT          NOT NULL                       COMMENT '用户ID，关联user表',
    `receiver_name`     VARCHAR(32)     NOT NULL                       COMMENT '收货人姓名',
    `receiver_phone`    VARCHAR(20)     NOT NULL                       COMMENT '收货人电话',
    `province`          VARCHAR(32)     NOT NULL                       COMMENT '省份',
    `city`              VARCHAR(32)     NOT NULL                       COMMENT '城市',
    `district`          VARCHAR(32)     NOT NULL                       COMMENT '区县',
    `detail_address`    VARCHAR(255)    NOT NULL                       COMMENT '详细地址',
    `is_default`        TINYINT         DEFAULT 0                      COMMENT '是否默认地址：0-否，1-是',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           TINYINT         DEFAULT 0                      COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_default` (`user_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地址表';

-- ---------------------------------------------
-- 表3: item - 物品表
-- ---------------------------------------------
DROP TABLE IF EXISTS `item`;
CREATE TABLE `item` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT        COMMENT '物品ID，主键',
    `user_id`           BIGINT          NOT NULL                       COMMENT '发布用户ID，关联user表',
    `title`             VARCHAR(128)    NOT NULL                       COMMENT '物品标题',
    `description`       TEXT            DEFAULT NULL                   COMMENT '物品描述',
    `category_id`       BIGINT          DEFAULT NULL                   COMMENT '分类ID',
    `price`             DECIMAL(10,2)   NOT NULL                       COMMENT '价格（元）',
    `original_price`    DECIMAL(10,2)   DEFAULT NULL                   COMMENT '原价（元）',
    `images`            VARCHAR(1000)   DEFAULT NULL                   COMMENT '图片URL列表，逗号分隔',
    `cover_image`       VARCHAR(255)    DEFAULT NULL                   COMMENT '封面图片URL',
    `condition`         TINYINT         DEFAULT 0                      COMMENT '新旧程度：0-全新，1-几乎全新，2-轻微使用，3-明显使用，4-有瑕疵',
    `trade_type`        TINYINT         DEFAULT 1                      COMMENT '交易方式：1-自提，2-邮寄，3-均可',
    `status`            TINYINT         DEFAULT 1                      COMMENT '物品状态：0-已下架，1-在售，2-已锁定，3-已售出',
    `view_count`        INT             DEFAULT 0                      COMMENT '浏览次数',
    `like_count`        INT             DEFAULT 0                      COMMENT '点赞/收藏次数',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           TINYINT         DEFAULT 0                      COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_price` (`price`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品表';

-- ---------------------------------------------
-- 表4: order_info - 订单表
-- ---------------------------------------------
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT        COMMENT '订单ID，主键',
    `order_no`          VARCHAR(64)     NOT NULL                       COMMENT '订单编号',
    `buyer_id`          BIGINT          NOT NULL                       COMMENT '买家用户ID，关联user表',
    `seller_id`         BIGINT          NOT NULL                       COMMENT '卖家用户ID，关联user表',
    `item_id`           BIGINT          NOT NULL                       COMMENT '物品ID，关联item表',
    `item_title`        VARCHAR(128)    NOT NULL                       COMMENT '物品标题（快照）',
    `item_image`        VARCHAR(255)    DEFAULT NULL                   COMMENT '物品图片（快照）',
    `price`             DECIMAL(10,2)   NOT NULL                       COMMENT '成交价格（元）',
    `shipping_fee`      DECIMAL(10,2)   DEFAULT 0.00                   COMMENT '运费（元）',
    `total_amount`      DECIMAL(10,2)   NOT NULL                       COMMENT '订单总金额（元）',
    `payment_method`    TINYINT         DEFAULT NULL                   COMMENT '支付方式：1-微信支付，2-支付宝，3-线下支付',
    `payment_time`      DATETIME        DEFAULT NULL                   COMMENT '支付时间',
    `trade_type`        TINYINT         NOT NULL                       COMMENT '交易方式：1-自提，2-邮寄',
    `address_id`        BIGINT          DEFAULT NULL                   COMMENT '收货地址ID，关联user_address表',
    `address_snapshot`  VARCHAR(500)    DEFAULT NULL                   COMMENT '收货地址快照（JSON格式）',
    `status`            TINYINT         DEFAULT 1                      COMMENT '订单状态：1-待支付，2-已支付待发货，3-已发货待收货，4-已完成，5-已取消，6-退款中，7-已退款',
    `buyer_remark`      VARCHAR(255)    DEFAULT NULL                   COMMENT '买家备注',
    `seller_remark`     VARCHAR(255)    DEFAULT NULL                   COMMENT '卖家备注',
    `cancel_reason`     VARCHAR(255)    DEFAULT NULL                   COMMENT '取消原因',
    `cancel_time`       DATETIME        DEFAULT NULL                   COMMENT '取消时间',
    `ship_time`         DATETIME        DEFAULT NULL                   COMMENT '发货时间',
    `receive_time`      DATETIME        DEFAULT NULL                   COMMENT '收货时间',
    `complete_time`     DATETIME        DEFAULT NULL                   COMMENT '完成时间',
    `expire_time`       DATETIME        DEFAULT NULL                   COMMENT '订单过期时间',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           TINYINT         DEFAULT 0                      COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_item_id` (`item_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_buyer_status` (`buyer_id`, `status`),
    KEY `idx_seller_status` (`seller_id`, `status`),
    KEY `idx_payment_time` (`payment_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ---------------------------------------------
-- 表5: user_credit - 用户信用表
-- ---------------------------------------------
DROP TABLE IF EXISTS `user_credit`;
CREATE TABLE `user_credit` (
    `id`                        BIGINT          NOT NULL AUTO_INCREMENT        COMMENT '主键ID',
    `user_id`                   BIGINT          NOT NULL                       COMMENT '用户ID',
    `credit_score`              INT             DEFAULT 100                    COMMENT '信用分',
    `credit_level`              TINYINT         DEFAULT 1                      COMMENT '信用等级：1-5',
    `violation_count`           INT             DEFAULT 0                      COMMENT '累计违约次数',
    `pending_violation_count`   INT             DEFAULT 0                      COMMENT '待处理违约次数',
    `continuous_fulfill_days`   INT             DEFAULT 0                      COMMENT '连续履约天数',
    `max_continuous_fulfill_days` INT           DEFAULT 0                      COMMENT '历史最高连续履约天数',
    `total_fulfill_count`       INT             DEFAULT 0                      COMMENT '累计履约次数',
    `total_trade_count`         INT             DEFAULT 0                      COMMENT '累计交易次数',
    `first_trade_time`          DATETIME        DEFAULT NULL                   COMMENT '首次交易时间',
    `last_fulfill_time`         DATETIME        DEFAULT NULL                   COMMENT '最后履约时间',
    `identity_verified`         TINYINT         DEFAULT 0                      COMMENT '实名认证：0-未认证，1-已认证',
    `phone_verified`            TINYINT         DEFAULT 0                      COMMENT '手机认证：0-未认证，1-已认证',
    `email_verified`            TINYINT         DEFAULT 0                      COMMENT '邮箱认证：0-未认证，1-已认证',
    `create_time`               DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `update_time`               DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`                   TINYINT         DEFAULT 0                      COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_credit_score` (`credit_score`),
    KEY `idx_credit_level` (`credit_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信用表';

-- ---------------------------------------------
-- 表6: credit_node - 信用成长节点表
-- ---------------------------------------------
DROP TABLE IF EXISTS `credit_node`;
CREATE TABLE `credit_node` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT        COMMENT '节点ID',
    `node_code`             VARCHAR(64)     NOT NULL                       COMMENT '节点编码',
    `node_name`             VARCHAR(64)     NOT NULL                       COMMENT '节点名称',
    `node_desc`             VARCHAR(255)    DEFAULT NULL                   COMMENT '节点描述',
    `icon`                  VARCHAR(255)    DEFAULT NULL                   COMMENT '节点图标',
    `required_score`        INT             DEFAULT 0                      COMMENT '所需信用分',
    `required_days`         INT             DEFAULT 0                      COMMENT '所需连续天数',
    `required_trade_count`  INT             DEFAULT 0                      COMMENT '所需交易次数',
    `require_verification`  TINYINT         DEFAULT 0                      COMMENT '是否需要实名认证：0-否，1-是',
    `sort_order`            INT             DEFAULT 0                      COMMENT '排序',
    `status`                TINYINT         DEFAULT 1                      COMMENT '状态：0-禁用，1-启用',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_node_code` (`node_code`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用成长节点表';

-- ---------------------------------------------
-- 表7: credit_violation - 信用违约记录表
-- ---------------------------------------------
DROP TABLE IF EXISTS `credit_violation`;
CREATE TABLE `credit_violation` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT        COMMENT '违约记录ID',
    `user_id`           BIGINT          NOT NULL                       COMMENT '用户ID',
    `violation_type`    VARCHAR(64)     NOT NULL                       COMMENT '违约类型',
    `violation_desc`    VARCHAR(255)    DEFAULT NULL                   COMMENT '违约描述',
    `deduct_score`      INT             DEFAULT 0                      COMMENT '扣除信用分',
    `status`            TINYINT         DEFAULT 1                      COMMENT '状态：1-待处理，2-已处理，3-已修复',
    `violation_time`    DATETIME        DEFAULT NULL                   COMMENT '违约时间',
    `process_time`      DATETIME        DEFAULT NULL                   COMMENT '处理时间',
    `process_remark`    VARCHAR(255)    DEFAULT NULL                   COMMENT '处理备注',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           TINYINT         DEFAULT 0                      COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用违约记录表';

-- ---------------------------------------------
-- 表8: credit_repair_order - 信用修复工单表
-- ---------------------------------------------
DROP TABLE IF EXISTS `credit_repair_order`;
CREATE TABLE `credit_repair_order` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT        COMMENT '工单ID',
    `order_no`          VARCHAR(64)     NOT NULL                       COMMENT '工单编号',
    `user_id`           BIGINT          NOT NULL                       COMMENT '用户ID',
    `violation_id`      BIGINT          NOT NULL                       COMMENT '违约记录ID',
    `repair_reason`     VARCHAR(500)    NOT NULL                       COMMENT '修复理由',
    `prove_images`      VARCHAR(1000)   DEFAULT NULL                   COMMENT '证明图片URL，逗号分隔',
    `status`            TINYINT         DEFAULT 1                      COMMENT '状态：1-审核中，2-已通过，3-已拒绝，4-已取消',
    `auditor_id`        BIGINT          DEFAULT NULL                   COMMENT '审核人ID',
    `audit_remark`      VARCHAR(255)    DEFAULT NULL                   COMMENT '审核备注',
    `audit_time`        DATETIME        DEFAULT NULL                   COMMENT '审核时间',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           TINYINT         DEFAULT 0                      COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_violation_id` (`violation_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用修复工单表';

-- ---------------------------------------------
-- 表9: visitor_record - 访客记录表
-- ---------------------------------------------
DROP TABLE IF EXISTS `visitor_record`;
CREATE TABLE `visitor_record` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT        COMMENT '记录ID',
    `user_id`               BIGINT          NOT NULL                       COMMENT '被访问用户ID',
    `visitor_id`            BIGINT          NOT NULL                       COMMENT '访客用户ID',
    `item_id`               BIGINT          DEFAULT NULL                   COMMENT '访问的物品ID',
    `visit_type`            TINYINT         DEFAULT 1                      COMMENT '访问类型：1-物品详情，2-用户主页',
    `visitor_nickname`      VARCHAR(64)     DEFAULT NULL                   COMMENT '访客昵称（快照）',
    `visitor_avatar`        VARCHAR(255)    DEFAULT NULL                   COMMENT '访客头像（快照）',
    `visitor_credit_level`  TINYINT         DEFAULT 1                      COMMENT '访客信用等级（快照）',
    `visit_time`            DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '访问时间',
    `deleted`               TINYINT         DEFAULT 0                      COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_visitor_id` (`visitor_id`),
    KEY `idx_user_visit_time` (`user_id`, `visit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客记录表';

-- ---------------------------------------------
-- 初始化信用成长节点数据
-- ---------------------------------------------
INSERT INTO `credit_node` (`node_code`, `node_name`, `node_desc`, `icon`, `required_score`, `required_days`, `required_trade_count`, `require_verification`, `sort_order`, `status`) VALUES
('PHONE_VERIFIED',       '手机认证',     '完成手机号绑定和验证',    'icon_phone',      0,   0,   0, 0, 1,  1),
('IDENTITY_VERIFIED',    '实名认证',     '完成身份证实名认证',      'icon_idcard',     0,   0,   0, 1, 2,  1),
('FIRST_TRADE',          '首次交易',     '完成第一笔成功交易',      'icon_trade',      0,   0,   1, 0, 3,  1),
('TRADE_COUNT_5',        '初露锋芒',     '累计完成5笔交易',        'icon_star1',      0,   0,   5, 0, 4,  1),
('CONTINUOUS_DAYS_7',    '坚守一周',     '连续7天履约',            'icon_calendar1',  0,   7,   0, 0, 5,  1),
('TRADE_COUNT_10',       '小有成就',     '累计完成10笔交易',       'icon_star2',      0,   0,  10, 0, 6,  1),
('CREDIT_SCORE_100',     '信用满分',     '信用分达到100分',        'icon_score1',   100,   0,   0, 0, 7,  1),
('CONTINUOUS_DAYS_30',   '月度达人',     '连续30天履约',           'icon_calendar2',  0,  30,   0, 0, 8,  1),
('TRADE_COUNT_50',       '交易大师',     '累计完成50笔交易',       'icon_star3',      0,   0,  50, 0, 9,  1),
('CREDIT_SCORE_500',     '信用精英',     '信用分达到500分',        'icon_score2',   500,   0,   0, 0, 10, 1);

-- ---------------------------------------------
-- 故事表 story
-- ---------------------------------------------
DROP TABLE IF EXISTS `story`;
CREATE TABLE `story` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT        COMMENT '主键ID',
    `user_id`               BIGINT          NOT NULL                       COMMENT '发布用户ID',
    `content`               VARCHAR(500)    NOT NULL                       COMMENT '故事内容',
    `images`                VARCHAR(1000)   DEFAULT NULL                   COMMENT '图片URL，多张逗号分隔',
    `like_count`            INT             DEFAULT 0                      COMMENT '点赞数',
    `view_count`            INT             DEFAULT 0                      COMMENT '浏览数',
    `comment_count`         INT             DEFAULT 0                      COMMENT '评论数',
    `status`                TINYINT         DEFAULT 1                      COMMENT '状态：0-删除，1-正常',
    `create_time`           DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `update_time`           DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               TINYINT         DEFAULT 0                      COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_create` (`user_id`, `create_time`),
    KEY `idx_status_create` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故事表';

-- ---------------------------------------------
-- 物品标签表 item_tag
-- ---------------------------------------------
DROP TABLE IF EXISTS `item_tag`;
CREATE TABLE `item_tag` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT        COMMENT '主键ID',
    `item_id`               BIGINT          NOT NULL                       COMMENT '物品ID',
    `tag_name`              VARCHAR(50)     NOT NULL                       COMMENT '标签名称',
    `create_time`           DATETIME        DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_item_id` (`item_id`),
    KEY `idx_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物品标签关联表';

SET FOREIGN_KEY_CHECKS = 1;
