package com.clouddrive.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseVO {
    
    /**
     * 用户信息
     */
    private UserVO userInfo;
    
    /**
     * 访问令牌
     */
    private String token;
    
    /**
     * 令牌类型
     */
    private String tokenType = "Bearer";
} 