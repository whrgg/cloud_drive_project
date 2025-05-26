package com.clouddrive.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO对象存储配置
 */
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 创建MinIO客户端
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
    
    /**
     * 获取MinIO服务器端点
     */
    public String getEndpoint() {
        return endpoint;
    }
    
    /**
     * 获取默认存储桶名称
     */
    public String getBucketName() {
        return bucketName;
    }
} 