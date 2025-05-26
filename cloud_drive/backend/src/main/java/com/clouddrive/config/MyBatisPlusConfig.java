package com.clouddrive.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * MyBatis-Plus配置类
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 配置MyBatis-Plus插件
     */
    @Bean
    @Order(0) // Ensure this bean is initialized early
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
    
    /**
     * 自定义MyBatis配置
     */
    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            // 确保MyBatis类型注册正常工作
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            configuration.setMapUnderscoreToCamelCase(true);
        };
    }
} 