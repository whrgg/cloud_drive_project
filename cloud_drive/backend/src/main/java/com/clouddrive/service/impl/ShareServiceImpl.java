package com.clouddrive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clouddrive.mapper.FileMapper;
import com.clouddrive.mapper.ShareMapper;
import com.clouddrive.mapper.UserFileMapper;
import com.clouddrive.mapper.UserMapper;
import com.clouddrive.model.dto.ShareDTO;
import com.clouddrive.model.entity.File;
import com.clouddrive.model.entity.Share;
import com.clouddrive.model.entity.User;
import com.clouddrive.model.entity.UserFile;
import com.clouddrive.model.vo.ShareVO;
import com.clouddrive.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 分享服务实现类
 */
@Service
@RequiredArgsConstructor
public class ShareServiceImpl extends ServiceImpl<ShareMapper, Share> implements ShareService {

    private final ShareMapper shareMapper;
    private final UserFileMapper userFileMapper;
    private final FileMapper fileMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public ShareVO createShare(ShareDTO shareDTO, Long userId) {
        // 查询用户文件信息
        UserFile userFile = userFileMapper.selectById(shareDTO.getUserFileId());
        if (userFile == null || !userFile.getUserId().equals(userId)) {
            throw new IllegalArgumentException("文件不存在或无权限分享");
        }

        // 查询文件信息
        File file = fileMapper.selectById(userFile.getFileId());
        if (file == null) {
            throw new IllegalArgumentException("文件不存在");
        }

        // 创建分享记录
        Share share = new Share();
        share.setUserId(userId);
        // 设置用户文件ID（user_file表中的id）
        share.setUserFileId(userFile.getId());
        
        // 生成分享码
        share.setShareCode(generateShareCode());
        
        // 设置提取码
        if (shareDTO.getHasExtraction()) {
            if (shareDTO.getExtractionCode() != null && !shareDTO.getExtractionCode().isEmpty()) {
                share.setExtractionCode(shareDTO.getExtractionCode());
            } else {
                share.setExtractionCode(generateExtractionCode());
            }
        }
        
        // 设置有效期（内存中设置type和validDays，不保存到数据库）
        share.setType(0); // 默认永久有效
        if (shareDTO.getExpireType() != null && shareDTO.getExpireType() > 0) {
            share.setType(1); // 有期限
            share.setValidDays(shareDTO.getExpireType());
            // 数据库中保存过期时间
            LocalDateTime expireTime = LocalDateTime.now().plusDays(shareDTO.getExpireType());
            share.setExpireTime(expireTime);
        } else {
            // 永久有效，过期时间为null
            share.setExpireTime(null);
        }
        
        // 初始化浏览和下载次数
        share.setViewCount(0);
        share.setDownloadCount(0);
        share.setStatus(1); // 1表示有效
        share.setCreateTime(LocalDateTime.now());
        share.setUpdateTime(LocalDateTime.now());
        
        // 保存分享记录
        shareMapper.insert(share);
        
        // 返回分享VO
        return convertToShareVO(share, userFile, file, null);
    }

    @Override
    public ShareVO getShareInfo(String shareCode) {
        Share share = shareMapper.selectByShareCode(shareCode);
        if (share == null) {
            return null;
        }
        
        // 设置分享类型
        if (share.getExpireTime() != null) {
            share.setType(1); // 有期限
            if (share.getExpireTime().isBefore(LocalDateTime.now())) {
                // 已过期
                return null;
            }
        } else {
            share.setType(0); // 永久有效
        }
        
        // 查询相关信息
        UserFile userFile = userFileMapper.selectById(share.getUserFileId());
        if (userFile == null) {
            return null;
        }
        
        File file = fileMapper.selectById(userFile.getFileId());
        if (file == null) {
            return null;
        }
        
        User user = userMapper.selectById(share.getUserId());
        
        // 增加浏览次数
        incrementViewCount(share.getId());
        
        return convertToShareVO(share, userFile, file, user);
    }

