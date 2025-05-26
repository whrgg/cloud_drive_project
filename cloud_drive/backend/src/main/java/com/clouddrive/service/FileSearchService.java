package com.clouddrive.service;

import com.clouddrive.model.entity.UserFile;

import java.util.List;

/**
 * 文件搜索服务接口
 */
public interface FileSearchService {

    /**
     * 索引单个文件
     * @param userFile 用户文件
     * @return 是否成功
     */
    boolean indexFile(UserFile userFile);

    /**
     * 批量索引文件
     * @param userFiles 用户文件列表
     * @return 是否成功
     */
    boolean bulkIndexFiles(List<UserFile> userFiles);

    /**
     * 更新文件索引
     * @param userFile 用户文件
     * @return 是否成功
     */
    boolean updateFileIndex(UserFile userFile);

    /**
     * 删除文件索引
     * @param fileId 文件ID
     * @return 是否成功
     */
    boolean deleteFileIndex(Long fileId);

    /**
     * 搜索文件
     * @param keyword 关键词
     * @param userId 用户ID
     * @return 文件列表
     */
    List<UserFile> searchFiles(String keyword, Long userId);

    /**
     * 搜索文件（带分页）
     * @param keyword 关键词
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 文件列表
     */
    List<UserFile> searchFiles(String keyword, Long userId, int page, int size);
} 