package com.clouddrive.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 文件实体类
 */
@Data
@TableName("file")
public class File {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 对象存储名称
     */
    @TableField("object_name")
    private String objectName;
    
    /**
     * 文件名
     */
    @TableField("file_name")
    private String fileName;
    
    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;
    
    /**
     * 文件类型
     */
    @TableField("file_type")
    private String fileType;
    
    /**
     * 文件MD5
     */
    @TableField("md5")
    private String md5;
    
    /**
     * 使用次数
     */
    @TableField("usage_count")
    private Integer usageCount;
    
    /**
     * 状态，1:可用，0:不可用
     */
    @TableField("status")
    private Integer status;
    
    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;
    
    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;
    
    /**
     * 存储路径（兼容旧代码）
     */
    public String getStoragePath() {
        return objectName;
    }
    
    /**
     * 设置存储路径（兼容旧代码）
     */
    public void setStoragePath(String storagePath) {
        this.objectName = storagePath;
    }
    
    /**
     * 获取文件MD5（兼容旧代码）
     */
    public String getFileMd5() {
        return md5;
    }
    
    /**
     * 设置文件MD5（兼容旧代码）
     */
    public void setFileMd5(String fileMd5) {
        this.md5 = fileMd5;
    }
} 