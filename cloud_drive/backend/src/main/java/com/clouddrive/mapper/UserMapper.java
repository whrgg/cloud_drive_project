package com.clouddrive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clouddrive.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户信息
     */
    @Select("SELECT * FROM user WHERE email = #{email}")
    User findByEmail(@Param("email") String email);
    
    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户信息
     */
    @Select("SELECT * FROM user WHERE phone = #{phone}")
    User findByPhone(@Param("phone") String phone);
    
    /**
     * 更新用户已使用空间
     * @param userId 用户ID
     * @param size 变化的空间大小(正值表示增加，负值表示减少)
     * @return 影响行数
     */
    int updateUsedSize(@Param("userId") Long userId, @Param("size") Long size);
} 