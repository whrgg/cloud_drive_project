package com.clouddrive.controller;

import com.clouddrive.model.dto.ShareDTO;
import com.clouddrive.model.entity.File;
import com.clouddrive.model.entity.Share;
import com.clouddrive.model.entity.UserFile;
import com.clouddrive.model.vo.ResultVO;
import com.clouddrive.model.vo.ShareVO;
import com.clouddrive.service.FileService;
import com.clouddrive.service.ShareService;
import com.clouddrive.service.StorageService;
import com.clouddrive.service.UserFileService;
import com.clouddrive.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 分享控制器
 */
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareController {
    
    private final ShareService shareService;
    private final UserFileService userFileService;
    private final FileService fileService;
    private final StorageService storageService;
    private final JwtUtil jwtUtil;
    
    @Value("${minio.bucketName:cloud-drive}")
    private String bucketName;
    
    /**
     * 创建分享
     */
    @PostMapping("/create")
    public ResultVO<ShareVO> createShare(@RequestBody @Valid ShareDTO shareDTO, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        ShareVO shareVO = shareService.createShare(shareDTO, userId);
        return ResultVO.success(shareVO);
    }
    
    /**
     * 获取分享信息
     */
    @GetMapping("/info/{shareCode}")
    public ResultVO<ShareVO> getShareInfo(@PathVariable String shareCode) {
        ShareVO shareVO = shareService.getShareInfo(shareCode);
        if (shareVO == null) {
            return ResultVO.fail("分享不存在或已过期");
        }
        return ResultVO.success(shareVO);
    }
    
    /**
     * 验证分享提取码
     */
    @PostMapping("/verify/{shareId}")
    public ResultVO<Boolean> verifyExtractionCode(
            @PathVariable String shareId,
            @RequestBody Map<String, String> codeData) {
        String code = codeData.get("code");
        boolean verified = shareService.verifyExtractionCode(shareId, code);
        if (!verified) {
            return ResultVO.fail("提取码错误");
        }
        return ResultVO.success(true);
    }
    
    /**
     * 获取分享文件列表
     */
    @GetMapping("/list/{shareId}")
    public ResultVO<Object> getShareFileList(
            @PathVariable String shareId,
            @RequestParam(value = "parentId", defaultValue = "0") Long parentId) {
        try {
            // 检查分享是否存在
            Share share = shareService.getById(Long.parseLong(shareId));
            if (share == null || share.getStatus() != 1) {
                return ResultVO.fail("分享不存在或已过期");
            }
            
            // 获取原始分享的文件ID
            Long userFileId = share.getUserFileId();
            
            // 如果请求的是根目录(parentId=0)，那么就返回分享的根文件/文件夹
            if (parentId == 0) {
                UserFile sharedFile = userFileService.getById(userFileId);
                if (sharedFile == null) {
                    return ResultVO.fail("分享的文件不存在");
                }
                
                // 如果是文件夹，则查询文件夹下的文件
                if (sharedFile.getIsDir()) {
                    List<UserFile> files = userFileService.getUserFileList(userFileId, sharedFile.getUserId());
                    return ResultVO.success(files);
                } else {
                    // 如果是单个文件，则返回该文件信息
                    List<UserFile> fileList = new ArrayList<>();
                    fileList.add(sharedFile);
                    return ResultVO.success(fileList);
                }
            } else {
                // 如果请求的不是根目录，则查询指定目录下的文件
                // 首先检查该目录是否属于分享范围内
                UserFile requestedFolder = userFileService.getById(parentId);
                if (requestedFolder == null || !requestedFolder.getIsDir()) {
                    return ResultVO.fail("请求的文件夹不存在");
                }
                
                // 检查请求的文件夹是否在分享范围内
                boolean isInShareScope = shareService.checkFileInShareScope(requestedFolder.getId(), userFileId);
                if (!isInShareScope) {
                    return ResultVO.fail("请求的文件夹不在分享范围内");
                }
                
                // 获取文件夹下的文件列表
                List<UserFile> files = userFileService.getUserFileList(parentId, requestedFolder.getUserId());
                return ResultVO.success(files);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("获取分享文件列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取我的分享列表
     * @param page 页码
     * @param size 每页大小
     * @param orderBy 排序字段
     * @param orderDirection 排序方向
     * @param request HTTP请求
     * @return 分享列表
     */
    @GetMapping("/my")
    public ResultVO<List<ShareVO>> getMyShares(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "orderBy", required = false) String orderBy,
            @RequestParam(value = "orderDirection", defaultValue = "desc") String orderDirection,
            HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return ResultVO.fail("未获取到用户信息");
        }
        List<ShareVO> shareList = shareService.getUserShareList(userId);
        return ResultVO.success(shareList);
    }
    
    /**
     * 取消分享
     */
    @DeleteMapping("/cancel/{shareId}")
    public ResultVO<Boolean> cancelShare(@PathVariable String shareId, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        boolean success = shareService.cancelShare(Long.parseLong(shareId), userId);
        if (!success) {
            return ResultVO.fail("取消分享失败");
        }
        return ResultVO.success(true);
    }
    
    /**
     * 批量取消分享
     */
    @DeleteMapping("/batch/cancel")
    public ResultVO<Boolean> batchCancelShare(@RequestBody Map<String, List<String>> cancelRequest, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        List<String> shareIds = cancelRequest.get("shareIds");
        if (shareIds == null || shareIds.isEmpty()) {
            return ResultVO.fail("分享ID列表不能为空");
        }
        
        try {
            int successCount = 0;
            List<String> failedIds = new ArrayList<>();
            
            for (String shareIdStr : shareIds) {
                try {
                    Long shareId = Long.parseLong(shareIdStr);
                    boolean canceled = shareService.cancelShare(shareId, userId);
                    if (canceled) {
                        successCount++;
                    } else {
                        failedIds.add(shareIdStr);
                    }
                } catch (NumberFormatException e) {
                    failedIds.add(shareIdStr);
                }
            }
            
            if (failedIds.isEmpty()) {
                return ResultVO.success("成功取消" + successCount + "个分享", true);
            } else {
                return ResultVO.success("成功取消" + successCount + "个分享，失败" + failedIds.size() + "个", true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("批量取消分享失败: " + e.getMessage());
        }
    }
    
    /**
     * 增加分享浏览次数
     */
    @PutMapping("/views/{shareId}")
    public ResultVO<Boolean> increaseViewCount(@PathVariable String shareId) {
        try {
            // 尝试将参数解析为Long类型的分享ID
            Long shareIdLong;
            try {
                shareIdLong = Long.parseLong(shareId);
            } catch (NumberFormatException e) {
                // 如果不是数字，则当作分享码处理
                Share share = shareService.getShareByCode(shareId);
                if (share == null) {
                    return ResultVO.fail("分享不存在或已过期");
                }
                shareIdLong = share.getId();
            }
            
            // 增加浏览次数
            boolean success = shareService.incrementViewCount(shareIdLong);
            if (!success) {
                return ResultVO.fail("更新浏览次数失败");
            }
            return ResultVO.success(true);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("更新浏览次数失败: " + e.getMessage());
        }
    }
    
    /**
     * 增加分享下载次数
     */
    @PutMapping("/downloads/{shareId}")
    public ResultVO<Boolean> increaseDownloadCount(@PathVariable String shareId) {
        try {
            // 尝试将参数解析为Long类型的分享ID
            Long shareIdLong;
            try {
                shareIdLong = Long.parseLong(shareId);
            } catch (NumberFormatException e) {
                // 如果不是数字，则当作分享码处理
                Share share = shareService.getShareByCode(shareId);
                if (share == null) {
                    return ResultVO.fail("分享不存在或已过期");
                }
                shareIdLong = share.getId();
            }
            
            // 增加下载次数
            boolean success = shareService.incrementDownloadCount(shareIdLong);
            if (!success) {
                return ResultVO.fail("更新下载次数失败");
            }
            return ResultVO.success(true);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("更新下载次数失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存分享文件到自己的网盘
     */
    @PostMapping("/save/{shareId}")
    public ResultVO<Boolean> saveShareFiles(
            @PathVariable String shareId,
            @RequestBody Map<String, Object> saveRequest,
            HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        
        try {
            // 检查分享是否存在
            Long shareIdLong;
            Share share;
            
            try {
                // 尝试将参数解析为Long类型的分享ID
                shareIdLong = Long.parseLong(shareId);
                share = shareService.getById(shareIdLong);
            } catch (NumberFormatException e) {
                // 如果不是数字，则当作分享码处理
                share = shareService.getShareByCode(shareId);
                if (share != null) {
                    shareIdLong = share.getId();
                } else {
                    return ResultVO.fail("分享不存在或已过期");
                }
            }
            
            // 检查分享状态
            if (share == null || share.getStatus() != 1) {
                return ResultVO.fail("分享不存在或已过期");
            }
            
            // 获取请求参数
            List<Long> fileIds = new ArrayList<>();
            Object fileIdsObj = saveRequest.get("fileIds");
            if (fileIdsObj instanceof List) {
                for (Object item : (List<?>) fileIdsObj) {
                    if (item instanceof Integer) {
                        fileIds.add(((Integer) item).longValue());
                    } else if (item instanceof Long) {
                        fileIds.add((Long) item);
                    } else if (item instanceof String) {
                        try {
                            fileIds.add(Long.parseLong((String) item));
                        } catch (NumberFormatException e) {
                            // 忽略无效的ID
                        }
                    }
                }
            }
            
            // 获取目标文件夹ID
            Long targetFolderId = 0L;  // 默认保存到根目录
            Object targetFolderIdObj = saveRequest.get("targetFolderId");
            if (targetFolderIdObj != null) {
                if (targetFolderIdObj instanceof Integer) {
                    targetFolderId = ((Integer) targetFolderIdObj).longValue();
                } else if (targetFolderIdObj instanceof Long) {
                    targetFolderId = (Long) targetFolderIdObj;
                } else if (targetFolderIdObj instanceof String) {
                    try {
                        targetFolderId = Long.parseLong((String) targetFolderIdObj);
                    } catch (NumberFormatException e) {
                        // 使用默认值
                    }
                }
            }
            
            // 检查目标文件夹是否存在
            if (targetFolderId > 0) {
                UserFile targetFolder = userFileService.getById(targetFolderId);
                if (targetFolder == null || !targetFolder.getIsDir() || !targetFolder.getUserId().equals(userId)) {
                    return ResultVO.fail("目标文件夹不存在或无权限");
                }
            }
            
            // 执行保存操作
            if (fileIds.isEmpty()) {
                // 如果未指定文件ID，则保存分享的根文件/文件夹
                boolean success = shareService.saveShareFile(share.getUserFileId(), targetFolderId, userId, share.getUserId());
                if (!success) {
                    return ResultVO.fail("保存分享文件失败");
                }
            } else {
                // 如果指定了文件ID，则保存指定的文件
                int successCount = 0;
                for (Long fileId : fileIds) {
                    // 检查文件是否在分享范围内
                    boolean isInShareScope = shareService.checkFileInShareScope(fileId, share.getUserFileId());
                    if (!isInShareScope) {
                        continue;
                    }
                    
                    boolean success = shareService.saveShareFile(fileId, targetFolderId, userId, share.getUserId());
                    if (success) {
                        successCount++;
                    }
                }
                
                if (successCount == 0) {
                    return ResultVO.fail("未能保存任何文件");
                } else if (successCount < fileIds.size()) {
                    return ResultVO.success("成功保存 " + successCount + "/" + fileIds.size() + " 个文件", true);
                }
            }
            
            // 增加分享保存次数
            shareService.incrementSaveCount(shareIdLong);
            
            return ResultVO.success("文件保存成功", true);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("保存分享文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载分享文件
     * @param shareCode 分享码
     * @param userFileId 用户文件ID
     * @return 文件流
     */
    @GetMapping("/download/{shareCode}/{userFileId}")
    public ResponseEntity<?> downloadShareFile(
            @PathVariable String shareCode,
            @PathVariable Long userFileId) {
        try {
            // 查询分享信息
            Share share = shareService.getShareByCode(shareCode);
            if (share == null || share.getStatus() != 1) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查分享的文件ID是否与请求的文件ID匹配
            if (!share.getUserFileId().equals(userFileId)) {
                return ResponseEntity.status(403).body("无权访问该文件");
            }
            
            // 查询用户文件信息
            UserFile userFile = userFileService.getById(userFileId);
            if (userFile == null || userFile.getDelFlag() > 0) {
                return ResponseEntity.notFound().build();
            }
            
            // 查询文件信息 - 使用userFile.getFileId()
            Long fileId = userFile.getFileId();
            File file = fileService.getById(fileId);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 增加下载次数
            shareService.incrementDownloadCount(share.getId());
            fileService.incrementDownloadCount(userFileId);
            
            // 获取文件流
            InputStream inputStream = storageService.downloadFile(bucketName, file.getObjectName());
            if (inputStream == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 设置文件名
            String fileName = userFile.getFileName();
            // 处理文件名中可能包含的@2o.jpg等后缀
            if (fileName.contains("@")) {
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
                        if (file.getFileType() != null && !file.getFileType().isEmpty()) {
                            try {
                                mediaType = MediaType.parseMediaType(file.getFileType());
                            } catch (Exception e) {
                                mediaType = MediaType.APPLICATION_OCTET_STREAM;
                            }
                        } else {
                            mediaType = MediaType.APPLICATION_OCTET_STREAM;
                        }
                }
            } else if (file.getFileType() != null && !file.getFileType().isEmpty()) {
                // 如果没有扩展名但有已知的文件类型，使用已知的文件类型
                try {
                    mediaType = MediaType.parseMediaType(file.getFileType());
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
            if (file.getFileSize() != null) {
                headers.setContentLength(file.getFileSize());
            }
            
            // 添加缓存控制头
            headers.setCacheControl("max-age=31536000"); // 缓存一年
            
            // 添加跨域头
            headers.add("Access-Control-Expose-Headers", "Content-Disposition");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("下载文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 预览分享文件
     * @param shareCode 分享码
     * @param userFileId 用户文件ID
     * @return 文件流
     */
    @GetMapping("/preview/{shareCode}/{userFileId}")
    public ResponseEntity<?> previewShareFile(
            @PathVariable String shareCode,
            @PathVariable Long userFileId,
            @RequestParam(required = false) String code) {
        try {
            // 查询分享信息
            Share share = shareService.getShareByCode(shareCode);
            if (share == null || share.getStatus() != 1) {
                return ResponseEntity.notFound().build();
            }
            
            // 验证提取码
            if (share.getExtractionCode() != null && !share.getExtractionCode().isEmpty()) {
                if (code == null || !share.getExtractionCode().equals(code)) {
                    return ResponseEntity.status(403).body("提取码错误");
                }
            }
            
            // 检查分享的文件ID是否与请求的文件ID匹配
            if (!share.getUserFileId().equals(userFileId)) {
                return ResponseEntity.status(403).body("无权访问该文件");
            }
            
            // 查询用户文件信息
            UserFile userFile = userFileService.getById(userFileId);
            if (userFile == null || userFile.getDelFlag() > 0) {
                return ResponseEntity.notFound().build();
            }
            
            // 查询文件信息 - 使用userFile.getFileId()
            Long fileId = userFile.getFileId();
            File file = fileService.getById(fileId);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 增加浏览次数
            shareService.incrementViewCount(share.getId());
            
            // 获取文件流
            InputStream inputStream = storageService.downloadFile(bucketName, file.getObjectName());
            if (inputStream == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 设置文件名
            String fileName = userFile.getFileName();
            // 处理文件名中可能包含的@2o.jpg等后缀
            if (fileName.contains("@")) {
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
                        if (file.getFileType() != null && !file.getFileType().isEmpty()) {
                            try {
                                mediaType = MediaType.parseMediaType(file.getFileType());
                            } catch (Exception e) {
                                mediaType = MediaType.APPLICATION_OCTET_STREAM;
                            }
                        } else {
                            mediaType = MediaType.APPLICATION_OCTET_STREAM;
                        }
                }
            } else if (file.getFileType() != null && !file.getFileType().isEmpty()) {
                // 如果没有扩展名但有已知的文件类型，使用已知的文件类型
                try {
                    mediaType = MediaType.parseMediaType(file.getFileType());
                } catch (Exception e) {
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
                }
            }
            
            headers.setContentType(mediaType);
            
            // 预览接口始终使用inline
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedFileName + "\"");
            
            // 如果知道文件大小，设置Content-Length
            if (file.getFileSize() != null) {
                headers.setContentLength(file.getFileSize());
            }
            
            // 添加缓存控制头
            headers.setCacheControl("max-age=31536000"); // 缓存一年
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("预览文件失败: " + e.getMessage());
        }
    }
} 