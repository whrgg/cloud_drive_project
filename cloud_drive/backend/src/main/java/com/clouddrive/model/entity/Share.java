package com.clouddrive.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分享实体类
 */
@Data
@TableName("share")
public class Share {

    /**
     * 分享ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 用户文件ID
     */
    @TableField("user_file_id")
    private Long userFileId;

    /**
     * 分享码
     */
    @TableField("share_code")
    private String shareCode;

    /**
     * 提取码，为空表示无密码
     */
    @TableField("extraction_code")
    private String extractionCode;

    /**
     * 分享类型 0-永久有效 1-有效期（数据库中无此字段，根据expire_time是否为null判断）
     */
    @TableField(exist = false)
    private Integer type;

    /**
     * 有效天数，type=1时有效（数据库中无此字段）
     */
    @TableField(exist = false)
    private Integer validDays;

    /**
     * 过期时间
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /**
     * 浏览次数
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 下载次数
     */
    @TableField("download_count")
    private Integer downloadCount;

    /**
     * 状态，1:有效，0:无效
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
} 