package com.clouddrive.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clouddrive.exception.BusinessException;
import com.clouddrive.mapper.UserMapper;
import com.clouddrive.model.dto.LoginDTO;
import com.clouddrive.model.dto.RegisterDTO;
import com.clouddrive.model.dto.UserInfoDTO;
import com.clouddrive.model.entity.User;
import com.clouddrive.model.vo.LoginResponseVO;
import com.clouddrive.model.vo.UserVO;
import com.clouddrive.service.UserService;
import com.clouddrive.util.JwtUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtUtil jwtUtil;
    
    @Value("${user.default.storage-size:10737418240}")
    private Long defaultStorageSize; // 默认10GB存储空间
    
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    
    @Autowired
    public UserServiceImpl(UserMapper userMapper, 
                          PasswordEncoder passwordEncoder,
                          AuthenticationManagerBuilder authenticationManagerBuilder,
                          JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.jwtUtil = jwtUtil;
    }
    
    @Override
    @Transactional
    public UserVO register(RegisterDTO registerDTO) {
        // 检查用户名是否已存在
        User existingUser = userMapper.findByUsername(registerDTO.getUsername());
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }
        
        // 检查邮箱是否已被使用
        User existingEmail = userMapper.findByEmail(registerDTO.getEmail());
        if (existingEmail != null) {
            throw new BusinessException("该邮箱已被注册");
        }
        
        // 如果提供了手机号，检查是否已被使用
        if (StringUtils.hasText(registerDTO.getPhone())) {
            User existingPhone = userMapper.findByPhone(registerDTO.getPhone());
            if (existingPhone != null) {
                throw new BusinessException("该手机号已被注册");
            }
        }
        
        // 验证验证码（如果需要）
        // 注释掉验证码验证逻辑，暂时不需要验证码
        /*
        String code = registerDTO.getCode();
        if (StringUtils.hasText(code)) {
            // TODO: 实现验证码校验逻辑
            // 可以调用验证码服务进行验证
            // verificationCodeService.verify(registerDTO.getEmail(), registerDTO.getPhone(), code);
        }
        */
        
        // 检查密码是否匹配
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        
        // 密码强度校验
        validatePasswordStrength(registerDTO.getPassword());
        
        // 创建新用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        user.setAvatar("default-avatar.png"); // 默认头像
        user.setStorageSize(defaultStorageSize);
        user.setUsedSize(0L);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        
        // 保存用户
        save(user);
        
        // 转换为VO对象
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        
        return userVO;
    }
    
    /**
     * 检查密码强度
     * @param password 密码
     */
    private void validatePasswordStrength(String password) {
        if (password.length() < 6) {
            throw new BusinessException("密码长度不能少于6个字符");
        }
        
        // 检查密码是否包含数字
        boolean hasDigit = false;
        // 检查密码是否包含字母
        boolean hasLetter = false;
        // 检查密码是否包含特殊字符
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isLetter(c)) {
                hasLetter = true;
            } else {
                hasSpecial = true;
            }
        }
        
        // 密码必须包含数字和字母
        if (!hasDigit || !hasLetter) {
            throw new BusinessException("密码必须包含数字和字母");
        }
        
        // 建议使用特殊字符（不强制）
        if (!hasSpecial) {
            // 可以记录日志或在返回消息中提示用户
        }
    }
    
    @Override
    public LoginResponseVO login(LoginDTO loginDTO) {
        try {
            // 创建认证令牌
            UsernamePasswordAuthenticationToken authenticationToken = 
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());
            
            // 使用AuthenticationManagerBuilder进行认证
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            
            // 认证成功，获取用户信息
            User user = getUserByUsername(loginDTO.getUsername());
            
            // 生成JWT令牌
            String token = jwtUtil.generateToken(user.getUsername(), user.getId());
            
            // 构建用户视图对象
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            
            // 构建登录响应
            return new LoginResponseVO(userVO, token, "Bearer");
        } catch (AuthenticationException e) {
            throw new BusinessException(401, "用户名或密码错误");
        }
    }
    
    @Override
    public UserVO getUserInfo(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        
        return userVO;
    }
    
    @Override
    public User getUserByUsername(String username) {
        return userMapper.findByUsername(username);
    }
    
    @Override
    public boolean updateUsedSize(Long userId, Long size) {
        // 实现更新用户已使用空间的逻辑
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 计算新的已使用空间
        Long newUsedSize = user.getUsedSize() + size;
        if (newUsedSize < 0) {
            log.warn("用户ID: {} 空间统计异常，被调整为0。原空间: {}, 调整量: {}", 
                    userId, user.getUsedSize(), size);
            newUsedSize = 0L; // 确保已使用空间不为负数
        }
        
        // 检查是否超出总空间
        if (newUsedSize > user.getStorageSize()) {
            throw new BusinessException("存储空间不足");
        }
        
        // 更新已使用空间
        user.setUsedSize(newUsedSize);
        user.setUpdateTime(new Date());
        
        log.info("用户ID: {} 空间使用更新: {} -> {} (调整量: {})", 
                userId, user.getUsedSize() - size, newUsedSize, size);
        
        return updateById(user);
    }
    
    @Override
    public boolean updateAvatar(Long userId, String avatarUrl) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 更新头像URL
        user.setAvatar(avatarUrl);
        user.setUpdateTime(new Date());
        
        return updateById(user);
    }
    
    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        
        // 检查新密码是否与旧密码相同
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(new Date());
        
        return updateById(user);
    }
    
    @Override
    public boolean updateUserInfo(Long userId, UserInfoDTO userInfoDTO) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 检查用户名是否已存在
        if (StringUtils.hasText(userInfoDTO.getUsername()) && !user.getUsername().equals(userInfoDTO.getUsername())) {
            User existingUser = userMapper.findByUsername(userInfoDTO.getUsername());
            if (existingUser != null) {
                throw new BusinessException("用户名已存在");
            }
            user.setUsername(userInfoDTO.getUsername());
        }
        
        // 更新邮箱
        if (StringUtils.hasText(userInfoDTO.getEmail())) {
            user.setEmail(userInfoDTO.getEmail());
        }
        
        user.setUpdateTime(new Date());
        
        return updateById(user);
    }
    
    @Override
    public User getUserByEmail(String email) {
        return userMapper.findByEmail(email);
    }
    
    @Override
    public User getUserByPhone(String phone) {
        return userMapper.findByPhone(phone);
    }
} 