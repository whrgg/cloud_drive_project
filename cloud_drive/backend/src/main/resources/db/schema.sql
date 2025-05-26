-- 创建file表
CREATE TABLE IF NOT EXISTS `file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `object_name` varchar(255) NOT NULL COMMENT '对象存储名称',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_size` bigint(20) NOT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(100) DEFAULT NULL COMMENT '文件类型',
  `md5` varchar(32) DEFAULT NULL COMMENT '文件MD5',
  `usage_count` int(11) DEFAULT '0' COMMENT '使用次数',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态，1:可用，0:不可用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_md5` (`md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- 创建user_file表
CREATE TABLE IF NOT EXISTS `user_file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `file_id` bigint(20) DEFAULT NULL COMMENT '文件ID，如果是目录则为null',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_size` bigint(20) DEFAULT '0' COMMENT '文件大小（字节）',
  `file_type` varchar(100) DEFAULT NULL COMMENT '文件类型',
  `parent_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '父文件夹ID',
  `is_dir` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为文件夹',
  `del_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标志，0:未删除，1:回收站，2:已删除',
  `is_starred` tinyint(1) DEFAULT '0' COMMENT '是否已收藏',
  `download_count` int(11) DEFAULT '0' COMMENT '下载次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户文件表';

-- 创建share表
CREATE TABLE IF NOT EXISTS `share` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '分享ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `user_file_id` bigint(20) NOT NULL COMMENT '用户文件ID',
  `share_code` varchar(50) NOT NULL COMMENT '分享码',
  `extraction_code` varchar(10) DEFAULT NULL COMMENT '提取码，为空表示无密码',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间，为空表示永久有效',
  `view_count` int(11) DEFAULT '0' COMMENT '浏览次数',
  `download_count` int(11) DEFAULT '0' COMMENT '下载次数',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态，1:有效，0:无效',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_code` (`share_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_file_id` (`user_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享表';

-- 创建user表
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '用户头像',
  `storage_size` bigint(20) DEFAULT '10737418240' COMMENT '存储空间大小（字节），默认10GB',
  `used_size` bigint(20) DEFAULT '0' COMMENT '已使用空间大小（字节）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 创建辅助索引表：用于记录用户文件夹路径
CREATE TABLE IF NOT EXISTS `file_path_index` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `user_file_id` bigint(20) NOT NULL COMMENT '用户文件ID',
  `path` varchar(1000) NOT NULL COMMENT '文件路径',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_file_id` (`user_file_id`),
  KEY `idx_path` (`path`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件路径索引表';

-- 创建文件标签表
CREATE TABLE IF NOT EXISTS `file_tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `name` varchar(50) NOT NULL COMMENT '标签名称',
  `color` varchar(20) DEFAULT '#3498db' COMMENT '标签颜色',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件标签表';

-- 创建文件标签关联表
CREATE TABLE IF NOT EXISTS `user_file_tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_file_id` bigint(20) NOT NULL COMMENT '用户文件ID',
  `tag_id` bigint(20) NOT NULL COMMENT '标签ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_tag` (`user_file_id`,`tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件标签关联表';

-- 创建文件操作日志表
CREATE TABLE IF NOT EXISTS `file_operation_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `user_file_id` bigint(20) DEFAULT NULL COMMENT '用户文件ID',
  `operation_type` varchar(20) NOT NULL COMMENT '操作类型：upload/download/delete/restore/rename/move/copy/share',
  `operation_desc` varchar(255) DEFAULT NULL COMMENT '操作描述',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_file_id` (`user_file_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件操作日志表'; 