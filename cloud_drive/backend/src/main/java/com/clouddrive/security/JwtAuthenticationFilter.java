package com.clouddrive.security;

import com.clouddrive.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Value("${security.jwt.header}")
    private String tokenHeader;
    
    @Value("${security.jwt.prefix}")
    private String tokenPrefix;
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // 不需要验证token的路径
    private final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/auth/**", 
        "/share/info/{shareCode}",
        "/share/info/**", 
        "/share/verify",
        "/share/verify/**", 
        "/file/download/public/**",
        "/error/**",
        "/actuator/**",
        "/favicon.ico"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) 
            throws ServletException, IOException {
        try {
            // 检查当前请求路径是否需要跳过token验证
            String requestPath = request.getServletPath();
            logger.debug("处理请求: " + requestPath);
            
            if (shouldSkipTokenValidation(requestPath)) {
                logger.debug("跳过token验证: " + requestPath);
                chain.doFilter(request, response);
                return;
            }
            
            // 从请求头中获取JWT
            String jwt = getJwtFromRequest(request);
            logger.debug("从请求中获取到的JWT: " + jwt);
            
            // 如果请求头中有JWT，并且JWT有效，则设置认证信息
            if (StringUtils.hasText(jwt)) {
                try {
                    // 验证token格式是否正确
                    if (!isValidJwtFormat(jwt)) {
                        logger.error("无效的JWT格式: " + jwt);
                    } else {
                        logger.debug("JWT格式有效，开始解析token");
                        String username = jwtUtil.getUsernameFromToken(jwt);
                        Long userId = jwtUtil.getUserIdFromToken(jwt);
                        logger.debug("从JWT中提取的用户信息: username=" + username + ", userId=" + userId);
                        
                        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                            // 创建认证对象
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                            
                            // 设置认证详情
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            
                            // 设置认证信息
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            logger.debug("已设置认证信息到SecurityContext");
                            
                            // 在请求属性中设置用户ID
                            request.setAttribute("userId", userId);
                            logger.debug("已设置userId=" + userId + " 到请求属性");
                        } else {
                            boolean hasAuth = SecurityContextHolder.getContext().getAuthentication() != null;
                            logger.debug("未设置认证信息: username=" + username + ", 已有认证=" + hasAuth);
                        }
                    }
                } catch (MalformedJwtException e) {
                    logger.warn("无效的JWT token", e);
                } catch (ExpiredJwtException e) {
                    logger.warn("JWT token已过期", e);
                } catch (UnsupportedJwtException e) {
                    logger.warn("不支持的JWT token", e);
                } catch (SecurityException e) {
                    logger.warn("JWT签名验证失败", e);
                } catch (Exception e) {
                    logger.error("无法设置用户认证", e);
                }
            } else {
                logger.debug("请求中没有JWT token");
            }
        } catch (Exception ex) {
            logger.error("JWT处理过程中发生错误", ex);
        }
        
        chain.doFilter(request, response);
    }
    
    /**
     * 检查当前请求路径是否需要跳过token验证
     */
    private boolean shouldSkipTokenValidation(String requestPath) {
        return EXCLUDED_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }
    
    /**
     * 从请求头中获取JWT
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(tokenHeader);
        logger.debug("从请求头获取到的Authorization: " + bearerToken);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(tokenPrefix + " ")) {
            return bearerToken.substring(tokenPrefix.length() + 1);
        }
        return null;
    }
    
    /**
     * 检查JWT格式是否有效
     * 有效的JWT应该包含两个点，分为三部分
     */
    private boolean isValidJwtFormat(String jwt) {
        if (jwt == null) return false;
        
        // 简单检查JWT格式 - 应该包含两个点，分为三部分
        return jwt.split("\\.").length == 3;
    }
}