    @Override
    public Share getShareByCode(String shareCode) {
        Share share = shareMapper.selectByShareCode(shareCode);
        if (share == null) {
            return null;
        }
        
        // 设置分享类型
        if (share.getExpireTime() != null) {
            share.setType(1); // 有期限
            if (share.getExpireTime().isBefore(LocalDateTime.now())) {
                // 已过期
                return null;
            }
        } else {
            share.setType(0); // 永久有效
        }
        
        return share;
    }

    @Override
    public boolean verifyExtractionCode(String shareCode, String extractionCode) {
        Share share = shareMapper.selectByShareCode(shareCode);
        if (share == null) {
            return false;
        }
        
        // 设置分享类型
        if (share.getExpireTime() != null) {
            share.setType(1); // 有期限
            if (share.getExpireTime().isBefore(LocalDateTime.now())) {
                // 已过期
                return false;
            }
        } else {
            share.setType(0); // 永久有效
        }
        
        // 如果没有设置提取码，直接返回true
        if (share.getExtractionCode() == null || share.getExtractionCode().isEmpty()) {
            return true;
        }
        
        return share.getExtractionCode().equals(extractionCode);
    }

    @Override
    public List<ShareVO> getUserShareList(Long userId) {
        LambdaQueryWrapper<Share> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Share::getUserId, userId)
                .eq(Share::getStatus, 1)
                .orderByDesc(Share::getCreateTime);
        
        List<Share> shares = shareMapper.selectList(queryWrapper);
        List<ShareVO> shareVOList = new ArrayList<>();
        
        for (Share share : shares) {
            // 设置分享类型
            if (share.getExpireTime() != null) {
                share.setType(1); // 有期限
                if (share.getExpireTime().isBefore(LocalDateTime.now())) {
                    // 跳过已过期的分享
                    continue;
                }
            } else {
                share.setType(0); // 永久有效
            }
            
            UserFile userFile = userFileMapper.selectById(share.getUserFileId());
            if (userFile == null) {
                continue;
            }
            
            File file = fileMapper.selectById(userFile.getFileId());
            if (file == null) {
                continue;
            }
            
            User user = userMapper.selectById(userId);
            shareVOList.add(convertToShareVO(share, userFile, file, user));
        }
        
