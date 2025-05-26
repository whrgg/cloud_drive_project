package com.clouddrive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clouddrive.model.entity.UserFile;

import java.util.List;

/**
 * 用户文件服务接口
 */
public interface UserFileService extends IService<UserFile> {
    
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
     * 根据ID获取用户文件
     * @param userFileId 用户文件ID
     * @return 用户文件
     */
    UserFile getById(Long userFileId);
    
    /**
     * 检查文件名在同一目录下是否存在
     * @param fileName 文件名
     * @param parentId 父文件夹ID
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean checkFileNameExists(String fileName, Long parentId, Long userId);
    
    /**
     * 更新删除标志
     * @param userFileId 用户文件ID
     * @param delFlag 删除标志
     * @param userId 用户ID
     * @return 是否更新成功
     */
    boolean updateDelFlag(Long userFileId, Integer delFlag, Long userId);
    
    /**
     * 清空回收站
     * @param userId 用户ID
     * @return 是否清空成功
     */
    boolean emptyRecycleBin(Long userId);
    
    /**
     * 根据文件ID和用户ID查询用户文件
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 用户文件
     */
    UserFile getByFileIdAndUserId(Long fileId, Long userId);
    
    /**
     * 获取文件夹中的所有子文件(包括子文件夹)
     * @param folderId 文件夹ID
     * @param userId 用户ID
     * @return 子文件列表
     */
    List<UserFile> getSubFiles(Long folderId, Long userId);
    
    /**
     * 获取文件夹中的所有已删除子文件(包括回收站中的子文件夹)
     * @param folderId 文件夹ID
     * @param userId 用户ID
     * @return 已删除子文件列表
     */
    List<UserFile> getDeletedSubFiles(Long folderId, Long userId);
    
    /**
     * 更新收藏状态
     * @param userFileId 用户文件ID
     * @param isStarred 是否收藏
     * @param userId 用户ID
     * @return 是否更新成功
     */
    boolean updateStarStatus(Long userFileId, Boolean isStarred, Long userId);
    
    /**
     * 获取用户收藏的文件列表
     * @param userId 用户ID
     * @return 收藏文件列表
     */
    List<UserFile> getStarredFiles(Long userId);
    
    /**
     * 根据文件名、父ID和用户ID查找用户文件记录
     * @param fileName 文件名
     * @param parentId 父文件夹ID
     * @param userId 用户ID
     * @return 用户文件
     */
    UserFile findUserFileByNameAndParent(String fileName, Long parentId, Long userId);
    
    /**
     * 增加下载次数
     * @param userFileId 用户文件ID
     * @return 是否更新成功
     */
    boolean increaseDownloadCount(Long userFileId);
} 