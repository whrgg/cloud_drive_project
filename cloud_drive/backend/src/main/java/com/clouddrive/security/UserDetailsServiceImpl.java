package com.clouddrive.security;

import com.clouddrive.model.entity.User;
import com.clouddrive.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * UserDetailsService实现类
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserService userService;
    
    @Autowired
    public UserDetailsServiceImpl(UserService userService) {
        this.userService = userService;
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库中查询用户
        User user = userService.getUserByUsername(username);
        
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        
        // 创建并返回UserDetails对象
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
} 