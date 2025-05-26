package com.clouddrive;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 云盘系统启动类
 */
@SpringBootApplication
@MapperScan("com.clouddrive.mapper")
public class CloudDriveApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudDriveApplication.class, args);
    }
} 