package com.clouddrive.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 存储服务接口
 */
public interface StorageService {
    
    /**
     * 创建存储桶
     * @param bucketName 存储桶名称
     * @return 是否创建成功
     */
    boolean createBucket(String bucketName);
    
    /**
     * 检查存储桶是否存在
     * @param bucketName 存储桶名称
     * @return 是否存在
     */
    boolean bucketExists(String bucketName);
    
    /**
     * 上传文件
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param file 文件
     * @param contentType 内容类型
     * @return 文件访问URL
     */
    String uploadFile(String bucketName, String objectName, MultipartFile file, String contentType);
    
    /**
     * 上传文件
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param inputStream 输入流
     * @param size 文件大小
     * @param contentType 内容类型
     * @return 文件访问URL
     */
    String uploadFile(String bucketName, String objectName, InputStream inputStream, long size, String contentType);
    
    /**
     * 下载文件
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 文件输入流
     */
    InputStream downloadFile(String bucketName, String objectName);
    
    /**
     * 获取文件访问URL
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @param expires 过期时间（秒）
     * @return 文件访问URL
     */
    String getPresignedObjectUrl(String bucketName, String objectName, int expires);
    
    /**
     * 删除文件
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 是否删除成功
     */
    boolean removeFile(String bucketName, String objectName);
    
    /**
     * 合并文件分片
     * @param bucketName 存储桶名称
     * @param sourceObjectNames 源对象名称列表
     * @param targetObjectName 目标对象名称
     * @param contentType 内容类型
     * @return 是否合并成功
     */
    boolean composeObject(String bucketName, String[] sourceObjectNames, String targetObjectName, String contentType);
    
    /**
     * 获取对象大小
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 对象大小（字节）
     */
    long getObjectSize(String bucketName, String objectName);
    
    /**
     * 检查对象是否存在
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 对象是否存在
     */
    boolean objectExists(String bucketName, String objectName);
    
    /**
     * 列出指定前缀的所有对象
     * @param bucketName 存储桶名称
     * @param prefix 对象名称前缀
     * @return 对象名称列表
     */
    List<String> listObjects(String bucketName, String prefix);
} 