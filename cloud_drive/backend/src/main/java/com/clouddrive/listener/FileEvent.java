package com.clouddrive.listener;

import com.clouddrive.model.entity.UserFile;
import lombok.Data;

import java.util.List;

/**
 * 文件事件
 */
@Data
public class FileEvent {
    
    /**
     * 事件类型
     */
    private String eventType;
    
    /**
     * 单个用户文件
     */
    private UserFile userFile;
    
    /**
     * 多个用户文件（用于批量操作）
     */
    private List<UserFile> userFiles;
    
    /**
     * 文件ID（用于删除操作）
     */
    private Long fileId;
    
    /**
     * 创建上传事件
     */
    public static FileEvent createUploadEvent(UserFile userFile) {
        FileEvent event = new FileEvent();
        event.setEventType("UPLOAD");
        event.setUserFile(userFile);
        return event;
    }
    
    /**
     * 创建更新事件
     */
    public static FileEvent createUpdateEvent(UserFile userFile) {
        FileEvent event = new FileEvent();
        event.setEventType("UPDATE");
        event.setUserFile(userFile);
        return event;
    }
    
    /**
     * 创建删除事件
     */
    public static FileEvent createDeleteEvent(Long fileId) {
        FileEvent event = new FileEvent();
        event.setEventType("DELETE");
        event.setFileId(fileId);
        return event;
    }
    
    /**
     * 创建批量索引事件
     */
    public static FileEvent createBulkIndexEvent(List<UserFile> userFiles) {
        FileEvent event = new FileEvent();
        event.setEventType("BULK_INDEX");
        event.setUserFiles(userFiles);
        return event;
    }
} 