package com.clouddrive.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户文件实体类
 */
@Data
@TableName("user_file")
public class UserFile {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 文件ID，如果是目录则为null
     */
    @TableField("file_id")
    private Long fileId;
    
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
     * 父文件夹ID
     */
    @TableField("parent_id")
    private Long parentId;
    
    /**
     * 是否为文件夹
     */
    @TableField("is_dir")
    private Boolean isDir;
    
    /**
     * 删除标志，0:未删除，1:回收站，2:已删除
     */
    @TableField("del_flag")
    private Integer delFlag;
    
    /**
     * 是否已收藏
     */
    @TableField(exist = false)
    private Boolean isStarred;
    
    /**
     * 删除时间
     */
    @TableField(exist = false)
    private Date deleteTime;
    
    /**
     * 下载次数
     */
    @TableField("download_count")
    private Integer downloadCount;
    
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
     * 获取是否已删除（兼容旧代码）
     */
    public Boolean getIsDeleted() {
        if (delFlag == null) {
            return false;
        }
        return delFlag > 0;
    }
    
    /**
     * 设置是否已删除（兼容旧代码）
     */
    public void setIsDeleted(Boolean isDeleted) {
        this.delFlag = isDeleted ? 1 : 0;
    }
    
    /**
     * 获取文件路径（兼容旧代码）
     */
    public String getFilePath() {
        return "/";
    }
    
    /**
     * 设置文件路径（兼容旧代码）
     */
    public void setFilePath(String filePath) {
        // 忽略，不再使用此字段
    }
} 