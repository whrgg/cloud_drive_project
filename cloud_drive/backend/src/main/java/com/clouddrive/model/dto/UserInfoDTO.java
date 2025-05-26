package com.clouddrive.model.dto;

import lombok.Data;

/**
 * 用户信息数据传输对象
 */
@Data
public class UserInfoDTO {
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
} 