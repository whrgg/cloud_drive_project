package com.clouddrive.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 分享VO
 */
@Data
public class ShareVO {
    
    /**
     * 分享ID
     */
    private Long id;
    
    /**
     * 分享用户ID
     */
    private Long userId;
    
    /**
     * 分享用户名
     */
    private String username;
    
    /**
     * 分享文件ID
     */
    private Long userFileId;
    
    /**
     * 文件ID
     */
    private Long fileId;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * 是否为目录
     */
    private Boolean isDir;
    
    /**
     * 分享码
     */
    private String shareCode;
    
    /**
     * 是否需要提取码
     */
    private Boolean needExtraction;
    
    /**
     * 提取码
     */
    private String extractionCode;
    
    /**
     * 过期时间
     */
    private Date expireTime;
    
    /**
     * 浏览次数
     */
    private Integer viewCount;
    
    /**
     * 下载次数
     */
    private Integer downloadCount;
    
    /**
     * 状态，1:有效，0:无效
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private Date createTime;
} 