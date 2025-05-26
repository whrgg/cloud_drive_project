package com.clouddrive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clouddrive.model.entity.UserFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户文件Mapper接口
 */
@Mapper
public interface UserFileMapper extends BaseMapper<UserFile> {
    
    /**
     * 获取用户文件列表
     * @param parentId 父文件夹ID
     * @param userId 用户ID
     * @return 文件列表
     */
    List<UserFile> selectUserFileList(@Param("parentId") Long parentId, @Param("userId") Long userId);
    
    /**
     * 获取用户回收站文件列表
     * @param userId 用户ID
     * @return 文件列表
     */
    List<UserFile> selectRecycleBinList(@Param("userId") Long userId);
    
    /**
     * 检查文件名在同一目录下是否存在
     * @param fileName 文件名
     * @param parentId 父文件夹ID
     * @param userId 用户ID
     * @return 存在数量
     */
    int checkFileNameExists(@Param("fileName") String fileName, @Param("parentId") Long parentId, @Param("userId") Long userId);
    
    /**
     * 更新删除标志
     * @param userFileId 用户文件ID
     * @param delFlag 删除标志
     * @param userId 用户ID
     * @return 影响行数
     */
    int updateDelFlag(@Param("userFileId") Long userFileId, @Param("delFlag") Integer delFlag, @Param("userId") Long userId);
    
    /**
     * 清空回收站
     * @param userId 用户ID
     * @return 影响行数
     */
    int emptyRecycleBin(@Param("userId") Long userId);
    
    /**
     * 根据文件ID和用户ID查询用户文件
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 用户文件
     */
    UserFile selectByFileIdAndUserId(@Param("fileId") Long fileId, @Param("userId") Long userId);
    
    /**
     * 获取文件夹中的所有子文件(包括子文件夹)
     * @param folderPath 文件夹路径模式，例如 "/folder1/%"
     * @param userId 用户ID
     * @return 子文件列表
     */
    List<UserFile> selectSubFiles(@Param("folderId") Long folderId, @Param("userId") Long userId);
    
    /**
     * 获取文件夹中的所有已删除子文件(包括回收站中的子文件夹)
     * @param folderId 文件夹ID
     * @param userId 用户ID
     * @return 已删除子文件列表
     */
    List<UserFile> selectDeletedSubFiles(@Param("folderId") Long folderId, @Param("userId") Long userId);
    
    /**
     * 更新收藏状态
     * @param userFileId 用户文件ID
     * @param isStarred 是否收藏
     * @param userId 用户ID
     * @return 影响行数
     */
    int updateStarStatus(@Param("userFileId") Long userFileId, @Param("isStarred") Boolean isStarred, @Param("userId") Long userId);
    
    /**
     * 获取用户收藏的文件列表
     * @param userId 用户ID
     * @return 收藏文件列表
     */
    List<UserFile> selectStarredFiles(@Param("userId") Long userId);
    
    /**
     * 增加下载次数
     * @param userFileId 用户文件ID
     * @return 影响行数
     */
    int increaseDownloadCount(@Param("userFileId") Long userFileId);
    
    /**
     * 根据文件名、父ID和用户ID查找用户文件记录
     * @param fileName 文件名
     * @param parentId 父文件夹ID
     * @param userId 用户ID
     * @return 用户文件
     */
    UserFile findUserFileByNameAndParent(@Param("fileName") String fileName, @Param("parentId") Long parentId, @Param("userId") Long userId);
} 