-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username`   VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户名',
    `password`   VARCHAR(128) NOT NULL COMMENT '密码',
    `display_name` VARCHAR(64) COMMENT '显示名称',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 初始用户（方便演示）
INSERT INTO `user` (`username`, `password`, `display_name`) VALUES
('zhangsan', '123456', '张三'),
('lisi', '123456', '李四')
ON DUPLICATE KEY UPDATE `username`=`username`;

-- 用户画像表
CREATE TABLE IF NOT EXISTS `user_profile` (
    `id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`      VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户名',
    `facts`        TEXT COMMENT '用户事实（性别、身份、设备等）',
    `preferences`  TEXT COMMENT '用户偏好（喜欢什么、不喜欢什么）',
    `updated_at`   BIGINT COMMENT '更新时间戳',
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像';

-- ============================================================
-- 第六课：商品结构化信息表（MySQL）
-- 价格、库存等频繁变化的数据存在 MySQL，不放向量数据库
-- ============================================================

CREATE TABLE IF NOT EXISTS `product` (
    `id`          VARCHAR(32)  PRIMARY KEY COMMENT '商品ID',
    `name`        VARCHAR(128) NOT NULL COMMENT '商品名称',
    `brand`       VARCHAR(64)  NOT NULL COMMENT '品牌',
    `category`    VARCHAR(64)  NOT NULL COMMENT '类别',
    `price`       INT          NOT NULL COMMENT '价格（元）',
    `stock`       INT          NOT NULL DEFAULT 100 COMMENT '库存数量',
    `status`      VARCHAR(16)  NOT NULL DEFAULT '在售' COMMENT '状态：在售/缺货/下架',
    `sales`       INT          NOT NULL DEFAULT 0 COMMENT '累计销量',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_category` (`category`),
    INDEX `idx_brand` (`brand`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品结构化信息';

-- 初始商品数据（价格、库存、销量 —— 这些数据会频繁变化）
INSERT INTO `product` (`id`, `name`, `brand`, `category`, `price`, `stock`, `status`, `sales`) VALUES
-- 笔记本电脑
('NB001', 'ThinkPad X1 Carbon Gen12', '联想', '笔记本电脑', 9999, 58, '在售', 342),
('NB002', 'MacBook Pro 14 M3 Pro', '苹果', '笔记本电脑', 14999, 23, '在售', 567),
('NB003', '联想拯救者 Y9000P 2025', '联想', '笔记本电脑', 8999, 12, '在售', 891),
('NB004', '华为 MateBook X Pro 2025', '华为', '笔记本电脑', 11999, 0, '缺货', 234),
('NB005', '戴尔 XPS 14 9440', '戴尔', '笔记本电脑', 12999, 31, '在售', 156),
('NB006', '小米 Book Pro 16 2025', '小米', '笔记本电脑', 5999, 200, '在售', 1203),
('NB007', '华硕 ROG 幻16 Air', '华硕', '笔记本电脑', 12999, 8, '在售', 78),
('NB008', 'ThinkPad T14s Gen5', '联想', '笔记本电脑', 7999, 67, '在售', 445),
('NB009', 'MacBook Air 15 M3', '苹果', '笔记本电脑', 9499, 45, '在售', 678),
('NB010', '机械革命 蛟龙16 Pro', '机械革命', '笔记本电脑', 6499, 150, '在售', 2340),
('NB011', '戴尔 灵越14 Plus 7440', '戴尔', '笔记本电脑', 6999, 89, '在售', 567),
('NB012', '惠普 暗影精灵10', '惠普', '笔记本电脑', 7999, 35, '在售', 789),
-- 手机
('PH001', 'iPhone 16 Pro Max', '苹果', '手机', 9999, 120, '在售', 2345),
('PH002', '华为 Mate 70 Pro', '华为', '手机', 6499, 80, '在售', 1876),
('PH003', '小米 15 Pro', '小米', '手机', 4999, 300, '在售', 5678),
('PH004', 'OPPO Find X8 Ultra', 'OPPO', '手机', 5999, 45, '在售', 890),
('PH005', '三星 Galaxy S25 Ultra', '三星', '手机', 9999, 60, '在售', 1234),
('PH006', 'vivo X200 Pro', 'vivo', '手机', 4499, 200, '在售', 3456),
-- 耳机
('HP001', 'AirPods Pro 2', '苹果', '耳机', 1899, 500, '在售', 8901),
('HP002', '索尼 WH-1000XM5', '索尼', '耳机', 2299, 80, '在售', 2345),
('HP003', '华为 FreeBuds Pro 3', '华为', '耳机', 1199, 300, '在售', 4567),
('HP004', '小米 Buds 5 Pro', '小米', '耳机', 699, 1000, '在售', 12340),
-- 平板
('TB001', 'iPad Pro 13 M4', '苹果', '平板', 10999, 35, '在售', 567),
('TB002', '华为 MatePad Pro 13.2', '华为', '平板', 5699, 60, '在售', 1234),
('TB003', '小米 Pad 7 Pro', '小米', '平板', 2999, 200, '在售', 3456),
('TB004', 'iPad Air 11 M2', '苹果', '平板', 4799, 100, '在售', 2345)
ON DUPLICATE KEY UPDATE `name`=`name`;
