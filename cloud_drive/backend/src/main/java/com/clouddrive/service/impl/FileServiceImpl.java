package com.clouddrive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clouddrive.listener.FileEvent;
import com.clouddrive.mapper.FileMapper;
import com.clouddrive.mapper.UserFileMapper;
import com.clouddrive.model.entity.File;
import com.clouddrive.model.entity.UserFile;
import com.clouddrive.service.FileService;
import com.clouddrive.service.StorageService;
import com.clouddrive.service.UserService;
import com.clouddrive.service.UserFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文件服务实现类
 */
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    @Autowired
    private StorageService storageService;
    
    @Autowired
    private UserFileMapper userFileMapper;
    
    @Autowired
    private FileMapper fileMapper;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserFileService userFileService;
    
    @Value("${minio.bucketName:cloud-drive}")
    private String bucketName;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // 辅助方法：逐级创建或获取文件夹ID
    private Long getOrCreateFolderHierarchy(Long initialParentId, String folderPath, Long userId) {
        if (folderPath == null || folderPath.isEmpty()) {
            return initialParentId;
        }

        // 分割路径，处理Windows和Unix风格的路径分隔符
        String normalizedPath = folderPath.replace('\\', '/');
        
        // 不再检测和处理重复路径，允许嵌套同名文件夹
        String[] folderNames = normalizedPath.split("/");
        Long currentParentId = initialParentId;

        System.out.println("创建文件夹层次结构: " + folderPath + ", 初始父ID: " + initialParentId);
        
        for (String folderName : folderNames) {
            if (folderName.isEmpty()) {
                continue;
            }
            
            System.out.println("处理文件夹: " + folderName + ", 父ID: " + currentParentId);
            
            // 查找是否已存在该文件夹
            UserFile existingFolder = userFileService.findUserFileByNameAndParent(folderName, currentParentId, userId);
            if (existingFolder != null && existingFolder.getIsDir()) {
                System.out.println("文件夹已存在: " + folderName + ", ID: " + existingFolder.getId());
                currentParentId = existingFolder.getId();
            } else if (existingFolder == null) {
                // 如果不存在，则创建
                UserFile newFolder = new UserFile();
                newFolder.setFileName(folderName);
                newFolder.setFileSize(0L);
                newFolder.setFileType("folder");
                newFolder.setIsDir(true);
                newFolder.setDelFlag(0);
                newFolder.setUserId(userId);
                newFolder.setParentId(currentParentId);
                newFolder.setCreateTime(new Date());
                newFolder.setUpdateTime(new Date());
                userFileService.save(newFolder);
                System.out.println("创建新文件夹: " + folderName + ", ID: " + newFolder.getId());
                currentParentId = newFolder.getId();
            } else {
                // 存在同名文件但不是文件夹，这是一个冲突
                throw new RuntimeException("目标路径中存在同名文件，无法创建文件夹: " + folderName);
            }
        }
        
        System.out.println("文件夹层次结构创建完成，最终父ID: " + currentParentId);
        return currentParentId;
    }

    @Override
    @Transactional
    public UserFile upload(MultipartFile file, Long parentId, String relativePath, Long userId) {
        try {
            String actualFileName = file.getOriginalFilename();
            Long finalParentId = parentId;
            String objectPathPrefix = ""; // 用于构建MinIO的objectName中的路径部分

            if (relativePath != null && !relativePath.isEmpty()) {
                System.out.println("处理上传文件的相对路径: " + relativePath);
                
                // 处理相对路径
                int lastSeparatorIndex = relativePath.lastIndexOf('/');
                if (lastSeparatorIndex != -1) {
                    // 提取文件夹路径和文件名
                    String folderPath = relativePath.substring(0, lastSeparatorIndex);
                    actualFileName = relativePath.substring(lastSeparatorIndex + 1);
                    
                    // 创建文件夹层次结构并获取最终的父文件夹ID
                    // 不再检测和处理重复路径，允许嵌套同名文件夹
                    finalParentId = getOrCreateFolderHierarchy(parentId, folderPath, userId);
                    objectPathPrefix = folderPath + "/"; // MinIO的路径需要包含这些创建的文件夹
                } else {
                    // relativePath 就是文件名，没有子文件夹
                    actualFileName = relativePath;
                    // finalParentId 仍然是传入的 parentId
                    // objectPathPrefix 保持为空
                }
            }
            
            // 检查最终父目录下是否已存在同名文件/文件夹
            UserFile existingSameNameEntry = userFileService.findUserFileByNameAndParent(actualFileName, finalParentId, userId);
            if (existingSameNameEntry != null) {
                throw new RuntimeException("目标位置已存在同名文件或文件夹: " + actualFileName);
            }

            // 1. 先保存文件元数据到File表
            File fileEntity = new File();
            fileEntity.setFileName(actualFileName); // 使用解析后的文件名
            fileEntity.setFileSize(file.getSize());
            fileEntity.setFileType(file.getContentType());
            
            // 构建对象存储路径
            String minioObjectName;
            if (!objectPathPrefix.isEmpty()) {
                // 如果有相对路径，使用userId/relativePath作为MinIO中的路径
                minioObjectName = userId + "/" + relativePath;
            } else {
                // 没有文件夹层级，直接在parentId下
                if (parentId == 0L) {
                    minioObjectName = userId + "/" + actualFileName;
                } else {
                    minioObjectName = userId + "/" + parentId + "/" + actualFileName;
                }
            }

            fileEntity.setObjectName(minioObjectName);
            
            fileEntity.setUsageCount(1); 
            fileEntity.setStatus(1); 
            fileEntity.setCreateTime(new Date());
            fileEntity.setUpdateTime(new Date());
            
            fileMapper.insert(fileEntity);
            
            // 2. 上传文件到MinIO
            storageService.uploadFile(bucketName, minioObjectName, file, file.getContentType());
            
            // 3. 创建用户文件记录
            UserFile userFile = new UserFile();
            userFile.setFileName(actualFileName); // 使用解析后的文件名
            userFile.setFileSize(file.getSize());
            userFile.setFileType(file.getContentType());
            userFile.setIsDir(false);
            userFile.setDelFlag(0); 
            userFile.setUserId(userId);
            userFile.setParentId(finalParentId); // 使用最终确定的父ID
            userFile.setFileId(fileEntity.getId()); 
            userFile.setCreateTime(new Date());
            userFile.setUpdateTime(new Date());
            
            userFileService.save(userFile);
            
            // 4. 更新用户已使用空间
            userService.updateUsedSize(userId, file.getSize());
            
            // 5. 发布文件上传事件，用于索引
            eventPublisher.publishEvent(FileEvent.createUploadEvent(userFile));
            
            return userFile;
        } catch (Exception e) {
            // 对于 Transactional 方法，如果希望回滚，需要抛出 RuntimeException
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("上传文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public UserFile createFolder(String folderName, Long parentId, Long userId) {
        // 检查同级目录下是否已存在同名文件夹
        boolean exists = userFileService.checkFileNameExists(folderName, parentId, userId);
        if (exists) {
            throw new RuntimeException("同名文件夹已存在");
        }
        
        // 创建文件夹记录
        UserFile folder = new UserFile();
        folder.setFileName(folderName);
        folder.setFileSize(0L);
        folder.setFileType("folder");
        folder.setIsDir(true);
        folder.setDelFlag(0); // 未删除
        folder.setUserId(userId);
        folder.setParentId(parentId);
        folder.setCreateTime(new Date());
        folder.setUpdateTime(new Date());
        
        // 保存到数据库
        userFileService.save(folder);
        
        return folder;
    }

    @Override
    public List<UserFile> getUserFileList(Long parentId, Long userId) {
        // 从数据库中获取用户文件列表
        return userFileService.getUserFileList(parentId, userId);
    }

    @Override
    public List<UserFile> getRecycleBinList(Long userId) {
        // 从数据库获取回收站文件列表
        return userFileService.getRecycleBinList(userId);
    }

    @Override
    public UserFile getUserFileById(Long userFileId, Long userId) {
        // 从数据库获取用户文件详情
        LambdaQueryWrapper<UserFile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFile::getId, userFileId);
        return userFileService.getById(userFileId);
    }

    @Override
    public File getFileById(Long fileId) {
        // 从数据库查询文件元数据
        return fileMapper.selectById(fileId);
    }

    @Override
    @Transactional
    public boolean rename(Long userFileId, String newFileName, Long userId) {
        // 检查同级目录下是否已存在同名文件
        UserFile userFile = getUserFileById(userFileId, userId);
        if (userFile == null) {
            return false;
        }
        
        boolean exists = userFileService.checkFileNameExists(newFileName, userFile.getParentId(), userId);
        if (exists) {
            throw new RuntimeException("同名文件已存在");
        }
        
        // 更新文件名
        LambdaUpdateWrapper<UserFile> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserFile::getId, userFileId)
                    .eq(UserFile::getUserId, userId)
                    .set(UserFile::getFileName, newFileName)
                    .set(UserFile::getUpdateTime, new Date());
        boolean result = userFileService.update(updateWrapper);
        
        // 发布文件更新事件
        if (result) {
            eventPublisher.publishEvent(FileEvent.createUpdateEvent(userFile));
        }
        
        return result;
    }

    @Override
    @Transactional
    public boolean move(Long userFileId, Long targetParentId, Long userId) {
        // 更新父文件夹ID
        LambdaUpdateWrapper<UserFile> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserFile::getId, userFileId)
                    .eq(UserFile::getUserId, userId)
                    .set(UserFile::getParentId, targetParentId)
                    .set(UserFile::getUpdateTime, new Date());
        return userFileService.update(updateWrapper);
    }

    @Override
    @Transactional
    public boolean copy(Long userFileId, Long targetParentId, Long userId) {
        // 获取源文件信息
        UserFile sourceFile = getUserFileById(userFileId, userId);
        if (sourceFile == null) {
            return false;
        }
        
        // 创建文件副本
        UserFile newFile = new UserFile();
        newFile.setFileName(sourceFile.getFileName());
        newFile.setFileSize(sourceFile.getFileSize());
        newFile.setFileType(sourceFile.getFileType());
        newFile.setIsDir(sourceFile.getIsDir());
        newFile.setDelFlag(0); // 未删除
        newFile.setUserId(userId);
        newFile.setParentId(targetParentId);
        newFile.setCreateTime(new Date());
        newFile.setUpdateTime(new Date());
        
        // 保存到数据库
        return userFileService.save(newFile);
    }

    @Override
    @Transactional
    public boolean delete(Long userFileId, Long userId) {
        // 获取文件信息，用于计算空间变化
        UserFile userFile = getUserFileById(userFileId, userId);
        if (userFile == null) {
            return false;
        }
        
        // 如果是文件（不是目录），需要更新用户已使用空间
        if (!userFile.getIsDir() && userFile.getFileSize() != null) {
            // 将文件移动到回收站不会立即释放空间，所以这里不需要更新用户已使用空间
            // 当彻底删除时才会更新
        }
        
        // 将文件移动到回收站
        boolean success = userFileService.updateDelFlag(userFileId, 1, userId);
        
        // 如果是文件夹，递归处理其中的文件和子文件夹
        if (userFile.getIsDir()) {
            // 获取文件夹下的所有子文件和子文件夹
            List<UserFile> subFiles = userFileService.getSubFiles(userFileId, userId);
            
            // 递归删除每个子文件和子文件夹
            for (UserFile subFile : subFiles) {
                delete(subFile.getId(), userId);
            }
        }
        
        // 发布文件删除事件
        if (userFile != null) {
            eventPublisher.publishEvent(FileEvent.createDeleteEvent(userFileId));
        }
        
        return success;
    }

    @Override
    @Transactional
    public boolean restore(Long userFileId, Long userId) {
        // 获取文件信息，用于计算空间变化
        UserFile userFile = getUserFileById(userFileId, userId);
        if (userFile == null) {
            return false;
        }
        
        // 如果是文件（不是目录），需要更新用户已使用空间
        if (!userFile.getIsDir() && userFile.getFileSize() != null) {
            // 从回收站恢复文件不会增加空间使用，因为回收站中的文件仍然计入用户空间
            // 当彻底删除时才会更新
        }
        
        // 从回收站恢复文件
        boolean success = userFileService.updateDelFlag(userFileId, 0, userId);
        
        // 如果是文件夹，递归处理其中的文件和子文件夹
        if (userFile.getIsDir()) {
            // 获取文件夹下所有被删除的子文件和子文件夹（包括回收站中的）
            List<UserFile> subFiles = userFileService.getDeletedSubFiles(userFileId, userId);
            
            // 递归恢复每个子文件和子文件夹
            for (UserFile subFile : subFiles) {
                restore(subFile.getId(), userId);
            }
        }
        
        return success;
    }

    @Override
    @Transactional
    public boolean deleteCompletely(Long userFileId, Long userId) {
        // 获取用户文件
        UserFile userFile = getUserFileById(userFileId, userId);
        if (userFile == null) {
            return false;
        }
        
        boolean success = false;
        
        // 如果是文件夹，先递归删除其中的文件和子文件夹
        if (userFile.getIsDir()) {
            // 获取文件夹下的所有子文件和子文件夹（包括回收站中的）
            List<UserFile> subFiles = userFileService.getDeletedSubFiles(userFileId, userId);
            
            // 递归彻底删除每个子文件和子文件夹
            for (UserFile subFile : subFiles) {
                deleteCompletely(subFile.getId(), userId);
            }
            
            // 删除文件夹记录
            LambdaQueryWrapper<UserFile> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserFile::getId, userFileId)
                       .eq(UserFile::getUserId, userId);
            success = userFileService.remove(queryWrapper);
        } else {
            // 如果是文件，处理文件元数据
            if (userFile.getFileId() != null) {
                // 减少文件使用次数
                fileMapper.decreaseUsageCount(userFile.getFileId());
                
                // 查询文件使用次数
                File fileEntity = fileMapper.selectById(userFile.getFileId());
                if (fileEntity != null && (fileEntity.getUsageCount() == null || fileEntity.getUsageCount() <= 0)) {
                    // 如果使用次数为0，从对象存储中删除文件
                    storageService.removeFile(bucketName, fileEntity.getObjectName());
                    
                    // 从数据库删除文件元数据
                    fileMapper.deleteById(fileEntity.getId());
                }
                
                // 更新用户已使用空间（减少）
                if (userFile.getFileSize() != null && userFile.getFileSize() > 0) {
                    userService.updateUsedSize(userId, -userFile.getFileSize());
                    log.info("删除文件 {} ({}), 释放空间: {} bytes", userFile.getFileName(), userFileId, userFile.getFileSize());
                }
            }
            
            // 删除用户文件记录
            LambdaQueryWrapper<UserFile> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserFile::getId, userFileId)
                       .eq(UserFile::getUserId, userId);
            success = userFileService.remove(queryWrapper);
        }
        
        // 发布文件删除事件
        if (userFile != null) {
            eventPublisher.publishEvent(FileEvent.createDeleteEvent(userFileId));
        }
        
        return success;
    }

    @Override
    @Transactional
    public boolean emptyRecycleBin(Long userId) {
        // 获取回收站文件列表
        List<UserFile> recycleBinList = getRecycleBinList(userId);
        
        // 计算总大小
        long totalSize = 0;
        for (UserFile userFile : recycleBinList) {
            if (!userFile.getIsDir() && userFile.getFileSize() != null) {
                totalSize += userFile.getFileSize();
                
                // 如果是文件，处理文件元数据
                if (userFile.getFileId() != null) {
                    // 减少文件使用次数
                    fileMapper.decreaseUsageCount(userFile.getFileId());
                    
                    // 查询文件使用次数
                    File fileEntity = fileMapper.selectById(userFile.getFileId());
                    if (fileEntity != null && (fileEntity.getUsageCount() == null || fileEntity.getUsageCount() <= 0)) {
                        // 如果使用次数为0，从对象存储中删除文件
                        try {
                            storageService.removeFile(bucketName, fileEntity.getObjectName());
                            
                            // 从数据库删除文件元数据
                            fileMapper.deleteById(fileEntity.getId());
                        } catch (Exception e) {
                            log.error("清空回收站时删除文件失败: {}", e.getMessage());
                        }
                    }
                }
            } else if (userFile.getIsDir()) {
                // 对于文件夹，递归获取其中的文件大小
                // 获取文件夹下的所有子文件和子文件夹（包括回收站中的）
                List<UserFile> subFiles = getDeletedSubFiles(userFile.getId(), userId);
                for (UserFile subFile : subFiles) {
                    if (!subFile.getIsDir() && subFile.getFileSize() != null) {
                        totalSize += subFile.getFileSize();
                        
                        // 如果是文件，处理文件元数据
                        if (subFile.getFileId() != null) {
                            // 减少文件使用次数
                            fileMapper.decreaseUsageCount(subFile.getFileId());
                            
                            // 查询文件使用次数
                            File fileEntity = fileMapper.selectById(subFile.getFileId());
                            if (fileEntity != null && (fileEntity.getUsageCount() == null || fileEntity.getUsageCount() <= 0)) {
                                // 如果使用次数为0，从对象存储中删除文件
                                try {
                                    storageService.removeFile(bucketName, fileEntity.getObjectName());
                                    
                                    // 从数据库删除文件元数据
                                    fileMapper.deleteById(fileEntity.getId());
                                } catch (Exception e) {
                                    log.error("清空回收站时删除子文件失败: {}", e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 清空回收站
        boolean result = userFileService.emptyRecycleBin(userId);
        
        // 更新用户已使用空间
        if (result && totalSize > 0) {
            userService.updateUsedSize(userId, -totalSize);
            log.info("清空回收站释放空间: {} bytes, 用户ID: {}", totalSize, userId);
        }
        
        return result;
    }

    @Override
    @Transactional
    public boolean toggleStar(Long userFileId, Boolean isStarred, Long userId) {
        // 更新收藏状态
        return userFileService.updateStarStatus(userFileId, isStarred, userId);
    }

    @Override
    public List<Map<String, Object>> getFilePath(Long parentId, Long userId) {
        List<Map<String, Object>> path = new ArrayList<>();
        
        // 如果是根目录，直接返回空列表
        if (parentId == 0L) {
            return path;
        }
        
        // 构建文件路径
        UserFile currentFolder = null;
        Long currentParentId = parentId;
        
        while (currentParentId != 0L) {
            // 查找当前父文件夹
            currentFolder = userFileService.getById(currentParentId);
            
            // 如果找不到父文件夹，或者不是当前用户的文件夹，中断循环
            if (currentFolder == null || !currentFolder.getUserId().equals(userId) || !currentFolder.getIsDir()) {
                break;
            }
            
            // 将父文件夹添加到路径前面
            Map<String, Object> folderInfo = new HashMap<>();
            folderInfo.put("id", currentFolder.getId());
            folderInfo.put("name", currentFolder.getFileName());
            path.add(0, folderInfo);
            
            // 继续查找上一级父文件夹
            currentParentId = currentFolder.getParentId();
        }
        
        return path;
    }

    @Override
    public List<UserFile> getUserAllFiles(Long userId) {
        // 获取用户所有文件（不包括回收站）
        LambdaQueryWrapper<UserFile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserFile::getUserId, userId)
                   .eq(UserFile::getDelFlag, 0);
        return userFileService.list(queryWrapper);
    }

    @Override
    public List<UserFile> getStarredFiles(Long userId) {
        // 从数据库获取收藏文件列表
        return userFileService.getStarredFiles(userId);
    }

    @Override
    public boolean incrementDownloadCount(Long userFileId) {
        // 增加下载次数
        return userFileService.increaseDownloadCount(userFileId);
    }

    @Override
    public List<UserFile> getDeletedSubFiles(Long folderId, Long userId) {
        // 获取回收站中特定文件夹的内容
        return userFileService.getDeletedSubFiles(folderId, userId);
    }

    /**
     * 重建所有文件索引
     * @param userId 用户ID
     * @return 是否成功
     */
    @Override
    public boolean rebuildFileIndex(Long userId) {
        // 获取用户所有未删除的文件
        List<UserFile> userFiles = getUserAllFiles(userId);
        
        // 发布索引重建事件
        for (UserFile userFile : userFiles) {
            if (!userFile.getIsDir()) {
                eventPublisher.publishEvent(FileEvent.createUpdateEvent(userFile));
            }
        }
        
        return true;
    }

    @Override
    public String saveChunk(MultipartFile file, String md5, Integer chunkIndex, Integer chunks, Long userId) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("上传文件分片不能为空");
        }
        
        // 创建临时文件夹存储分片，路径为：userId/md5/
        String chunkFolderPath = userId + "/" + md5 + "/";
        String chunkFileName = chunkIndex + ".chunk";
        String objectName = chunkFolderPath + chunkFileName;
        
        // 上传分片到对象存储
        storageService.uploadFile(bucketName, objectName, file, "application/octet-stream");
        
        return objectName;
    }
    
    @Override
    @Transactional
    public UserFile mergeChunks(String md5, String fileName, Long parentId, Integer chunks, Long userId) throws Exception {
        // 1. 获取所有实际存在的分片
        String chunkFolderPath = userId + "/" + md5 + "/";
        
        System.out.println("开始合并分片，用户ID: " + userId + ", MD5: " + md5 + ", 文件名: " + fileName + ", 前端报告分片数: " + chunks);
        
        // 使用 listObjects 获取所有已上传的分片
        List<String> existingObjects = storageService.listObjects(bucketName, chunkFolderPath);
        Set<Integer> uploadedChunks = new HashSet<>();
        int maxChunkIndex = -1;
        
        System.out.println("找到对象数量: " + existingObjects.size());
        
        // 解析对象名称，提取分片索引
        for (String objectName : existingObjects) {
            if (objectName.endsWith(".chunk")) {
                String chunkFileName = objectName.substring(objectName.lastIndexOf('/') + 1);
                String chunkIndexStr = chunkFileName.replace(".chunk", "");
                try {
                    int chunkIndex = Integer.parseInt(chunkIndexStr);
                    uploadedChunks.add(chunkIndex);
                    if (chunkIndex > maxChunkIndex) {
                        maxChunkIndex = chunkIndex;
                    }
                    System.out.println("找到分片: " + chunkIndex);
                } catch (NumberFormatException e) {
                    // 忽略无法解析的文件名
                    System.err.println("无法解析分片索引: " + chunkFileName);
                }
            }
        }
        
        // 确定实际分片数量（使用实际上传的最大分片索引+1，而不是前端传递的chunks）
        int actualChunks = maxChunkIndex + 1;
        System.out.println("已上传分片数量: " + uploadedChunks.size() + ", 最大分片索引: " + maxChunkIndex + ", 实际分片数: " + actualChunks + ", 前端报告分片数: " + chunks);
        
        if (actualChunks != chunks) {
            System.out.println("警告: 实际分片数(" + actualChunks + ")与前端报告的分片数(" + chunks + ")不一致，将使用实际分片数");
        }
        
        // 检查是否有缺失的分片（只检查0到maxChunkIndex范围内的分片）
        List<Integer> missingChunks = new ArrayList<>();
        for (int i = 0; i < actualChunks; i++) {
            if (!uploadedChunks.contains(i)) {
                missingChunks.add(i);
                System.err.println("缺少分片: " + i);
            }
        }
        
        // 如果有分片缺失，尝试使用 objectExists 方法再次检查
        if (!missingChunks.isEmpty()) {
            System.out.println("使用 objectExists 方法再次检查缺失的分片");
            List<Integer> stillMissingChunks = new ArrayList<>();
            for (Integer chunkIndex : missingChunks) {
                String chunkName = chunkFolderPath + chunkIndex + ".chunk";
                if (!storageService.objectExists(bucketName, chunkName)) {
                    stillMissingChunks.add(chunkIndex);
                    System.err.println("确认缺少分片: " + chunkIndex);
                } else {
                    System.out.println("分片存在但未被列出: " + chunkIndex);
                    // 添加到已上传分片集合中
                    uploadedChunks.add(chunkIndex);
                }
            }
            
            // 更新缺失分片列表
            missingChunks = stillMissingChunks;
        }
        
        // 如果仍然有分片缺失，抛出异常
        if (!missingChunks.isEmpty()) {
            throw new Exception("分片文件不完整，缺少分片: " + missingChunks);
        }
        
        // 创建源对象名称数组
        String[] sourceObjectNames = new String[actualChunks];
        for (int i = 0; i < actualChunks; i++) {
            sourceObjectNames[i] = chunkFolderPath + i + ".chunk";
        }
        
        System.out.println("所有分片都已存在，开始合并 " + actualChunks + " 个分片");
        
        // 2. 创建文件元数据
        File fileEntity = new File();
        fileEntity.setFileName(fileName);
        fileEntity.setFileType(getFileType(fileName));
        fileEntity.setMd5(md5); // 设置文件MD5值
        
        // 合并后的文件路径
        String mergedFilePath = userId + "/" + md5 + "/" + fileName;
        fileEntity.setObjectName(mergedFilePath);
        
        // 3. 请求对象存储服务合并分片
        boolean mergeResult = storageService.composeObject(
            bucketName, 
            sourceObjectNames, 
            mergedFilePath, 
            fileEntity.getFileType()
        );
        
        if (!mergeResult) {
            throw new Exception("合并文件分片失败");
        }
        
        // 获取合并后的文件大小
        // 这里简化处理，实际项目中应该通过对象存储API获取文件大小
        long totalSize = 0;
        try {
            // 获取合并后文件的实际大小
            totalSize = storageService.getObjectSize(bucketName, mergedFilePath);
        } catch (Exception e) {
            // 忽略异常，使用默认大小
            totalSize = 1024 * 1024; // 临时设置为1MB
        }
        
        fileEntity.setFileSize(totalSize);
        fileEntity.setUsageCount(1);
        fileEntity.setStatus(1);
        fileEntity.setCreateTime(new Date());
        fileEntity.setUpdateTime(new Date());
        
        fileMapper.insert(fileEntity);
        
        // 4. 创建用户文件记录
        UserFile userFile = new UserFile();
        userFile.setFileName(fileName);
        userFile.setFileSize(totalSize);
        userFile.setFileType(fileEntity.getFileType());
        userFile.setIsDir(false);
        userFile.setDelFlag(0);
        userFile.setUserId(userId);
        userFile.setParentId(parentId);
        userFile.setFileId(fileEntity.getId());
        userFile.setCreateTime(new Date());
        userFile.setUpdateTime(new Date());
        
        userFileService.save(userFile);
        
        // 5. 更新用户已使用空间
        userService.updateUsedSize(userId, totalSize);
        
        // 6. 发布文件上传事件，用于索引
        eventPublisher.publishEvent(FileEvent.createUploadEvent(userFile));
        
        // 7. 清理分片文件
        for (String sourceObjectName : sourceObjectNames) {
            try {
                storageService.removeFile(bucketName, sourceObjectName);
            } catch (Exception e) {
                // 忽略清理分片的异常
            }
        }
        
        return userFile;
    }
    
    /**
     * 根据文件名获取文件类型
     * @param fileName 文件名
     * @return 文件类型
     */
    private String getFileType(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1).toLowerCase();
        }
        
        // 根据扩展名返回MIME类型
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt":
                return "text/plain";
            case "html":
            case "htm":
                return "text/html";
            case "mp3":
                return "audio/mpeg";
            case "mp4":
                return "video/mp4";
            case "zip":
                return "application/zip";
            case "rar":
                return "application/x-rar-compressed";
            default:
                return "application/octet-stream";
        }
    }

    @Override
    public Map<String, Object> checkFileMd5(String md5, String fileName, Long fileSize, Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("isExists", false);
        result.put("uploadedChunks", new ArrayList<Integer>());
        
        try {
            // 1. 检查是否有相同MD5的文件
            LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(File::getMd5, md5);
            File existingFile = fileMapper.selectOne(queryWrapper);
            
            if (existingFile != null) {
                // 文件已存在，可以实现秒传
                // 创建用户文件记录
                UserFile userFile = new UserFile();
                userFile.setFileName(fileName);
                userFile.setFileSize(existingFile.getFileSize());
                userFile.setFileType(existingFile.getFileType());
                userFile.setIsDir(false);
                userFile.setDelFlag(0);
                userFile.setUserId(userId);
                userFile.setParentId(0L); // 默认放在根目录，前端会传递实际的parentId
                userFile.setFileId(existingFile.getId());
                userFile.setCreateTime(new Date());
                userFile.setUpdateTime(new Date());
                
                userFileService.save(userFile);
                
                // 增加文件使用次数
                fileMapper.increaseUsageCount(existingFile.getId());
                
                // 更新用户已使用空间
                userService.updateUsedSize(userId, existingFile.getFileSize());
                
                // 返回文件已存在的信息
                result.put("isExists", true);
                result.put("fileId", userFile.getId());
                return result;
            }
            
            // 2. 检查是否有已上传的分片
            String chunkFolderPath = userId + "/" + md5 + "/";
            
            System.out.println("检查文件MD5: " + md5 + ", 文件名: " + fileName);
            
            // 使用 listObjects 方法获取已上传的分片
            List<String> existingObjects = storageService.listObjects(bucketName, chunkFolderPath);
            Set<Integer> uploadedChunks = new HashSet<>();
            int maxChunkIndex = -1;
            
            System.out.println("找到对象数量: " + existingObjects.size());
            
            // 解析对象名称，提取分片索引
            for (String objectName : existingObjects) {
                if (objectName.endsWith(".chunk")) {
                    String chunkFileName = objectName.substring(objectName.lastIndexOf('/') + 1);
                    String chunkIndexStr = chunkFileName.replace(".chunk", "");
                    try {
                        int chunkIndex = Integer.parseInt(chunkIndexStr);
                        uploadedChunks.add(chunkIndex);
                        if (chunkIndex > maxChunkIndex) {
                            maxChunkIndex = chunkIndex;
                        }
                        System.out.println("找到分片: " + chunkIndex);
                    } catch (NumberFormatException e) {
                        // 忽略无法解析的文件名
                        System.err.println("无法解析分片索引: " + chunkFileName);
                    }
                }
            }
            
            System.out.println("已上传分片数量: " + uploadedChunks.size() + ", 最大分片索引: " + maxChunkIndex);
            
            // 检查是否有缺失的分片，使用objectExists方法进行二次验证
            if (maxChunkIndex >= 0) {
                for (int i = 0; i <= maxChunkIndex; i++) {
                    if (!uploadedChunks.contains(i)) {
                        String chunkName = chunkFolderPath + i + ".chunk";
                        if (storageService.objectExists(bucketName, chunkName)) {
                            uploadedChunks.add(i);
                            System.out.println("分片存在但未被列出: " + i);
                        }
                    }
                }
            }
            
            result.put("uploadedChunks", new ArrayList<>(uploadedChunks));
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("检查文件MD5失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量删除文件（移入回收站）
     * @param fileIds 文件ID列表
     * @param userId 用户ID
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDelete(List<Long> fileIds, Long userId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return false;
        }
        
        boolean success = true;
        for (Long fileId : fileIds) {
            try {
                // 循环调用单个文件删除的方法
                success = success && delete(fileId, userId);
            } catch (Exception e) {
                log.error("批量删除文件失败, fileId: {}, userId: {}, error: {}", fileId, userId, e.getMessage());
                success = false;
            }
        }
        
        return success;
    }
} 