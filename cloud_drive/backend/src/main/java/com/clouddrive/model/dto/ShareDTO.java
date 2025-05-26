package com.clouddrive.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/**
 * 分享DTO
 */
@Data
public class ShareDTO {
    
    /**
     * 用户文件ID
     */
    @NotNull(message = "用户文件ID不能为空")
    private Long userFileId;
    
    /**
     * 过期时间类型：0-永久有效，1-1天，7-7天，30-30天
     */
    private Integer expireType;
    
    /**
     * 是否设置提取码：true-是，false-否
     */
    private Boolean hasExtraction;
    
    /**
     * 自定义提取码，为空则自动生成
     */
    private String extractionCode;
    
    /**
     * 分享说明
     */
    private String description;
} 