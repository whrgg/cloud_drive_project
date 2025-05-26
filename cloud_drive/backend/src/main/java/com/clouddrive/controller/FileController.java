package com.clouddrive.controller;

import com.clouddrive.model.entity.File;
import com.clouddrive.model.entity.UserFile;
import com.clouddrive.model.vo.ResultVO;
import com.clouddrive.service.StorageService;
import com.clouddrive.service.FileService;
import com.clouddrive.service.FileSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 文件控制器
 */
@RestController
@RequestMapping("/file")
public class FileController {
    
    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    
    @Autowired
    private StorageService storageService;
    
    @Autowired
    private FileService fileService;
    
    @Autowired
    private FileSearchService fileSearchService;
    
    @Value("${minio.bucketName:cloud-drive}")
    private String bucketName;
    
    /**
     * 上传文件
     * @param file 文件
     * @param parentId 父文件夹ID
     * @param relativePath 相对路径
     * @param userId 用户ID
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ResultVO<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("parentId") Long parentId,
            @RequestParam(value = "relativePath", required = false) String relativePath,
            @RequestAttribute("userId") Long userId) {
        
        if (file.isEmpty()) {
            return ResultVO.fail("上传文件不能为空");
        }
        
        try {
            // 调用 FileService 的 upload 方法，传递 parentId 和 relativePath
            UserFile userFile = fileService.upload(file, parentId, relativePath, userId);
            
            // 如果需要返回文件URL，确保 UserFile 实体或其关联实体有该信息，或者 service 返回更丰富的对象
            // 暂时简单返回成功信息和文件名
            return ResultVO.success("上传成功: " + userFile.getFileName(), userFile.getFileName()); // 示例性返回
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载文件
     * @param objectName 对象名称或文件ID
     * @return 文件流
     */
    @GetMapping("/download/{objectName}")
    public ResponseEntity<?> downloadFile(@PathVariable("objectName") String objectName) throws Exception {
        // 检查objectName是否为数字（文件ID）
        InputStream inputStream = null;
        String fileName = null;
        String fileType = null;
        Long fileSize = null;
        String objectNameToDownload = null;
        Long userFileId = null;
        
        try {
            // 尝试将objectName解析为数字（文件ID）
            userFileId = Long.parseLong(objectName);
            
            // 如果是数字，则查询用户文件信息
            UserFile userFile = fileService.getUserFileById(userFileId, null);
            if (userFile == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 查询文件信息 - 使用userFile.getFileId()
            Long fileId = userFile.getFileId();
            File file = fileService.getById(fileId);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            
            objectNameToDownload = file.getObjectName();
            fileName = userFile.getFileName(); // 使用用户文件名而不是文件表中的文件名
            fileType = file.getFileType();
            fileSize = file.getFileSize();
            
            // 更新下载次数
            fileService.incrementDownloadCount(userFileId);
        } catch (NumberFormatException e) {
            // 如果不是数字，则直接使用objectName下载
            objectNameToDownload = objectName;
            fileName = objectName.substring(objectName.lastIndexOf("/") + 1);
        }
        
        // 处理文件名中可能包含的@2o.jpg等后缀
        if (fileName != null && fileName.contains("@")) {
            // 获取文件扩展名
            String extension = "";
            int lastDotIndex = fileName.lastIndexOf(".");
            if (lastDotIndex > 0) {
                extension = fileName.substring(lastDotIndex);
            }
            
            // 截取@之前的部分
            fileName = fileName.substring(0, fileName.indexOf("@"));
            
            // 如果截取后的文件名不包含扩展名，则添加扩展名
            if (!fileName.toLowerCase().endsWith(extension.toLowerCase())) {
                fileName += extension;
            }
        }
        
        // 获取文件流
        try {
            inputStream = storageService.downloadFile(bucketName, objectNameToDownload);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("下载文件失败: " + e.getMessage());
        }
        
        if (inputStream == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 设置文件名编码
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20"); // 替换空格为%20，避免某些浏览器问题
        
        // 构建响应头
        HttpHeaders headers = new HttpHeaders();
        
        // 从文件扩展名确定Content-Type
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        String fileExtension = "";
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            fileExtension = fileName.substring(lastDotIndex + 1).toLowerCase();
            
            // 根据扩展名设置常见的Content-Type
            switch (fileExtension) {
                case "jpg":
                case "jpeg":
                    mediaType = MediaType.IMAGE_JPEG;
                    break;
                case "png":
                    mediaType = MediaType.IMAGE_PNG;
                    break;
                case "gif":
                    mediaType = MediaType.IMAGE_GIF;
                    break;
                case "pdf":
                    mediaType = MediaType.APPLICATION_PDF;
                    break;
                case "html":
                    mediaType = MediaType.TEXT_HTML;
                    break;
                case "txt":
                    mediaType = MediaType.TEXT_PLAIN;
                    break;
                case "json":
                    mediaType = MediaType.APPLICATION_JSON;
                    break;
                case "xml":
                    mediaType = MediaType.APPLICATION_XML;
                    break;
                // 可以根据需要添加更多的类型
                default:
                    // 如果有已知的文件类型，优先使用
                    if (fileType != null && !fileType.isEmpty()) {
                        try {
                            mediaType = MediaType.parseMediaType(fileType);
                        } catch (Exception e) {
                            mediaType = MediaType.APPLICATION_OCTET_STREAM;
                        }
                    } else {
                        mediaType = MediaType.APPLICATION_OCTET_STREAM;
                    }
            }
        } else if (fileType != null && !fileType.isEmpty()) {
            // 如果没有扩展名但有已知的文件类型，使用已知的文件类型
            try {
                mediaType = MediaType.parseMediaType(fileType);
            } catch (Exception e) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        
        headers.setContentType(mediaType);
        
        // 判断是否为可以预览的内容类型
        boolean isPreviewable = mediaType.toString().startsWith("image/") || 
                               mediaType.toString().startsWith("text/") || 
                               mediaType.toString().equals("application/pdf");
        
        // 根据请求头判断是否需要强制下载
        boolean forceDownload = false;
        
        // 根据内容类型和强制下载标志决定是直接预览还是下载
        String contentDisposition;
        if (isPreviewable && !forceDownload) {
            // 对于可预览的文件类型，使用inline方式
            contentDisposition = "inline; filename=\"" + encodedFileName + "\"";
        } else {
            // 对于其他文件类型，使用attachment方式下载
            contentDisposition = "attachment; filename=\"" + encodedFileName + "\"";
        }
        
        headers.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
        
        // 如果知道文件大小，设置Content-Length
        if (fileSize != null) {
            headers.setContentLength(fileSize);
        }
        
        // 添加缓存控制头
        headers.setCacheControl("max-age=31536000"); // 缓存一年
        
        // 添加跨域头
        headers.add("Access-Control-Expose-Headers", "Content-Disposition");
        
        // 返回文件流
        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(inputStream));
    }
    
    /**
     * 删除文件
     * @param objectName 对象名称
     * @return 删除结果
     */
    @DeleteMapping("/{objectName}")
    public ResultVO<Boolean> deleteFile(@PathVariable("objectName") String objectName) {
        boolean result = storageService.removeFile(bucketName, objectName);
        return ResultVO.success("删除成功", result);
    }
    
    /**
     * 删除文件或文件夹（放入回收站）
     * @param requestBody 请求参数，包含fileIds或fileId
     * @param userId 用户ID
     * @return 删除结果
     */
    @PutMapping("/delete")
    public ResultVO<Boolean> moveToRecycleBin(
            @RequestBody Map<String, Object> requestBody,
            @RequestAttribute("userId") Long userId) {
        
        try {
            // 参数验证，支持fileIds数组或fileId单个值
            if (requestBody.containsKey("fileIds")) {
                // 处理fileIds数组
                List<Long> fileIds = null;
                Object fileIdsObj = requestBody.get("fileIds");
                
                if (fileIdsObj instanceof List) {
                    fileIds = ((List<?>) fileIdsObj).stream()
                            .map(id -> Long.valueOf(id.toString()))
                            .collect(Collectors.toList());
                } else if (fileIdsObj instanceof Object[]) {
                    fileIds = Arrays.stream((Object[]) fileIdsObj)
                            .map(id -> Long.valueOf(id.toString()))
                            .collect(Collectors.toList());
                }
                
                if (fileIds != null && !fileIds.isEmpty()) {
                    return ResultVO.success(fileService.batchDelete(fileIds, userId));
                } else {
                    return ResultVO.fail("参数错误：fileIds格式不正确");
                }
            } else if (requestBody.containsKey("fileId")) {
                // 处理单个fileId
                Long fileId = Long.valueOf(requestBody.get("fileId").toString());
                return ResultVO.success(fileService.delete(fileId, userId));
            } else if (requestBody.containsKey("id")) {
                // 兼容使用id作为参数名
                Long fileId = Long.valueOf(requestBody.get("id").toString());
                return ResultVO.success(fileService.delete(fileId, userId));
            } else {
                return ResultVO.fail("参数错误：缺少fileIds或fileId");
            }
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return ResultVO.fail("删除文件失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除文件或文件夹（放入回收站）- POST方法版本
     * @param requestBody 请求参数，包含fileIds、fileId或id
     * @param userId 用户ID
     * @return 删除结果
     */
    @PostMapping("/delete")
    public ResultVO<Boolean> moveToRecycleBinPost(
            @RequestBody Map<String, Object> requestBody,
            @RequestAttribute("userId") Long userId) {
        return moveToRecycleBin(requestBody, userId);
    }
    
    /**
     * 恢复回收站文件
     * @param requestBody 请求参数，包含fileIds、fileId或id
     * @param userId 用户ID
     * @return 恢复结果
     */
    @PutMapping("/restore")
    public ResultVO<Boolean> restoreFromRecycleBin(
            @RequestBody Map<String, Object> requestBody,
            @RequestAttribute("userId") Long userId) {
        
        try {
            // 参数验证，支持fileIds数组或fileId/id单个值
            if (requestBody.containsKey("fileIds")) {
                // 处理fileIds数组
                List<Long> fileIds = null;
                Object fileIdsObj = requestBody.get("fileIds");
                
                if (fileIdsObj instanceof List) {
                    fileIds = ((List<?>) fileIdsObj).stream()
                            .map(id -> Long.valueOf(id.toString()))
                            .collect(Collectors.toList());
                } else if (fileIdsObj instanceof Object[]) {
                    fileIds = Arrays.stream((Object[]) fileIdsObj)
                            .map(id -> Long.valueOf(id.toString()))
                            .collect(Collectors.toList());
                }
                
                if (fileIds == null || fileIds.isEmpty()) {
                    return ResultVO.fail("文件ID列表不能为空");
                }
                
                // 处理所有文件
                boolean allSuccess = true;
                for (Long fileId : fileIds) {
                    boolean result = fileService.restore(fileId, userId);
                    if (!result) {
                        allSuccess = false;
                    }
                }
                
                return ResultVO.success(allSuccess ? "所有文件已恢复" : "部分文件恢复失败", allSuccess);
            } else {
                // 处理单个fileId
                Long fileId = null;
                
                if (requestBody.containsKey("fileId")) {
                    fileId = Long.valueOf(requestBody.get("fileId").toString());
                } else if (requestBody.containsKey("id")) {
                    fileId = Long.valueOf(requestBody.get("id").toString());
                } else {
                    return ResultVO.fail("缺少必要参数: fileIds或fileId或id");
                }
                
                // 调用文件服务恢复文件
                boolean result = fileService.restore(fileId, userId);
                return ResultVO.success("文件已恢复", result);
            }
        } catch (NumberFormatException e) {
            return ResultVO.fail("参数格式错误: 文件ID必须是数字");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("恢复文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 彻底删除文件（从回收站删除）
     * @param requestBody 请求参数，包含fileIds、fileId或id
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping({"/delete/completely", "/remove"})
    public ResultVO<Boolean> deleteCompletely(
            @RequestBody Map<String, Object> requestBody,
            @RequestAttribute("userId") Long userId) {
        
        try {
            // 参数验证，支持fileIds数组或fileId/id单个值
            if (requestBody.containsKey("fileIds")) {
                // 处理fileIds数组
                List<Long> fileIds = null;
                Object fileIdsObj = requestBody.get("fileIds");
                
                if (fileIdsObj instanceof List) {
                    fileIds = ((List<?>) fileIdsObj).stream()
                            .map(id -> Long.valueOf(id.toString()))
                            .collect(Collectors.toList());
                } else if (fileIdsObj instanceof Object[]) {
                    fileIds = Arrays.stream((Object[]) fileIdsObj)
                            .map(id -> Long.valueOf(id.toString()))
                            .collect(Collectors.toList());
                }
                
                if (fileIds == null || fileIds.isEmpty()) {
                    return ResultVO.fail("文件ID列表不能为空");
                }
                
                // 处理所有文件
                boolean allSuccess = true;
                for (Long fileId : fileIds) {
                    boolean result = fileService.deleteCompletely(fileId, userId);
                    if (!result) {
                        allSuccess = false;
                    }
                }
                
                return ResultVO.success(allSuccess ? "所有文件已彻底删除" : "部分文件彻底删除失败", allSuccess);
            } else {
                // 处理单个fileId
                Long fileId = null;
                
                if (requestBody.containsKey("fileId")) {
                    fileId = Long.valueOf(requestBody.get("fileId").toString());
                } else if (requestBody.containsKey("id")) {
                    fileId = Long.valueOf(requestBody.get("id").toString());
                } else {
                    return ResultVO.fail("缺少必要参数: fileIds或fileId或id");
                }
                
                // 调用文件服务彻底删除文件
                boolean result = fileService.deleteCompletely(fileId, userId);
                return ResultVO.success("文件已彻底删除", result);
            }
        } catch (NumberFormatException e) {
            return ResultVO.fail("参数格式错误: 文件ID必须是数字");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("彻底删除文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 清空回收站
     * @param userId 用户ID
     * @return 清空结果
     */
    @DeleteMapping({"/recycle/empty", "/clear-recycle"})
    public ResultVO<Boolean> emptyRecycleBin(@RequestAttribute("userId") Long userId) {
        try {
            // 调用文件服务清空回收站
            boolean result = fileService.emptyRecycleBin(userId);
            return ResultVO.success("回收站已清空", result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("清空回收站失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取文件访问URL
     * @param objectName 对象名称
     * @param expires 过期时间（秒）
     * @return 文件访问URL
     */
    @GetMapping("/url")
    public ResultVO<String> getFileUrl(
            @RequestParam("objectName") String objectName,
            @RequestParam(value = "expires", defaultValue = "3600") Integer expires) {
        
        String url = storageService.getPresignedObjectUrl(bucketName, objectName, expires);
        return ResultVO.success(url);
    }
    
    /**
     * 获取文件列表
     * @param parentId 父文件夹ID
     * @param orderBy 排序字段
     * @param orderDirection 排序方向
     * @param userId 用户ID
     * @return 文件列表
     */
    @GetMapping("/list")
    public ResultVO<Object> getFileList(
            @RequestParam(value = "parentId", defaultValue = "0") Long parentId,
            @RequestParam(value = "orderBy", defaultValue = "name") String orderBy,
            @RequestParam(value = "orderDirection", defaultValue = "asc") String orderDirection,
            @RequestAttribute("userId") Long userId) {
        
        try {
            // 调用文件服务获取用户文件列表
            List<UserFile> fileList = fileService.getUserFileList(parentId, userId);
            
            // 根据排序参数排序
            if ("name".equals(orderBy)) {
                if ("asc".equals(orderDirection)) {
                    fileList.sort((a, b) -> a.getFileName().compareTo(b.getFileName()));
                } else {
                    fileList.sort((a, b) -> b.getFileName().compareTo(a.getFileName()));
                }
            } else if ("size".equals(orderBy)) {
                if ("asc".equals(orderDirection)) {
                    fileList.sort((a, b) -> a.getFileSize().compareTo(b.getFileSize()));
                } else {
                    fileList.sort((a, b) -> b.getFileSize().compareTo(a.getFileSize()));
                }
            } else if ("time".equals(orderBy)) {
                if ("asc".equals(orderDirection)) {
                    fileList.sort((a, b) -> a.getUpdateTime().compareTo(b.getUpdateTime()));
                } else {
                    fileList.sort((a, b) -> b.getUpdateTime().compareTo(a.getUpdateTime()));
                }
            }
            
            // 获取文件路径（面包屑导航）
            List<Map<String, Object>> pathList = fileService.getFilePath(parentId, userId);
            
            // 构建返回结果，包含list和path属性
            Map<String, Object> result = new HashMap<>();
            result.put("list", fileList);
            result.put("path", pathList);
            
            return ResultVO.success(result);
        } catch (Exception e) {
            return ResultVO.fail("获取文件列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取收藏的文件列表
     * @param orderBy 排序字段
     * @param orderDirection 排序方向
     * @param userId 用户ID
     * @return 收藏文件列表
     */
    @GetMapping("/starred")
    public ResultVO<Object> getStarredFiles(
            @RequestParam(value = "orderBy", defaultValue = "name") String orderBy,
            @RequestParam(value = "orderDirection", defaultValue = "asc") String orderDirection,
            @RequestAttribute("userId") Long userId) {
        
        try {
            // 使用专门的收藏文件查询方法
            List<UserFile> starredFiles = fileService.getStarredFiles(userId);
            
            // 根据排序参数排序
            if ("name".equals(orderBy)) {
                if ("asc".equals(orderDirection)) {
                    starredFiles.sort((a, b) -> a.getFileName().compareTo(b.getFileName()));
                } else {
                    starredFiles.sort((a, b) -> b.getFileName().compareTo(a.getFileName()));
                }
            } else if ("size".equals(orderBy)) {
                if ("asc".equals(orderDirection)) {
                    starredFiles.sort((a, b) -> a.getFileSize().compareTo(b.getFileSize()));
                } else {
                    starredFiles.sort((a, b) -> b.getFileSize().compareTo(a.getFileSize()));
                }
            } else if ("time".equals(orderBy)) {
                if ("asc".equals(orderDirection)) {
                    starredFiles.sort((a, b) -> a.getUpdateTime().compareTo(b.getUpdateTime()));
                } else {
                    starredFiles.sort((a, b) -> b.getUpdateTime().compareTo(a.getUpdateTime()));
                }
            }
            
            // 收藏页面不需要路径，使用空列表
            List<Map<String, Object>> emptyPath = new ArrayList<>();
            
            // 构建返回结果，包含list和path属性
            Map<String, Object> result = new HashMap<>();
            result.put("list", starredFiles);
            result.put("path", emptyPath);
            
            return ResultVO.success(result);
        } catch (Exception e) {
            return ResultVO.fail("获取收藏文件列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取回收站文件列表
     * @param orderBy 排序字段
     * @param orderDirection 排序方向
     * @param userId 用户ID
     * @return 回收站文件列表
     */
    @GetMapping("/recycle")
    public ResultVO<Object> getRecycleBinFiles(
            @RequestParam(value = "orderBy", defaultValue = "updateTime") String orderBy,
            @RequestParam(value = "orderDirection", defaultValue = "desc") String orderDirection,
            @RequestAttribute("userId") Long userId) {
        
        try {
            // 获取回收站文件列表
            List<UserFile> recycleBinList = fileService.getRecycleBinList(userId);
            
            // 根据排序参数排序
            if ("name".equals(orderBy)) {
                if ("asc".equals(orderDirection)) {
                    recycleBinList.sort((a, b) -> a.getFileName().compareTo(b.getFileName()));
                } else {
                    recycleBinList.sort((a, b) -> b.getFileName().compareTo(a.getFileName()));
                }
            } else if ("size".equals(orderBy)) {
                if ("asc".equals(orderDirection)) {
                    recycleBinList.sort((a, b) -> a.getFileSize().compareTo(b.getFileSize()));
                } else {
                    recycleBinList.sort((a, b) -> b.getFileSize().compareTo(a.getFileSize()));
                }
            } else if ("updateTime".equals(orderBy)) {
                if ("asc".equals(orderDirection)) {
                    recycleBinList.sort((a, b) -> a.getUpdateTime().compareTo(b.getUpdateTime()));
                } else {
                    recycleBinList.sort((a, b) -> b.getUpdateTime().compareTo(a.getUpdateTime()));
                }
            }
            
            // 回收站页面不需要路径，使用空列表
            List<Map<String, Object>> emptyPath = new ArrayList<>();
            
            // 构建返回结果，包含list和path属性
            Map<String, Object> result = new HashMap<>();
            result.put("list", recycleBinList);
            result.put("path", emptyPath);
            
            return ResultVO.success(result);
        } catch (Exception e) {
            return ResultVO.fail("获取回收站文件列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取回收站中特定文件夹的内容
     * @param folderId 文件夹ID
     * @param orderBy 排序字段
     * @param orderDirection 排序方向
     * @param userId 用户ID
     * @return 文件夹内容列表
     */
    @GetMapping("/recycle/folder/{folderId}")
    public ResultVO<Object> getRecycleBinFolderContents(
            @PathVariable("folderId") Long folderId,
            @RequestParam(value = "orderBy", defaultValue = "updateTime") String orderBy,
            @RequestParam(value = "orderDirection", defaultValue = "desc") String orderDirection,
            @RequestAttribute("userId") Long userId) {
        
        try {
            // 获取文件夹信息
            UserFile folder = fileService.getUserFileById(folderId, userId);
            if (folder == null || !folder.getIsDir() || folder.getDelFlag() != 1) {
                return ResultVO.fail("文件夹不存在或不在回收站中");
            }
            
            // 获取文件夹内容
            List<UserFile> folderContents = fileService.getDeletedSubFiles(folderId, userId);
            
            // 根据排序参数排序
            if ("name".equals(orderBy)) {
                if ("asc".equals(orderDirection)) {
                    folderContents.sort((a, b) -> a.getFileName().compareTo(b.getFileName()));
                } else {
                    folderContents.sort((a, b) -> b.getFileName().compareTo(a.getFileName()));
                }
            } else if ("size".equals(orderBy)) {
                if ("asc".equals(orderDirection)) {
                    folderContents.sort((a, b) -> a.getFileSize().compareTo(b.getFileSize()));
                } else {
                    folderContents.sort((a, b) -> b.getFileSize().compareTo(a.getFileSize()));
                }
            } else if ("updateTime".equals(orderBy)) {
                if ("asc".equals(orderDirection)) {
                    folderContents.sort((a, b) -> a.getUpdateTime().compareTo(b.getUpdateTime()));
                } else {
                    folderContents.sort((a, b) -> b.getUpdateTime().compareTo(a.getUpdateTime()));
                }
            }
            
            // 构建路径信息
            List<Map<String, Object>> pathList = new ArrayList<>();
            Map<String, Object> folderInfo = new HashMap<>();
            folderInfo.put("id", folder.getId());
            folderInfo.put("name", folder.getFileName());
            pathList.add(folderInfo);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("list", folderContents);
            result.put("path", pathList);
            
            return ResultVO.success(result);
        } catch (Exception e) {
            return ResultVO.fail("获取回收站文件夹内容失败: " + e.getMessage());
        }
    }
    
    /**
     * 收藏/取消收藏文件
     * @param fileId 文件ID
     * @param starRequest 收藏请求参数
     * @param userId 用户ID
     * @return 操作结果
     */
    @PutMapping("/star/{fileId}")
    public ResultVO<Boolean> toggleStarFile(
            @PathVariable("fileId") Long fileId,
            @RequestBody Map<String, Boolean> starRequest,
            @RequestAttribute("userId") Long userId) {
        
        Boolean isStar = starRequest.get("star");
        if (isStar == null) {
            return ResultVO.fail("缺少必要参数");
        }
        
        try {
            // 调用文件服务切换收藏状态
            boolean result = fileService.toggleStar(fileId, isStar, userId);
            return ResultVO.success(isStar ? "收藏成功" : "取消收藏成功", result);
        } catch (Exception e) {
            return ResultVO.fail(isStar ? "收藏失败" : "取消收藏失败" + ": " + e.getMessage());
        }
    }
    
    /**
     * 创建文件夹
     * @param requestBody 请求参数，包含parentId和name
     * @param userId 用户ID
     * @return 创建结果
     */
    @PostMapping("/folder")
    public ResultVO<Object> createFolder(
            @RequestBody Map<String, Object> requestBody,
            @RequestAttribute("userId") Long userId) {
        
        try {
            // 参数验证
            if (!requestBody.containsKey("parentId") || !requestBody.containsKey("name")) {
                return ResultVO.fail("缺少必要参数: parentId或name");
            }
            
            if (requestBody.get("name") == null || requestBody.get("name").toString().trim().isEmpty()) {
                return ResultVO.fail("文件夹名称不能为空");
            }
            
            Long parentId = Long.valueOf(requestBody.get("parentId").toString());
            String name = requestBody.get("name").toString().trim();
            
            // 调用文件服务创建文件夹
            UserFile folder = fileService.createFolder(name, parentId, userId);
            
            // 获取文件路径（面包屑导航）
            List<Map<String, Object>> pathList = fileService.getFilePath(parentId, userId);
            
            // 构建返回结果，包含list和path属性，与getFileList方法保持一致
            Map<String, Object> result = new HashMap<>();
            List<UserFile> fileList = new ArrayList<>();
            fileList.add(folder);
            result.put("list", fileList);
            result.put("path", pathList);
            
            return ResultVO.success("创建文件夹成功", result);
        } catch (NumberFormatException e) {
            return ResultVO.fail("参数格式错误: parentId必须是数字");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("创建文件夹失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取文件详情
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件详情
     */
    @GetMapping("/detail/{fileId}")
    public ResultVO<UserFile> getFileDetail(
            @PathVariable("fileId") Long fileId,
            @RequestAttribute("userId") Long userId) {
        
        UserFile userFile = fileService.getUserFileById(fileId, userId);
        if (userFile == null) {
            return ResultVO.fail("文件不存在或无权限访问");
        }
        
        return ResultVO.success(userFile);
    }

    /**
     * 获取文件预览URL
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 文件预览URL
     */
    @GetMapping("/preview/{fileId}")
    public ResultVO<String> getFilePreviewUrl(
            @PathVariable("fileId") Long fileId,
            @RequestAttribute("userId") Long userId) {
        
        UserFile userFile = fileService.getUserFileById(fileId, userId);
        if (userFile == null) {
            return ResultVO.fail("文件不存在或无权限访问");
        }
        
        // 获取文件信息
        Long realFileId = userFile.getFileId();
        File file = fileService.getById(realFileId);
        if (file == null) {
            return ResultVO.fail("文件元数据不存在");
        }
        
        // 获取临时预览URL
        String previewUrl = storageService.getPresignedObjectUrl(bucketName, file.getObjectName(), 3600);
        
        return ResultVO.success(previewUrl);
    }

    /**
     * 搜索文件
     * @param keyword 关键词
     * @param orderBy 排序字段
     * @param orderDirection 排序方向
     * @param userId 用户ID
     * @return 搜索结果
     */
    @GetMapping("/search")
    public ResultVO<Object> searchFiles(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "orderBy", defaultValue = "name") String orderBy,
            @RequestParam(value = "orderDirection", defaultValue = "asc") String orderDirection,
            @RequestAttribute("userId") Long userId) {
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResultVO.fail("搜索关键词不能为空");
        }
        
        try {
            // 调用搜索服务
            List<UserFile> searchResults = fileSearchService.searchFiles(keyword, userId);
            
            // 排序处理
            sortUserFiles(searchResults, orderBy, orderDirection);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("list", searchResults);
            
            return ResultVO.success("搜索成功", result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件分片
     * @param file 文件分片
     * @param parentId 父文件夹ID
     * @param md5 文件MD5值
     * @param chunkIndex 分片索引
     * @param chunks 总分片数
     * @param userId 用户ID
     * @return 上传结果
     */
    @PostMapping("/upload-chunk")
    public ResultVO<String> uploadChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("parentId") Long parentId,
            @RequestParam("md5") String md5,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("chunks") Integer chunks,
            @RequestAttribute("userId") Long userId) {
        
        if (file.isEmpty()) {
            return ResultVO.fail("上传文件分片不能为空");
        }
        
        try {
            // 调用文件服务保存分片
            String chunkPath = fileService.saveChunk(file, md5, chunkIndex, chunks, userId);
            return ResultVO.success("分片上传成功", chunkPath);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("分片上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 合并文件分片
     * @param requestBody 请求参数，包含md5、fileName、parentId和chunks
     * @param userId 用户ID
     * @return 合并结果
     */
    @PostMapping("/merge-chunks")
    public ResultVO<UserFile> mergeChunks(
            @RequestBody Map<String, Object> requestBody,
            @RequestAttribute("userId") Long userId) {
        
        try {
            // 参数验证
            if (!requestBody.containsKey("md5") || !requestBody.containsKey("fileName") || 
                !requestBody.containsKey("parentId") || !requestBody.containsKey("chunks")) {
                return ResultVO.fail("缺少必要参数: md5、fileName、parentId或chunks");
            }
            
            String md5 = requestBody.get("md5").toString();
            String fileName = requestBody.get("fileName").toString();
            Long parentId = Long.valueOf(requestBody.get("parentId").toString());
            Integer chunks = Integer.valueOf(requestBody.get("chunks").toString());
            
            // 调用文件服务合并分片
            UserFile userFile = fileService.mergeChunks(md5, fileName, parentId, chunks, userId);
            
            return ResultVO.success("文件合并成功", userFile);
        } catch (NumberFormatException e) {
            return ResultVO.fail("参数格式错误: parentId或chunks必须是数字");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("文件合并失败: " + e.getMessage());
        }
    }
    
    /**
     * 重建文件索引
     * @param userId 用户ID
     * @return 结果
     */
    @PostMapping("/rebuild-index")
    public ResultVO<Boolean> rebuildFileIndex(@RequestAttribute("userId") Long userId) {
        try {
            boolean result = fileService.rebuildFileIndex(userId);
            if (result) {
                return ResultVO.success("重建索引已提交，请稍后", true);
            } else {
                return ResultVO.fail("没有可索引的文件");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("重建索引失败: " + e.getMessage());
        }
    }
    
    /**
     * 对文件列表进行排序
     * @param userFiles 文件列表
     * @param orderBy 排序字段：name(文件名)、size(大小)、time(时间)
     * @param orderDirection 排序方向：asc(升序)、desc(降序)
     */
    private void sortUserFiles(List<UserFile> userFiles, String orderBy, String orderDirection) {
        if (userFiles == null || userFiles.isEmpty()) {
            return;
        }
        
        // 按文件名排序
        if ("name".equalsIgnoreCase(orderBy)) {
            if ("asc".equalsIgnoreCase(orderDirection)) {
                userFiles.sort((a, b) -> a.getFileName().compareTo(b.getFileName()));
            } else {
                userFiles.sort((a, b) -> b.getFileName().compareTo(a.getFileName()));
            }
        }
        // 按文件大小排序
        else if ("size".equalsIgnoreCase(orderBy)) {
            if ("asc".equalsIgnoreCase(orderDirection)) {
                userFiles.sort((a, b) -> a.getFileSize().compareTo(b.getFileSize()));
            } else {
                userFiles.sort((a, b) -> b.getFileSize().compareTo(a.getFileSize()));
            }
        }
        // 按修改时间排序
        else if ("time".equalsIgnoreCase(orderBy) || "updateTime".equalsIgnoreCase(orderBy)) {
            if ("asc".equalsIgnoreCase(orderDirection)) {
                userFiles.sort((a, b) -> a.getUpdateTime().compareTo(b.getUpdateTime()));
            } else {
                userFiles.sort((a, b) -> b.getUpdateTime().compareTo(a.getUpdateTime()));
            }
        }
    }

    /**
     * 检查文件MD5是否已存在（秒传功能）
     * @param requestBody 请求参数，包含md5、fileName和fileSize
     * @param userId 用户ID
     * @return 检查结果
     */
    @PostMapping("/check-md5")
    public ResultVO<Object> checkFileMd5(
            @RequestBody Map<String, Object> requestBody,
            @RequestAttribute("userId") Long userId) {
        
        try {
            // 参数验证
            if (!requestBody.containsKey("md5") || !requestBody.containsKey("fileName") || !requestBody.containsKey("fileSize")) {
                return ResultVO.fail("缺少必要参数: md5、fileName或fileSize");
            }
            
            String md5 = requestBody.get("md5").toString();
            String fileName = requestBody.get("fileName").toString();
            Long fileSize = Long.valueOf(requestBody.get("fileSize").toString());
            
            // 检查文件是否已存在
            Map<String, Object> result = fileService.checkFileMd5(md5, fileName, fileSize, userId);
            
            return ResultVO.success("检查成功", result);
        } catch (NumberFormatException e) {
            return ResultVO.fail("参数格式错误: fileSize必须是数字");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("检查文件MD5失败: " + e.getMessage());
        }
    }
} 