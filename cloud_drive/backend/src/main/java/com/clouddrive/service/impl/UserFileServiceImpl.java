package com.clouddrive.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clouddrive.mapper.UserFileMapper;
import com.clouddrive.model.entity.UserFile;
import com.clouddrive.service.UserFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户文件服务实现类
 */
@Service
public class UserFileServiceImpl extends ServiceImpl<UserFileMapper, UserFile> implements UserFileService {

    @Autowired
    private UserFileMapper userFileMapper;
    
    @Override
    public List<UserFile> getUserFileList(Long parentId, Long userId) {
        return userFileMapper.selectUserFileList(parentId, userId);
    }
    
    @Override
    public List<UserFile> getRecycleBinList(Long userId) {
        return userFileMapper.selectRecycleBinList(userId);
    }
    
    @Override
    public UserFile getById(Long userFileId) {
        return userFileMapper.selectById(userFileId);
    }
    
    @Override
    public boolean checkFileNameExists(String fileName, Long parentId, Long userId) {
        int count = userFileMapper.checkFileNameExists(fileName, parentId, userId);
        return count > 0;
    }
    
    @Override
    public boolean updateDelFlag(Long userFileId, Integer delFlag, Long userId) {
        int rows = userFileMapper.updateDelFlag(userFileId, delFlag, userId);
        return rows > 0;
    }
    
    @Override
    public boolean emptyRecycleBin(Long userId) {
        int rows = userFileMapper.emptyRecycleBin(userId);
        return rows > 0;
    }
    
    @Override
    public UserFile getByFileIdAndUserId(Long fileId, Long userId) {
        return userFileMapper.selectByFileIdAndUserId(fileId, userId);
    }
    
    @Override
    public List<UserFile> getSubFiles(Long folderId, Long userId) {
        return userFileMapper.selectSubFiles(folderId, userId);
    }
    
    @Override
    public List<UserFile> getDeletedSubFiles(Long folderId, Long userId) {
        return userFileMapper.selectDeletedSubFiles(folderId, userId);
    }
    
    @Override
    public boolean updateStarStatus(Long userFileId, Boolean isStarred, Long userId) {
        int rows = userFileMapper.updateStarStatus(userFileId, isStarred, userId);
        return rows > 0;
    }
    
    @Override
    public List<UserFile> getStarredFiles(Long userId) {
        return userFileMapper.selectStarredFiles(userId);
    }
    
    @Override
    public UserFile findUserFileByNameAndParent(String fileName, Long parentId, Long userId) {
        return userFileMapper.findUserFileByNameAndParent(fileName, parentId, userId);
    }
    
    @Override
    public boolean increaseDownloadCount(Long userFileId) {
        int rows = userFileMapper.increaseDownloadCount(userFileId);
        return rows > 0;
    }
} 