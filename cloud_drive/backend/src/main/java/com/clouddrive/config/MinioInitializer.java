package com.clouddrive.config;

import com.clouddrive.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * MinIO初始化器
 * 在应用启动时初始化MinIO存储桶
 */
@Component
public class MinioInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(MinioInitializer.class);
    
    @Autowired
    private StorageService storageService;
    
    @Value("${minio.bucketName:cloud-drive}")
    private String bucketName;
    
    @Override
    public void run(String... args) {
        try {
            // 检查并创建存储桶
            if (!storageService.bucketExists(bucketName)) {
                logger.info("创建MinIO存储桶: {}", bucketName);
                storageService.createBucket(bucketName);
                logger.info("MinIO存储桶创建成功: {}", bucketName);
            } else {
                logger.info("MinIO存储桶已存在: {}", bucketName);
            }
        } catch (Exception e) {
            logger.error("初始化MinIO存储桶失败: {}", e.getMessage(), e);
        }
    }
} 