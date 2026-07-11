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
