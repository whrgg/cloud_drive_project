package com.clouddrive.listener;

import com.clouddrive.model.entity.UserFile;
import com.clouddrive.service.FileSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件索引监听器
 * 监听文件变更事件，同步更新Elasticsearch索引
 */
@Component
@Slf4j
public class FileIndexListener {

    @Autowired
    private FileSearchService fileSearchService;

    /**
     * 处理文件上传事件
     * @param userFile 用户文件
     */
    @Async
    @EventListener(condition = "#event.eventType == 'UPLOAD'")
    public void handleFileUploadEvent(FileEvent event) {
        UserFile userFile = event.getUserFile();
        log.info("Received file upload event, indexing file: {}", userFile.getFileName());
        fileSearchService.indexFile(userFile);
    }

    /**
     * 处理文件更新事件
     * @param userFile 用户文件
     */
    @Async
    @EventListener(condition = "#event.eventType == 'UPDATE'")
    public void handleFileUpdateEvent(FileEvent event) {
        UserFile userFile = event.getUserFile();
        log.info("Received file update event, updating index for file: {}", userFile.getFileName());
        fileSearchService.updateFileIndex(userFile);
    }

    /**
     * 处理文件删除事件
     * @param fileId 文件ID
     */
    @Async
    @EventListener(condition = "#event.eventType == 'DELETE'")
    public void handleFileDeleteEvent(FileEvent event) {
        Long fileId = event.getFileId();
        log.info("Received file delete event, removing file from index: {}", fileId);
        fileSearchService.deleteFileIndex(fileId);
    }

    /**
     * 处理批量索引事件
     * @param userFiles 用户文件列表
     */
    @Async
    @EventListener(condition = "#event.eventType == 'BULK_INDEX'")
    public void handleBulkIndexEvent(FileEvent event) {
        List<UserFile> userFiles = event.getUserFiles();
        log.info("Received bulk index event, indexing {} files", userFiles.size());
        fileSearchService.bulkIndexFiles(userFiles);
    }
} 