package com.clouddrive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clouddrive.model.entity.File;
import com.clouddrive.model.entity.UserFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 文件服务接口
 */
public interface FileService extends IService<File> {
    
    /**
     * 上传文件
     * @param file 文件
     * @param parentId 父文件夹ID
     * @param relativePath 文件的相对路径 (例如 "folderA/file.txt")
     * @param userId 用户ID
     * @return 文件信息
     */
    UserFile upload(MultipartFile file, Long parentId, String relativePath, Long userId);
    
    /**
     * 创建文件夹
     * @param folderName 文件夹名称
     * @param parentId 父文件夹ID
     * @param userId 用户ID
     * @return 文件夹信息
     */
    UserFile createFolder(String folderName, Long parentId, Long userId);
    
    /**
     * 获取用户文件列表
     * @param parentId 父文件夹ID
     * @param userId 用户ID
     * @return 文件列表
     */
    List<UserFile> getUserFileList(Long parentId, Long userId);
    
    /**
     * 获取用户回收站文件列表
     * @param userId 用户ID
     * @return 文件列表
     */
    List<UserFile> getRecycleBinList(Long userId);
    
    /**
     * 获取回收站中特定文件夹的内容
     * @param folderId 文件夹ID
     * @param userId 用户ID
     * @return 文件列表
     */
    List<UserFile> getDeletedSubFiles(Long folderId, Long userId);
    
    /**
     * 获取文件详情
     * @param userFileId 用户文件ID
     * @param userId 用户ID
     * @return 文件详情
     */
    UserFile getUserFileById(Long userFileId, Long userId);
    
    /**
     * 根据文件ID获取文件信息
     * @param fileId 文件ID
     * @return 文件信息
     */
    File getFileById(Long fileId);
    
    /**
     * 重命名文件或文件夹
     * @param userFileId 用户文件ID
     * @param newFileName 新文件名
     * @param userId 用户ID
     * @return 是否重命名成功
     */
    boolean rename(Long userFileId, String newFileName, Long userId);
    
    /**
     * 移动文件或文件夹
     * @param userFileId 用户文件ID
     * @param targetParentId 目标父文件夹ID
     * @param userId 用户ID
     * @return 是否移动成功
     */
    boolean move(Long userFileId, Long targetParentId, Long userId);
    
    /**
     * 复制文件或文件夹
     * @param userFileId 用户文件ID
     * @param targetParentId 目标父文件夹ID
     * @param userId 用户ID
     * @return 是否复制成功
     */
    boolean copy(Long userFileId, Long targetParentId, Long userId);
    
    /**
     * 删除文件或文件夹（放入回收站）
     * @param userFileId 用户文件ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean delete(Long userFileId, Long userId);
    
    /**
     * 恢复回收站文件
     * @param userFileId 用户文件ID
     * @param userId 用户ID
     * @return 是否恢复成功
     */
    boolean restore(Long userFileId, Long userId);
    
    /**
     * 彻底删除文件（从回收站删除）
     * @param userFileId 用户文件ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteCompletely(Long userFileId, Long userId);
    
    /**
     * 清空回收站
     * @param userId 用户ID
     * @return 是否清空成功
     */
    boolean emptyRecycleBin(Long userId);
    
    /**
     * 切换文件收藏状态
     * @param userFileId 用户文件ID
     * @param isStarred 是否收藏
     * @param userId 用户ID
     * @return 是否设置成功
     */
    boolean toggleStar(Long userFileId, Boolean isStarred, Long userId);
    
    /**
     * 获取文件路径（面包屑导航）
     * @param parentId 父文件夹ID
     * @param userId 用户ID
     * @return 文件路径列表
     */
    List<Map<String, Object>> getFilePath(Long parentId, Long userId);
    
    /**
     * 获取用户所有未删除的文件
     * @param userId 用户ID
     * @return 文件列表
     */
    List<UserFile> getUserAllFiles(Long userId);
    
    /**
     * 获取用户收藏的文件列表
     * @param userId 用户ID
     * @return 收藏文件列表
     */
    List<UserFile> getStarredFiles(Long userId);
    
    /**
     * 更新文件下载次数
     * @param userFileId 用户文件ID
     * @return 是否更新成功
     */
    boolean incrementDownloadCount(Long userFileId);
    
    /**
     * 重建文件索引
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean rebuildFileIndex(Long userId);
    
    /**
     * 保存文件分片
     * @param file 文件分片
     * @param md5 文件MD5值
     * @param chunkIndex 分片索引
     * @param chunks 总分片数
     * @param userId 用户ID
     * @return 分片保存路径
     */
    String saveChunk(MultipartFile file, String md5, Integer chunkIndex, Integer chunks, Long userId) throws Exception;
    
    /**
     * 合并文件分片
     * @param md5 文件MD5值
     * @param fileName 文件名
     * @param parentId 父文件夹ID
     * @param chunks 总分片数
     * @param userId 用户ID
     * @return 合并后的文件信息
     */
    UserFile mergeChunks(String md5, String fileName, Long parentId, Integer chunks, Long userId) throws Exception;
    
    /**
     * 检查文件MD5是否已存在
     * @param md5 文件MD5值
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param userId 用户ID
     * @return 检查结果，包含isExists和uploadedChunks
     */
    Map<String, Object> checkFileMd5(String md5, String fileName, Long fileSize, Long userId);
    
    /**
     * 批量删除文件（移入回收站）
     * @param fileIds 文件ID列表
     * @param userId 用户ID
     * @return 删除结果
     */
    boolean batchDelete(List<Long> fileIds, Long userId);
} 