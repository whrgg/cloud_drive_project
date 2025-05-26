package com.clouddrive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clouddrive.model.entity.Share;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 分享Mapper接口
 */
@Mapper
public interface ShareMapper extends BaseMapper<Share> {
    
    /**
     * 根据分享码查询分享信息
     * @param shareCode 分享码
     * @return 分享信息
     */
    Share selectByShareCode(@Param("shareCode") String shareCode);
    
    /**
     * 增加浏览次数
     * @param shareId 分享ID
     * @return 影响行数
     */
    int increaseViewCount(@Param("shareId") Long shareId);
    
    /**
     * 增加下载次数
     * @param shareId 分享ID
     * @return 影响行数
     */
    int increaseDownloadCount(@Param("shareId") Long shareId);
} 