package com.clouddrive.service;

import com.clouddrive.model.dto.LoginDTO;
import com.clouddrive.model.dto.RegisterDTO;
import com.clouddrive.model.dto.UserInfoDTO;
import com.clouddrive.model.entity.User;
import com.clouddrive.model.vo.LoginResponseVO;
import com.clouddrive.model.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户注册
     * @param registerDTO 注册信息
     * @return 注册结果
     */
    UserVO register(RegisterDTO registerDTO);
    
    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    LoginResponseVO login(LoginDTO loginDTO);
    
    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    UserVO getUserInfo(Long userId);
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);
    
    /**
     * 更新用户已使用空间
     * @param userId 用户ID
     * @param size 变化的空间大小(正值表示增加，负值表示减少)
     * @return 更新结果
     */
    boolean updateUsedSize(Long userId, Long size);
    
    /**
     * 更新用户头像
     * @param userId 用户ID
     * @param avatarUrl 头像URL
     * @return 更新结果
     */
    boolean updateAvatar(Long userId, String avatarUrl);
    
    /**
     * 修改用户密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 修改结果
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 更新用户基本信息
     * @param userId 用户ID
     * @param userInfoDTO 用户信息
     * @return 更新结果
     */
    boolean updateUserInfo(Long userId, UserInfoDTO userInfoDTO);
    
    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户信息
     */
    User getUserByEmail(String email);
    
    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户信息
     */
    User getUserByPhone(String phone);
} 