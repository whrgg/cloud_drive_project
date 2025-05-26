package com.clouddrive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clouddrive.model.entity.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文件Mapper接口
 */
@Mapper
public interface FileMapper extends BaseMapper<File> {
    
    /**
     * 根据MD5查询文件
     * @param md5 文件MD5值
     * @return 文件
     */
    File selectByMd5(@Param("md5") String md5);
    
    /**
     * 增加文件使用次数
     * @param fileId 文件ID
     * @return 影响行数
     */
    int increaseUsageCount(@Param("fileId") Long fileId);
    
    /**
     * 减少文件使用次数
     * @param fileId 文件ID
     * @return 影响行数
     */
    int decreaseUsageCount(@Param("fileId") Long fileId);
} 