        return shareVOList;
    }

    @Override
    @Transactional
    public boolean cancelShare(Long shareId, Long userId) {
        Share share = shareMapper.selectById(shareId);
        if (share == null || !share.getUserId().equals(userId)) {
            return false;
        }
        
        // 更新状态为无效
        share.setStatus(0);
        share.setUpdateTime(LocalDateTime.now());
        return shareMapper.updateById(share) > 0;
    }

    @Override
    public boolean incrementViewCount(Long shareId) {
        return shareMapper.increaseViewCount(shareId) > 0;
    }

    @Override
    public boolean incrementDownloadCount(Long shareId) {
        return shareMapper.increaseDownloadCount(shareId) > 0;
    }
    
    @Override
    public Share getById(Long shareId) {
        return shareMapper.selectById(shareId);
    }
    
    @Override
    public boolean checkFileInShareScope(Long fileId, Long shareRootFileId) {
        // 如果是分享的根文件本身，直接返回true
        if (fileId.equals(shareRootFileId)) {
            return true;
        }
        
        // 检查文件是否存在
        UserFile file = userFileMapper.selectById(fileId);
        if (file == null) {
            return false;
        }
        
        // 获取分享的根文件信息
        UserFile shareRootFile = userFileMapper.selectById(shareRootFileId);
        if (shareRootFile == null) {
            return false;
        }
        
        // 如果根文件不是目录，那么只有根文件本身在分享范围内
        if (!shareRootFile.getIsDir()) {
            return false;
        }
        
        // 检查文件是否在分享根目录下
        // 找到文件的所有父级目录
        return isInFolder(file, shareRootFileId);
    }
    
    /**
     * 检查文件是否在指定文件夹内（递归检查父级目录）
     * @param file 要检查的文件
     * @param folderId 目标文件夹ID
     * @return 是否在文件夹内
     */
    private boolean isInFolder(UserFile file, Long folderId) {
        // 如果文件就是目标文件夹，返回true
        if (file.getId().equals(folderId)) {
            return true;
        }
        
        // 如果文件没有父级目录，返回false
        if (file.getParentId() == null || file.getParentId() == 0) {
            return false;
        }
        
        // 如果文件的父级就是目标文件夹，返回true
        if (file.getParentId().equals(folderId)) {
            return true;
        }
        
        // 递归检查父级目录
        UserFile parentFile = userFileMapper.selectById(file.getParentId());
        if (parentFile == null) {
            return false;
        }
        
        return isInFolder(parentFile, folderId);
    }
    
    @Override
    @Transactional
    public boolean saveShareFile(Long sourceFileId, Long targetFolderId, Long targetUserId, Long sourceUserId) {
        try {
            // 获取源文件信息
            UserFile sourceFile = userFileMapper.selectById(sourceFileId);
            if (sourceFile == null) {
                return false;
            }
            
            // 检查目标文件夹是否存在
            if (targetFolderId > 0) {
                UserFile targetFolder = userFileMapper.selectById(targetFolderId);
                if (targetFolder == null || !targetFolder.getIsDir() || !targetFolder.getUserId().equals(targetUserId)) {
                    return false;
                }
            }
            
            // 如果是目录，需要递归复制
            if (sourceFile.getIsDir()) {
                return copyFolder(sourceFile, targetFolderId, targetUserId, sourceUserId);
            } else {
                return copyFile(sourceFile, targetFolderId, targetUserId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 复制文件
     * @param sourceFile 源文件
     * @param targetFolderId 目标文件夹ID
     * @param targetUserId 目标用户ID
     * @return 是否复制成功
     */
    private boolean copyFile(UserFile sourceFile, Long targetFolderId, Long targetUserId) {
        try {
            // 创建新的用户文件记录
            UserFile newUserFile = new UserFile();
            BeanUtils.copyProperties(sourceFile, newUserFile, "id", "userId", "parentId", "createTime", "updateTime");
            
            newUserFile.setId(null); // 自动生成新ID
            newUserFile.setUserId(targetUserId);
            newUserFile.setParentId(targetFolderId);
            newUserFile.setCreateTime(new Date());
            newUserFile.setUpdateTime(new Date());
            newUserFile.setDelFlag(0); // 非删除状态
            newUserFile.setIsDir(sourceFile.getIsDir());
            
            // 防止文件名冲突，检查目标文件夹中是否已有同名文件
            String newFileName = sourceFile.getFileName();
            int count = 0;
            while (true) {
                LambdaQueryWrapper<UserFile> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(UserFile::getUserId, targetUserId)
                        .eq(UserFile::getParentId, targetFolderId)
                        .eq(UserFile::getFileName, newFileName)
                        .eq(UserFile::getDelFlag, 0);
                
                Long existsCount = userFileMapper.selectCount(queryWrapper);
                if (existsCount == null || existsCount == 0) {
                    break;
                }
                
                count++;
                // 文件名添加副本标识
                int dotIndex = sourceFile.getFileName().lastIndexOf(".");
                if (dotIndex > 0) {
                    String name = sourceFile.getFileName().substring(0, dotIndex);
                    String ext = sourceFile.getFileName().substring(dotIndex);
                    newFileName = name + " 副本(" + count + ")" + ext;
                } else {
                    newFileName = sourceFile.getFileName() + " 副本(" + count + ")";
                }
            }
            
            newUserFile.setFileName(newFileName);
            
            // 保存新的用户文件记录
            userFileMapper.insert(newUserFile);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 复制文件夹
     * @param sourceFolder 源文件夹
     * @param targetFolderId 目标文件夹ID
     * @param targetUserId 目标用户ID
     * @param sourceUserId 源用户ID
     * @return 是否复制成功
     */
    private boolean copyFolder(UserFile sourceFolder, Long targetFolderId, Long targetUserId, Long sourceUserId) {
        try {
            // 创建新的文件夹记录
            UserFile newFolder = new UserFile();
            BeanUtils.copyProperties(sourceFolder, newFolder, "id", "userId", "parentId", "createTime", "updateTime");
            
            newFolder.setId(null); // 自动生成新ID
            newFolder.setUserId(targetUserId);
            newFolder.setParentId(targetFolderId);
            newFolder.setCreateTime(new Date());
            newFolder.setUpdateTime(new Date());
            newFolder.setDelFlag(0); // 非删除状态
            newFolder.setIsDir(true); // 确保是目录
            
            // 防止文件夹名冲突，检查目标文件夹中是否已有同名文件夹
            String newFolderName = sourceFolder.getFileName();
            int count = 0;
            while (true) {
                LambdaQueryWrapper<UserFile> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(UserFile::getUserId, targetUserId)
                        .eq(UserFile::getParentId, targetFolderId)
                        .eq(UserFile::getFileName, newFolderName)
                        .eq(UserFile::getIsDir, true)
                        .eq(UserFile::getDelFlag, 0);
                
                Long existsCount = userFileMapper.selectCount(queryWrapper);
                if (existsCount == null || existsCount == 0) {
                    break;
                }
                
                count++;
                newFolderName = sourceFolder.getFileName() + " 副本(" + count + ")";
            }
            
            newFolder.setFileName(newFolderName);
            
            // 保存新的文件夹记录
            userFileMapper.insert(newFolder);
            
            // 获取新文件夹的ID
            Long newFolderId = newFolder.getId();
            
            // 递归复制文件夹中的内容
            LambdaQueryWrapper<UserFile> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserFile::getParentId, sourceFolder.getId())
                    .eq(UserFile::getUserId, sourceUserId)
                    .eq(UserFile::getDelFlag, 0);
            
            List<UserFile> childFiles = userFileMapper.selectList(queryWrapper);
            for (UserFile childFile : childFiles) {
                if (childFile.getIsDir()) {
                    copyFolder(childFile, newFolderId, targetUserId, sourceUserId);
                } else {
                    copyFile(childFile, newFolderId, targetUserId);
                }
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean incrementSaveCount(Long shareId) {
        // 更新分享保存次数 - 因为Share实体类没有saveCount字段，需要修改其他方式来记录
        Share share = shareMapper.selectById(shareId);
        if (share == null) {
            return false;
        }
        
        // 更新时间
        share.setUpdateTime(LocalDateTime.now());
        
        return shareMapper.updateById(share) > 0;
    }
    
    /**
     * 生成分享码
     */
    private String generateShareCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
    
    /**
     * 生成提取码
     */
    private String generateExtractionCode() {
        return String.valueOf((int)((Math.random() * 9 + 1) * 1000));
    }
    
    /**
     * 转换为ShareVO
     */
    private ShareVO convertToShareVO(Share share, UserFile userFile, File file, User user) {
        ShareVO shareVO = new ShareVO();
        
        shareVO.setId(share.getId());
        shareVO.setUserId(share.getUserId());
        shareVO.setUsername(user != null ? user.getUsername() : null);
        // 设置用户文件ID（user_file表中的id）
        shareVO.setUserFileId(share.getUserFileId());
        // 设置文件ID（file表中的id）
        shareVO.setFileId(userFile.getFileId());
        shareVO.setFileName(userFile.getFileName());
        shareVO.setFileType(file.getFileType());
        shareVO.setFileSize(file.getFileSize());
        shareVO.setIsDir(userFile.getIsDir());
        shareVO.setShareCode(share.getShareCode());
        shareVO.setNeedExtraction(share.getExtractionCode() != null && !share.getExtractionCode().isEmpty());
        shareVO.setExtractionCode(share.getExtractionCode());
        
        // 处理LocalDateTime转Date
        if (share.getExpireTime() != null) {
            shareVO.setExpireTime(java.util.Date.from(share.getExpireTime().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        } else {
            shareVO.setExpireTime(null);
        }
        
        shareVO.setViewCount(share.getViewCount());
        shareVO.setDownloadCount(share.getDownloadCount());
        
        // 处理状态
        boolean isValid = share.getStatus() == 1 && (share.getType() == 0 || 
                (share.getExpireTime() != null && share.getExpireTime().isAfter(LocalDateTime.now())));
        shareVO.setStatus(isValid ? 1 : 0);
        
        // 处理创建时间
        if (share.getCreateTime() != null) {
            shareVO.setCreateTime(java.util.Date.from(share.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }
        
        return shareVO;
    }
} 