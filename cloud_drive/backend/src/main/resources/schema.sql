-- 创建用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `avatar` VARCHAR(255) DEFAULT 'default-avatar.png' COMMENT '用户头像',
  `storage_size` BIGINT DEFAULT 10737418240 COMMENT '存储空间大小（字节），默认10GB',
  `used_size` BIGINT DEFAULT 0 COMMENT '已使用空间大小（字节）',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 创建文件表（存储文件元数据）
CREATE TABLE IF NOT EXISTS `file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `object_name` VARCHAR(255) NOT NULL COMMENT '对象存储名称',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
  `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
  `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
  `md5` VARCHAR(32) DEFAULT NULL COMMENT '文件MD5值',
  `usage_count` INT DEFAULT 0 COMMENT '使用次数',
  `status` TINYINT DEFAULT 1 COMMENT '状态，1:可用，0:不可用',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_md5` (`md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- 创建用户文件表（关联用户和文件）
CREATE TABLE IF NOT EXISTS `user_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `file_id` BIGINT DEFAULT NULL COMMENT '文件ID，如果是目录则为null',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父目录ID，0表示根目录',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
  `is_dir` TINYINT NOT NULL DEFAULT 0 COMMENT '是否为目录，1:是，0:否',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
  `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
  `del_flag` TINYINT DEFAULT 0 COMMENT '删除标志，0:未删除，1:回收站，2:已删除',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_parent` (`user_id`, `parent_id`),
  KEY `idx_user_del` (`user_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户文件表';

-- 创建分享表
CREATE TABLE IF NOT EXISTS `share` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分享ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `user_file_id` BIGINT NOT NULL COMMENT '用户文件ID',
  `share_code` VARCHAR(10) NOT NULL COMMENT '分享码',
  `extraction_code` VARCHAR(10) DEFAULT NULL COMMENT '提取码',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间，null表示永不过期',
  `view_count` INT DEFAULT 0 COMMENT '浏览次数',
  `download_count` INT DEFAULT 0 COMMENT '下载次数',
  `status` TINYINT DEFAULT 1 COMMENT '状态，1:有效，0:无效',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_code` (`share_code`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享表'; 