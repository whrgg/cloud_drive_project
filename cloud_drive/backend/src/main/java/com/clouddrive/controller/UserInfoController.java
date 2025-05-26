package com.clouddrive.controller;

import com.clouddrive.model.dto.UserInfoDTO;
import com.clouddrive.model.entity.UserFile;
import com.clouddrive.model.vo.ResultVO;
import com.clouddrive.model.vo.UserVO;
import com.clouddrive.service.UserService;
import com.clouddrive.service.StorageService;
import com.clouddrive.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * 用户信息控制器
 */
@RestController
@RequestMapping("/user")
public class UserInfoController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private StorageService storageService;
    
    @Autowired
    private FileService fileService;
    
    @Value("${minio.bucketName:cloud-drive}")
    private String bucketName;
    
    /**
     * 获取当前用户信息
     * @param userId 用户ID（从请求属性中获取）
     * @return 用户信息
     */
    @GetMapping("/info")
    public ResultVO<UserVO> getUserInfo(@RequestAttribute(name = "userId") Long userId) {
        UserVO userVO = userService.getUserInfo(userId);
        return ResultVO.success(userVO);
    }
    
    /**
     * 更新用户信息
     * @param userInfoDTO 用户信息
     * @param userId 用户ID
     * @return 更新结果
     */
    @PutMapping("/info")
    public ResultVO<Boolean> updateUserInfo(
            @RequestBody UserInfoDTO userInfoDTO,
            @RequestAttribute("userId") Long userId) {
        
        try {
            boolean updated = userService.updateUserInfo(userId, userInfoDTO);
            if (updated) {
                return ResultVO.success("更新用户信息成功", true);
            } else {
                return ResultVO.fail("更新用户信息失败");
            }
        } catch (Exception e) {
            return ResultVO.fail(e.getMessage());
        }
    }
    
    /**
     * 获取用户存储空间信息
     * @param userId 用户ID（从请求属性中获取）
     * @return 存储空间信息
     */
    @GetMapping("/storage")
    public ResultVO<Object> getUserStorageInfo(@RequestAttribute(name = "userId") Long userId) {
        try {
            UserVO userVO = userService.getUserInfo(userId);
            
            // 构建存储信息响应
            Map<String, Object> storageInfo = new HashMap<>();
            storageInfo.put("totalSpace", userVO.getStorageSize());
            storageInfo.put("usedSpace", userVO.getUsedSize());
            storageInfo.put("availableSpace", userVO.getStorageSize() - userVO.getUsedSize());
            storageInfo.put("usagePercent", calculateUsagePercent(userVO.getUsedSize(), userVO.getStorageSize()));
            
            // 获取文件类型分布
            Map<String, Long> typeDistribution = getFileTypeDistribution(userId);
            storageInfo.put("typeDistribution", typeDistribution);
            
            return ResultVO.success(storageInfo);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("获取存储空间信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 计算使用百分比
     * @param usedSpace 已使用空间
     * @param totalSpace 总空间
     * @return 使用百分比
     */
    private double calculateUsagePercent(Long usedSpace, Long totalSpace) {
        if (totalSpace == null || totalSpace == 0) {
            return 0;
        }
        return Math.round((double) usedSpace / totalSpace * 100.0 * 100) / 100.0;
    }
    
    /**
     * 获取文件类型分布
     * @param userId 用户ID
     * @return 文件类型分布
     */
    private Map<String, Long> getFileTypeDistribution(Long userId) {
        // 获取用户所有文件（不包括已删除的）
        List<UserFile> userFiles = fileService.getUserAllFiles(userId);
        
        // 按文件类型分组统计大小
        Map<String, Long> typeDistribution = new HashMap<>();
        
        for (UserFile userFile : userFiles) {
            // 跳过文件夹
            if (userFile.getIsDir()) {
                continue;
            }
            
            // 获取文件类型
            String fileType = getFileCategory(userFile.getFileType());
            
            // 累加大小
            typeDistribution.put(fileType, 
                typeDistribution.getOrDefault(fileType, 0L) + userFile.getFileSize());
        }
        
        return typeDistribution;
    }
    
    /**
     * 获取文件分类
     * @param mimeType 文件MIME类型
     * @return 文件分类
     */
    private String getFileCategory(String mimeType) {
        if (mimeType == null) {
            return "其他";
        }
        
        if (mimeType.startsWith("image/")) {
            return "图片";
        } else if (mimeType.startsWith("video/")) {
            return "视频";
        } else if (mimeType.startsWith("audio/")) {
            return "音频";
        } else if (mimeType.startsWith("text/")) {
            return "文档";
        } else if (mimeType.contains("pdf") || mimeType.contains("document") || 
                  mimeType.contains("excel") || mimeType.contains("powerpoint")) {
            return "文档";
        } else if (mimeType.contains("zip") || mimeType.contains("rar") || 
                  mimeType.contains("tar") || mimeType.contains("gzip")) {
            return "压缩包";
        } else {
            return "其他";
        }
    }
    
    /**
     * 上传用户头像
     * @param file 头像文件
     * @param userId 用户ID
     * @return 上传结果
     */
    @PostMapping("/avatar")
    public ResultVO<String> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("userId") Long userId) {
        
        if (file.isEmpty()) {
            return ResultVO.fail("头像文件不能为空");
        }
        
        try {
            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResultVO.fail("只能上传图片文件");
            }
            
            // 限制文件大小（最大5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResultVO.fail("头像文件大小不能超过5MB");
            }
            
            // 构建头像存储路径，确保路径规范化
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "avatar_" + System.currentTimeMillis() + ".jpg";
            }
            
            // 替换文件名中的空格和特殊字符
            String safeFilename = originalFilename.replaceAll("[\\s\\\\/:*?\"<>|]", "_");
            String objectName = "avatars/" + userId + "/" + System.currentTimeMillis() + "_" + safeFilename;
            
            System.out.println("上传头像: " + objectName);
            
            // 上传头像到MinIO
            String avatarUrl = storageService.uploadFile(bucketName, objectName, file, file.getContentType());
            System.out.println("头像URL: " + avatarUrl);
            
            // 更新用户头像信息
            boolean updated = userService.updateAvatar(userId, avatarUrl);
            if (!updated) {
                return ResultVO.fail("更新用户头像信息失败");
            }
            
            // 返回头像URL
            return ResultVO.success("头像上传成功", avatarUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.fail("头像上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 修改用户密码
     * @param passwordRequest 密码修改请求
     * @param userId 用户ID
     * @return 修改结果
     */
    @PutMapping("/password")
    public ResultVO<Boolean> changePassword(
            @RequestBody Map<String, String> passwordRequest,
            @RequestAttribute("userId") Long userId) {
        
        String oldPassword = passwordRequest.get("oldPassword");
        String newPassword = passwordRequest.get("newPassword");
        
        if (oldPassword == null || newPassword == null) {
            return ResultVO.fail("旧密码和新密码不能为空");
        }
        
        // 验证新密码长度
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            return ResultVO.fail("新密码长度应在6-20个字符之间");
        }
        
        try {
            boolean result = userService.changePassword(userId, oldPassword, newPassword);
            if (result) {
                return ResultVO.success("密码修改成功", true);
            } else {
                return ResultVO.fail("密码修改失败");
            }
        } catch (Exception e) {
            return ResultVO.fail(e.getMessage());
        }
    }
} 