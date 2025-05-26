package com.clouddrive.service;

import com.clouddrive.model.dto.ShareDTO;
import com.clouddrive.model.entity.Share;
import com.clouddrive.model.vo.ShareVO;

import java.util.List;

/**
 * 分享服务接口
 */
public interface ShareService {
    
    /**
     * 创建分享
     * @param shareDTO 分享信息
     * @param userId 用户ID
     * @return 分享信息
     */
    ShareVO createShare(ShareDTO shareDTO, Long userId);
    
    /**
     * 获取分享详情
     * @param shareCode 分享码
     * @return 分享详情
     */
    ShareVO getShareInfo(String shareCode);
    
    /**
     * 根据分享码获取分享实体
     * @param shareCode 分享码
     * @return 分享实体
     */
    Share getShareByCode(String shareCode);
    
    /**
     * 验证分享提取码
     * @param shareCode 分享码
     * @param extractionCode 提取码
     * @return 是否验证成功
     */
    boolean verifyExtractionCode(String shareCode, String extractionCode);
    
    /**
     * 获取用户分享列表
     * @param userId 用户ID
     * @return 分享列表
     */
    List<ShareVO> getUserShareList(Long userId);
    
    /**
     * 取消分享
     * @param shareId 分享ID
     * @param userId 用户ID
     * @return 是否取消成功
     */
    boolean cancelShare(Long shareId, Long userId);
    
    /**
     * 更新分享浏览次数
     * @param shareId 分享ID
     * @return 是否更新成功
     */
    boolean incrementViewCount(Long shareId);
    
    /**
     * 更新分享下载次数
     * @param shareId 分享ID
     * @return 是否更新成功
     */
    boolean incrementDownloadCount(Long shareId);
    
    /**
     * 根据ID获取分享
     * @param shareId 分享ID
     * @return 分享实体
     */
    Share getById(Long shareId);
    
    /**
     * 检查文件是否在分享范围内
     * @param fileId 要检查的文件ID
     * @param shareRootFileId 分享的根文件ID
     * @return 是否在分享范围内
     */
    boolean checkFileInShareScope(Long fileId, Long shareRootFileId);
    
    /**
     * 保存分享文件到用户网盘
     * @param sourceFileId 源文件ID
     * @param targetFolderId 目标文件夹ID
     * @param targetUserId 目标用户ID
     * @param sourceUserId 源用户ID
     * @return 是否保存成功
     */
    boolean saveShareFile(Long sourceFileId, Long targetFolderId, Long targetUserId, Long sourceUserId);
    
    /**
     * 更新分享保存次数
     * @param shareId 分享ID
     * @return 是否更新成功
     */
    boolean incrementSaveCount(Long shareId);
} 