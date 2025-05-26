package com.clouddrive.service.impl;

import com.clouddrive.config.MinioConfig;
import com.clouddrive.exception.BusinessException;
import com.clouddrive.service.StorageService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MinIO存储服务实现类
 */
@Service
public class MinioStorageServiceImpl implements StorageService {
    
    @Autowired
    private MinioClient minioClient;
    
    @Autowired
    private MinioConfig minioConfig;
    
    /**
     * 初始化存储桶，确保应用启动时已正确配置
     */
    @PostConstruct
    public void init() {
        try {
            // 获取配置的存储桶名称
            String bucketName = minioConfig.getBucketName();
            System.out.println("初始化MinIO存储服务...");
            System.out.println("端点: " + minioConfig.getEndpoint());
            System.out.println("存储桶: " + bucketName);
            
            // 确保存储桶存在并设置正确的策略
            createBucket(bucketName);
            
            System.out.println("MinIO存储服务初始化完成");
        } catch (Exception e) {
            System.err.println("MinIO存储服务初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public boolean createBucket(String bucketName) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                
                // 设置存储桶策略，允许公开访问
                setBucketPolicy(bucketName);
                System.out.println("存储桶 " + bucketName + " 创建成功并已设置公开访问策略");
            } else {
                // 如果存储桶已存在，也确保策略正确设置
                setBucketPolicy(bucketName);
                System.out.println("存储桶 " + bucketName + " 已存在，已更新访问策略");
            }
            return true;
        } catch (Exception e) {
            System.err.println("创建或更新存储桶失败: " + e.getMessage());
            e.printStackTrace();
            throw new BusinessException("创建存储桶失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new BusinessException("检查存储桶是否存在失败: " + e.getMessage());
        }
    }
    
    @Override
    public String uploadFile(String bucketName, String objectName, MultipartFile file, String contentType) {
        try {
            InputStream inputStream = file.getInputStream();
            return uploadFile(bucketName, objectName, inputStream, file.getSize(), contentType);
        } catch (IOException e) {
            throw new BusinessException("上传文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public String uploadFile(String bucketName, String objectName, InputStream inputStream, long size, String contentType) {
        try {
            // 确保存储桶存在
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            
            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            
            // 获取文件URL
            return getFileUrl(bucketName, objectName);
        } catch (Exception e) {
            throw new BusinessException("上传文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public InputStream downloadFile(String bucketName, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new BusinessException("下载文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getPresignedObjectUrl(String bucketName, String objectName, int expires) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expires, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new BusinessException("获取文件URL失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean removeFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            throw new BusinessException("删除文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean composeObject(String bucketName, String[] sourceObjectNames, String targetObjectName, String contentType) {
        try {
            // 创建源对象列表
            List<ComposeSource> sources = new ArrayList<>();
            for (String sourceObjectName : sourceObjectNames) {
                sources.add(
                    ComposeSource.builder()
                        .bucket(bucketName)
                        .object(sourceObjectName)
                        .build()
                );
            }
            
            // 执行合并操作
            minioClient.composeObject(
                ComposeObjectArgs.builder()
                    .bucket(bucketName)
                    .object(targetObjectName)
                    .sources(sources)
                    .build()
            );
            
            // 获取合并后的对象信息
            StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(targetObjectName)
                    .build()
            );
            
            // 如果需要设置内容类型，则重新上传对象元数据
            if (contentType != null && !contentType.isEmpty()) {
                // 获取对象数据
                InputStream is = minioClient.getObject(
                    GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(targetObjectName)
                        .build()
                );
                
                // 重新上传对象，设置正确的内容类型
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(targetObjectName)
                        .stream(is, stat.size(), -1)
                        .contentType(contentType)
                        .build()
                );
                
                // 关闭输入流
                is.close();
            }
            
            return true;
        } catch (Exception e) {
            throw new BusinessException("合并文件分片失败: " + e.getMessage());
        }
    }
    
    @Override
    public long getObjectSize(String bucketName, String objectName) {
        try {
            // 获取对象状态信息
            StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
            
            // 返回对象大小
            return stat.size();
        } catch (Exception e) {
            throw new BusinessException("获取对象大小失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean objectExists(String bucketName, String objectName) {
        try {
            // 尝试获取对象状态信息，如果成功则表示对象存在
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            // 如果是对象不存在的错误，返回false
            if (e.errorResponse().code().equals("NoSuchKey") || 
                e.errorResponse().code().equals("NoSuchObject")) {
                return false;
            }
            // 其他错误抛出异常
            throw new BusinessException("检查对象是否存在失败: " + e.getMessage());
        } catch (Exception e) {
            throw new BusinessException("检查对象是否存在失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<String> listObjects(String bucketName, String prefix) {
        try {
            List<String> objectNames = new ArrayList<>();
            
            // 列出指定前缀的所有对象
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .recursive(true)
                    .build()
            );
            
            // 遍历结果，获取对象名称
            for (Result<Item> result : results) {
                try {
                    Item item = result.get();
                    // 只添加非目录对象
                    if (!item.isDir()) {
                        objectNames.add(item.objectName());
                    }
                } catch (Exception e) {
                    // 忽略单个对象的异常，继续处理其他对象
                    System.err.println("获取对象信息失败: " + e.getMessage());
                }
            }
            
            // 调试输出
            System.out.println("列出对象: " + prefix + ", 共找到: " + objectNames.size() + " 个对象");
            for (String name : objectNames) {
                System.out.println(" - " + name);
            }
            
            return objectNames;
        } catch (Exception e) {
            throw new BusinessException("列出对象失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取文件URL
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 文件URL
     */
    private String getFileUrl(String bucketName, String objectName) {
        try {
            // 判断是否为头像文件
            if (objectName.startsWith("avatars/")) {
                // 检查桶是否存在
                boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
                if (!found) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                    setBucketPolicy(bucketName);
                } else {
                    // 确保桶策略正确设置
                    setBucketPolicy(bucketName);
                }
                
                // 对于头像文件，返回直接访问的URL（不带过期时间）
                return minioConfig.getEndpoint() + "/" + bucketName + "/" + objectName;
            } else {
                // 对于其他文件，返回带有过期时间的URL
                return minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucketName)
                                .object(objectName)
                                .expiry(7, TimeUnit.DAYS) // 默认7天有效期
                                .build()
                );
            }
        } catch (Exception e) {
            throw new BusinessException("获取文件URL失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置存储桶策略，允许公开读取
     * @param bucketName 存储桶名称
     */
    private void setBucketPolicy(String bucketName) {
        try {
            System.out.println("正在设置存储桶策略，允许公开读取avatars目录下的文件...");
            
            // 创建一个策略，允许公开读取avatars目录下的文件
            String policy = "{\n" +
                    "    \"Version\": \"2012-10-17\",\n" +
                    "    \"Statement\": [\n" +
                    "        {\n" +
                    "            \"Effect\": \"Allow\",\n" +
                    "            \"Principal\": {\"AWS\": [\"*\"]},\n" +
                    "            \"Action\": [\"s3:GetObject\"],\n" +
                    "            \"Resource\": [\"arn:aws:s3:::" + bucketName + "/avatars/*\"]\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";
            
            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policy)
                            .build()
            );
            
            System.out.println("存储桶策略设置成功!");
        } catch (Exception e) {
            System.err.println("设置存储桶策略失败: " + e.getMessage());
            e.printStackTrace();
            throw new BusinessException("设置存储桶策略失败: " + e.getMessage());
        }
    }
} 