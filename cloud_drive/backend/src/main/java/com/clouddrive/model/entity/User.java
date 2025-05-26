package com.clouddrive.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 用户头像
     */
    private String avatar;
    
    /**
     * 存储空间大小（字节）
     */
    private Long storageSize;
    
    /**
     * 已使用空间大小（字节）
     */
    private Long usedSize;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
} 