package com.clouddrive.controller;

import com.clouddrive.model.dto.LoginDTO;
import com.clouddrive.model.dto.RegisterDTO;
import com.clouddrive.model.vo.LoginResponseVO;
import com.clouddrive.model.vo.ResultVO;
import com.clouddrive.model.vo.UserVO;
import com.clouddrive.model.entity.User;
import com.clouddrive.service.UserService;
import com.clouddrive.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/auth")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 用户注册
     * @param registerDTO 注册信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResultVO<UserVO> register(@RequestBody @Valid RegisterDTO registerDTO) {
        UserVO userVO = userService.register(registerDTO);
        return ResultVO.success("注册成功", userVO);
    }
    
    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @param request HTTP请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResultVO<LoginResponseVO> login(@RequestBody @Valid LoginDTO loginDTO, HttpServletRequest request) {
        LoginResponseVO loginResponse = userService.login(loginDTO);
        logger.info("用户 {} 登录成功，IP: {}", loginDTO.getUsername(), getClientIp(request));
        return ResultVO.success("登录成功", loginResponse);
    }
    
    /**
     * 用户登出
     * @param request HTTP请求
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ResultVO<Boolean> logout(HttpServletRequest request) {
        try {
            // 从请求头中获取JWT
            String bearerToken = request.getHeader("Authorization");
            
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                String token = bearerToken.substring(7);
                
                // 获取用户信息
                String username = jwtUtil.getUsernameFromToken(token);
                Long userId = jwtUtil.getUserIdFromToken(token);
                
                if (username != null && userId != null) {
                    // 记录用户登出日志
                    logger.info("用户 {} (ID: {}) 登出成功，IP: {}, 时间: {}", 
                        username, userId, getClientIp(request), new Date());
                    
                    // 将token加入黑名单（如果有实现Redis等缓存，可以在这里将token加入黑名单）
                    // tokenBlacklistService.addToBlacklist(token, jwtUtil.getExpirationDateFromToken(token));
                    
                    // 如果系统有用户在线状态功能，可以在这里更新用户状态
                    // userService.updateOnlineStatus(userId, false);
                    
                    return ResultVO.success("登出成功", true);
                }
            }
            
            // 如果没有token或token无效，也返回成功（因为前端已经清除了token）
            return ResultVO.success("登出成功", true);
            
        } catch (Exception e) {
            // 记录异常，但依然返回成功（不影响用户体验）
            logger.error("用户登出过程中发生异常: " + e.getMessage(), e);
            return ResultVO.success("登出成功", true);
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 发送验证码
     * @param requestBody 请求体，包含type和target
     * @return 发送结果
     */
    @PostMapping("/send-code")
    public ResultVO<Boolean> sendVerificationCode(@RequestBody Map<String, String> requestBody) {
        try {
            String type = requestBody.get("type");
            String target = requestBody.get("target");
            
            // 验证参数
            if (!StringUtils.hasText(type) || !StringUtils.hasText(target)) {
                return ResultVO.fail("参数不完整");
            }
            
            if (!"email".equals(type) && !"phone".equals(type)) {
                return ResultVO.fail("不支持的验证码类型");
            }
            
            // 根据类型验证格式
            if ("email".equals(type)) {
                // 简单的邮箱格式验证
                if (!target.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                    return ResultVO.fail("邮箱格式不正确");
                }
            } else if ("phone".equals(type)) {
                // 简单的手机号格式验证
                if (!target.matches("^1[3-9]\\d{9}$")) {
                    return ResultVO.fail("手机号格式不正确");
                }
            }
            
            // 检查用户是否已存在
            if ("email".equals(type)) {
                User existingUser = userService.getUserByEmail(target);
                if (existingUser != null) {
                    return ResultVO.fail("该邮箱已被注册");
                }
            } else if ("phone".equals(type)) {
                User existingUser = userService.getUserByPhone(target);
                if (existingUser != null) {
                    return ResultVO.fail("该手机号已被注册");
                }
            }
            
            // 生成验证码
            String code = generateVerificationCode();
            
            // 保存验证码到缓存或数据库
            // 在实际应用中，应该将验证码保存到Redis等缓存中，并设置过期时间
            // cacheService.setWithExpire("verification_code:" + type + ":" + target, code, 300);
            
            // 发送验证码
            if ("email".equals(type)) {
                // 调用邮件服务发送验证码
                // emailService.sendVerificationCode(target, code);
                logger.info("向邮箱 {} 发送验证码: {}", target, code);
            } else if ("phone".equals(type)) {
                // 调用短信服务发送验证码
                // smsService.sendVerificationCode(target, code);
                logger.info("向手机号 {} 发送验证码: {}", target, code);
            }
            
            return ResultVO.success("验证码已发送", true);
        } catch (Exception e) {
            logger.error("发送验证码失败: " + e.getMessage(), e);
            return ResultVO.fail("发送验证码失败: " + e.getMessage());
        }
    }

    /**
     * 生成6位数字验证码
     * @return 验证码
     */
    private String generateVerificationCode() {
        // 生成6位随机数字
        StringBuilder code = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
} 