package com.clouddrive.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    @Value("${security.jwt.expiration}")
    private long expiration;
    
    @Value("${security.jwt.secret:defaultSecretKeyForDevelopmentEnvironmentOnly}")
    private String secret;
    
    private Key key;
    
    @PostConstruct
    public void init() {
        // 使用配置的固定密钥
        try {
            // 使用SHA-512哈希算法处理密钥，确保长度足够
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] secretBytes = md.digest(secret.getBytes(StandardCharsets.UTF_8));
            // 使用处理后的字节创建密钥
            this.key = Keys.hmacShaKeyFor(secretBytes);
            logger.info("已初始化JWT密钥");
        } catch (NoSuchAlgorithmException e) {
            logger.error("初始化JWT密钥失败", e);
            // 如果哈希算法不可用，使用默认方式创建密钥
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }
    }

    /**
     * 从token中提取用户名
     */
    public String getUsernameFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从token中提取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 提取token中的声明
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * 获取token中的所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 检查token是否过期
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 生成token
     */
    public String generateToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return doGenerateToken(claims, username);
    }

    /**
     * 生成token的具体实现
     */
    private String doGenerateToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 验证token
     */
    public Boolean validateToken(String token, String username) {
        final String tokenUsername = getUsernameFromToken(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }
    
    /**
     * 从token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return Long.valueOf(claims.get("userId").toString());
    }
    
    /**
     * 从请求头中提取用户ID
     * @param request HTTP请求
     * @return 用户ID
     */
    public Long getUserIdFromRequest(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return null;
        }
        return getUserIdFromToken(token);
    }
    
    /**
     * 从请求头中提取Token
     * @param request HTTP请求
     * @return Token字符串
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
} 