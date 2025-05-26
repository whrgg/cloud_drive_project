<template>
  <div class="share-access">
    <div class="share-header">
      <div class="logo">
        <router-link to="/">
          <img src="../../assets/logo.png" alt="Cloud Drive" />
          <span class="logo-text">云盘</span>
        </router-link>
      </div>
      <div class="user-actions" v-if="isAuthenticated">
        <el-button type="primary" @click="backToMyDrive">返回我的云盘</el-button>
      </div>
      <div class="user-actions" v-else>
        <el-button @click="goToLogin">登录</el-button>
        <el-button type="primary" @click="goToRegister">注册</el-button>
      </div>
    </div>

    <!-- 需要验证提取码 -->
    <div class="share-verify" v-if="!isVerified && shareExists">
      <div class="verify-container">
        <h2>访问分享</h2>
        <div v-if="shareBasicInfo" class="share-basic-info">
          <div class="share-icon">
            <el-icon :size="48">
              <FolderOpened v-if="shareBasicInfo.isDir" />
              <Document v-else />
            </el-icon>
          </div>
          <div class="share-name">{{ shareBasicInfo.fileName }}</div>
          <div class="share-meta">
            由 {{ shareBasicInfo.username }} 分享
          </div>
        </div>

        <div v-if="needCode" class="verify-form">
          <el-alert
            type="info"
            :closable="false"
            show-icon
          >
            此分享需要提取码才能访问
          </el-alert>
          
          <div class="code-input">
            <el-input 
              v-model="verifyCode" 
              placeholder="请输入提取码"
              clearable
              maxlength="6"
              @keyup.enter="verifyShare"
            />
            <el-button type="primary" @click="verifyShare" :loading="verifyLoading">
              确认
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分享内容 -->
    <div class="share-content" v-else-if="isVerified">
      <div class="share-info">
        <div class="share-owner">
          <div class="avatar">{{ shareInfo?.username?.substring(0, 1).toUpperCase() }}</div>
          <div class="owner-info">
            <div class="owner-name">{{ shareInfo?.username }}</div>
            <div class="share-time">分享于 {{ formatDate(shareInfo?.createTime) }}</div>
          </div>
        </div>
      
        <div class="share-title">
          <h2>{{ shareInfo?.fileName }}</h2>
          <div class="share-meta">
            {{ shareInfo?.isDir ? `文件夹（${shareFileList.length}个项目）` : formatSize(shareInfo?.fileSize) }}
            <span v-if="shareInfo?.expireTime">，有效期至 {{ formatDate(shareInfo?.expireTime) }}</span>
          </div>
          <div class="share-description" v-if="shareInfo?.description">
            {{ shareInfo?.description }}
          </div>
        </div>
      </div>

      <!-- 保存到我的云盘 -->
      <div class="share-actions">
        <el-button type="primary" @click="saveToMyDrive" :disabled="!isAuthenticated">
          保存到我的云盘
        </el-button>
        
        <el-button @click="downloadShare" v-if="!shareInfo?.isDir">
          <el-icon><Download /></el-icon>下载
        </el-button>
      </div>

      <!-- 文件内容预览 -->
      <div class="share-preview">
        <!-- 如果是文件夹，显示文件列表 -->
        <div v-if="shareInfo?.isDir" class="folder-content">
          <div class="folder-header">
            <div class="breadcrumb">
              <el-breadcrumb separator="/">
                <el-breadcrumb-item :to="`/s/${shareId}`">根目录</el-breadcrumb-item>
                <el-breadcrumb-item 
                  v-for="(item, index) in breadcrumbList" 
                  :key="index"
                  :to="`/s/${shareId}/${item.id}`"
                >
                  {{ item.name }}
                </el-breadcrumb-item>
              </el-breadcrumb>
            </div>
          </div>

          <!-- 文件列表 -->
          <el-table :data="shareFileList" style="width: 100%">
            <el-table-column prop="name" label="文件名" min-width="220">
              <template #default="{ row }">
                <div class="file-item" @click="handleFileClick(row)">
                  <el-icon v-if="row.isDir"><Folder /></el-icon>
                  <el-icon v-else-if="row.fileType === 'image'"><Picture /></el-icon>
                  <el-icon v-else-if="row.fileType === 'video'"><VideoPlay /></el-icon>
                  <el-icon v-else-if="row.fileType === 'audio'"><Headset /></el-icon>
                  <el-icon v-else><Document /></el-icon>
                  <span class="file-name">{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="updateTime" label="修改时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.updateTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="size" label="大小" width="150">
              <template #default="{ row }">
                {{ row.isDir ? '-' : formatSize(row.size) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button-group>
                  <el-button 
                    size="small" 
                    type="primary" 
                    text 
                    @click.stop="downloadShareFile(row)"
                    v-if="!row.isDir"
                  >
                    <el-icon><Download /></el-icon>
                  </el-button>
                  <el-button 
                    size="small" 
                    type="primary" 
                    text 
                    @click.stop="previewFile(row)"
                    v-if="canPreview(row)"
                  >
                    <el-icon><View /></el-icon>
                  </el-button>
                </el-button-group>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <!-- 如果是图片文件，直接预览 -->
        <div v-else-if="!shareInfo?.isDir && canPreviewDirectly(shareInfo)" class="file-preview">
          <advanced-preview 
            :file="shareFileToPreviewFile(shareInfo)"
            :preview-url="previewUrl"
            :loading="loading"
          />
        </div>
        
        <!-- 其他文件类型，显示文件信息 -->
        <div v-else-if="!shareInfo?.isDir" class="file-info-card">
          <div class="file-type-icon">
            <el-icon :size="64"><Document /></el-icon>
          </div>
          <div class="file-details">
            <h3>{{ shareInfo?.fileName }}</h3>
            <p>大小：{{ formatSize(shareInfo?.fileSize) }}</p>
            <p>类型：{{ shareInfo?.fileType }}</p>
            <el-button type="primary" @click="downloadShare">下载文件</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分享不存在、已取消或已过期 -->
    <div class="share-error" v-else>
      <el-empty 
        description="抱歉，该分享链接不存在、已取消或已过期" 
        :image-size="200"
      >
        <template #image>
          <el-icon :size="128"><Warning /></el-icon>
        </template>
        <el-button type="primary" @click="goToHome">返回首页</el-button>
      </el-empty>
    </div>

    <!-- 保存对话框 -->
    <el-dialog v-model="saveDialogVisible" title="保存到我的云盘" width="500px">
      <div class="save-dialog-content">
        <p>确认将文件保存到您的云盘？</p>
        <!-- TODO：添加文件夹选择功能 -->
      </div>
      <template #footer>
        <span>
          <el-button @click="saveDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveToMyDrive" :loading="savingToMyDrive">
            确认保存
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 文件预览对话框 -->
    <el-dialog v-model="previewDialogVisible" title="文件预览" width="80%" top="5vh">
      <div class="preview-dialog-content">
        <advanced-preview 
          v-if="currentPreviewFile"
          :file="currentPreviewFile"
          :preview-url="previewFileUrl"
          :loading="previewLoading"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { 
  getShareInfo, 
  verifyShareCode, 
  getShareFileList, 
  saveShareFiles, 
  increaseShareViews, 
  getShareFileDownloadUrl,
  increaseShareDownloads
} from '../../api/share';
import type { ShareInfo, ShareFileInfo, ShareBreadcrumb } from '../../types/share';
import AdvancedPreview from '../../components/preview/AdvancedPreview.vue';

const route = useRoute();
const router = useRouter();

// 状态
const loading = ref(false);
const shareExists = ref(true);
const isVerified = ref(false);
const needCode = ref(false);
const verifyCode = ref('');
const verifyLoading = ref(false);
const saveDialogVisible = ref(false);
const savingToMyDrive = ref(false);
const previewDialogVisible = ref(false);
const previewFileUrl = ref('');
const previewFileName = ref('');
const previewFileType = ref('');
const previewLoading = ref(false);
const currentPreviewFile = ref<any>(null);

// 数据
const shareId = computed(() => route.params.shareId as string);
const shareInfo = ref<ShareInfo | null>(null);
const shareBasicInfo = ref<{
  fileName: string;
  isDir: boolean;
  username: string;
} | null>(null);
const shareFileList = ref<ShareFileInfo[]>([]);
const currentFolderId = ref(0);
const breadcrumbList = ref<ShareBreadcrumb[]>([]);
const previewUrl = ref('');

// 用户是否已登录
const isAuthenticated = computed(() => {
  return !!localStorage.getItem('token');
});

// 初始化
onMounted(async () => {
  if (!shareId.value) {
    shareExists.value = false;
    return;
  }

  try {
    // 获取基本分享信息
    const basicInfo = await getShareInfo(shareId.value);
    
    if (!basicInfo) {
      shareExists.value = false;
      return;
    }
    
    shareBasicInfo.value = {
      fileName: basicInfo.fileName,
      isDir: basicInfo.isDir,
      username: basicInfo.username,
    };
    
    // 检查是否需要提取码
    if (basicInfo.code) {
      needCode.value = true;
    } else {
      // 不需要提取码，直接获取详情
      await getShareDetails();
    }
    
    // 更新分享浏览次数
    await increaseShareViews(shareId.value);
  } catch (error) {
    console.error('Failed to get share info', error);
    shareExists.value = false;
    ElMessage.error('获取分享信息失败');
  }
});

// 验证分享提取码
const verifyShare = async () => {
  if (!verifyCode.value) {
    ElMessage.warning('请输入提取码');
    return;
  }
  
  verifyLoading.value = true;
  
  try {
    const result = await verifyShareCode(shareId.value, verifyCode.value);
    
    if (result.verified) {
      isVerified.value = true;
      // 获取详细信息
      await getShareDetails();
    } else {
      ElMessage.error('提取码错误');
    }
  } catch (error) {
    console.error('Failed to verify share code', error);
    ElMessage.error('验证提取码失败');
  } finally {
    verifyLoading.value = false;
  }
};

// 获取分享详情
const getShareDetails = async () => {
  loading.value = true;
  
  try {
    // 获取分享信息
    const info = await getShareInfo(shareId.value, verifyCode.value || undefined);
    console.log('Share info received:', info); // 添加调试日志
    shareInfo.value = info;
    
    // 如果已验证，获取文件列表（如果是文件夹）
    if (info.isDir) {
      loadShareFiles();
    } else {
      // 如果是文件，获取预览URL
      // TODO: 根据文件类型确定是否可以预览，这里先假设都可以
      previewUrl.value = `api/share/preview/${shareId.value}/${info.userFileId}`;
      if (verifyCode.value) {
        previewUrl.value += `?code=${verifyCode.value}`;
      }
    }
    
    isVerified.value = true;
  } catch (error) {
    console.error('Failed to get share details', error);
    ElMessage.error('获取分享详情失败');
  } finally {
    loading.value = false;
  }
};

// 加载分享文件列表
const loadShareFiles = async () => {
  loading.value = true;
  
  try {
    const result = await getShareFileList(shareId.value, currentFolderId.value);
    shareFileList.value = result.list || [];
    breadcrumbList.value = result.path || [];
  } catch (error) {
    console.error('Failed to load share files', error);
    ElMessage.error('获取文件列表失败');
  } finally {
    loading.value = false;
  }
};

// 处理文件点击
const handleFileClick = (file: ShareFileInfo) => {
  if (file.isDir) {
    // 进入文件夹
    currentFolderId.value = file.id;
    loadShareFiles();
  } else if (canPreview(file)) {
    // 预览文件
    previewFile(file);
  } else {
    // 下载文件
    downloadShareFile(file);
  }
};

// 判断文件是否可预览
const canPreview = (file: ShareFileInfo) => {
  if (file.isDir) return false;
  
  // 检查文件类型和大小限制
  if (file.fileType === 'image') {
    // 图片文件小于50MB可预览
    return file.size < 50 * 1024 * 1024;
  } else if (file.fileType === 'video') {
    // 视频文件小于200MB可预览
    return file.size < 200 * 1024 * 1024;
  } else if (file.fileType === 'audio') {
    return true;
  }
  
  return false;
};

// 预览文件
const previewFile = (file: ShareFileInfo) => {
  previewFileName.value = file.name;
  previewFileType.value = file.fileType || '';
  previewLoading.value = true;
  
  // 构建预览URL
  previewFileUrl.value = `api/share/preview/${shareId.value}/${file.id}`;
  if (verifyCode.value) {
    previewFileUrl.value += `?code=${verifyCode.value}`;
  }
  
  // 创建文件对象
  currentPreviewFile.value = {
    id: file.id,
    fileName: file.name,
    fileType: file.fileType,
    size: file.size,
    type: file.type
  };
  
  previewDialogVisible.value = true;
  previewLoading.value = false;
};

// 下载单个分享文件
const downloadShareFile = async (file: ShareFileInfo) => {
  if (file.isDir) {
    ElMessage.warning('不能直接下载文件夹');
    return;
  }
  
  try {
    console.log('下载分享文件，shareId:', shareId.value, 'fileId:', file.id);
    
    const url = await getShareFileDownloadUrl(shareId.value, file.id);
    const a = document.createElement('a');
    a.href = url;
    
    // 处理文件名中可能包含的@2o.jpg等后缀
    let fileName = file.name;
    if (fileName.includes('@')) {
      // 获取文件扩展名
      const lastDotIndex = fileName.lastIndexOf('.');
      const extension = lastDotIndex > 0 ? fileName.substring(lastDotIndex) : '';
      
      // 截取@之前的部分
      fileName = fileName.substring(0, fileName.indexOf('@'));
      
      // 如果截取后的文件名不包含扩展名，则添加扩展名
      if (extension && !fileName.toLowerCase().endsWith(extension.toLowerCase())) {
        fileName += extension;
      }
    }
    a.download = fileName;
    
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url); // 释放URL对象
    
    // 增加下载次数统计
    try {
      await increaseShareDownloads(shareId.value);
    } catch (err) {
      console.warn('Failed to increase download count', err);
    }
  } catch (error) {
    console.error('Failed to download file', error);
    ElMessage.error('下载失败');
  }
};

// 下载整个分享
const downloadShare = async () => {
  if (!shareInfo.value) return;
  
  if (shareInfo.value.isDir) {
    ElMessage.warning('暂不支持下载整个文件夹');
    return;
  }
  
  try {
    // 检查userFileId是否存在
    if (!shareInfo.value.userFileId) {
      console.error('User File ID is undefined', shareInfo.value);
      ElMessage.error('文件ID不存在，无法下载');
      return;
    }
    
    // 使用shareCode字段作为分享码
    const shareCode = shareInfo.value.shareCode || shareId.value;
    const userFileId = shareInfo.value.userFileId;
    
    console.log('下载分享文件，shareCode:', shareCode, 'userFileId:', userFileId);
    
    const url = await getShareFileDownloadUrl(shareCode, userFileId);
    const a = document.createElement('a');
    a.href = url;
    
    // 处理文件名中可能包含的@2o.jpg等后缀
    let fileName = shareInfo.value.fileName;
    if (fileName.includes('@')) {
      // 获取文件扩展名
      const lastDotIndex = fileName.lastIndexOf('.');
      const extension = lastDotIndex > 0 ? fileName.substring(lastDotIndex) : '';
      
      // 截取@之前的部分
      fileName = fileName.substring(0, fileName.indexOf('@'));
      
      // 如果截取后的文件名不包含扩展名，则添加扩展名
      if (extension && !fileName.toLowerCase().endsWith(extension.toLowerCase())) {
        fileName += extension;
      }
    }
    a.download = fileName;
    
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url); // 释放URL对象
    
    // 增加下载次数统计
    try {
      await increaseShareDownloads(shareCode);
    } catch (err) {
      console.warn('Failed to increase download count', err);
    }
  } catch (error) {
    console.error('Failed to download file', error);
    ElMessage.error('下载失败');
  }
};

// 保存到我的云盘
const saveToMyDrive = () => {
  if (!isAuthenticated.value) {
    ElMessage.warning('请先登录');
    return;
  }
  
  saveDialogVisible.value = true;
};

// 处理保存到我的云盘
const handleSaveToMyDrive = async () => {
  if (!shareInfo.value) return;
  
  savingToMyDrive.value = true;
  try {
    // 检查userFileId是否存在
    if (!shareInfo.value.userFileId) {
      console.error('User File ID is undefined', shareInfo.value);
      ElMessage.error('文件ID不存在，无法保存');
      return;
    }
    
    await saveShareFiles(shareId.value, [shareInfo.value.userFileId], 0); // 保存到根目录
    ElMessage.success('文件已成功保存到您的云盘');
    saveDialogVisible.value = false;
  } catch (error) {
    console.error('Failed to save to my drive', error);
    ElMessage.error('保存失败');
  } finally {
    savingToMyDrive.value = false;
  }
};

// 返回我的云盘
const backToMyDrive = () => {
  router.push('/');
};

// 前往登录页
const goToLogin = () => {
  router.push('/login');
};

// 前往注册页
const goToRegister = () => {
  router.push('/register');
};

// 返回首页
const goToHome = () => {
  router.push('/');
};

// 格式化日期
const formatDate = (date: string | undefined) => {
  if (!date) return '-';
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

// 格式化文件大小
const formatSize = (bytes: number | undefined) => {
  if (!bytes) return '-';
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
};

// 判断文件是否可以直接预览
const canPreviewDirectly = (file: ShareInfo | null) => {
  if (!file) return false;
  if (file.isDir) return false;
  
  // 检查文件类型和大小限制
  if (file.fileType === 'image') {
    // 图片文件小于50MB可预览
    return file.fileSize < 50 * 1024 * 1024;
  } else if (file.fileType === 'video') {
    // 视频文件小于200MB可预览
    return file.fileSize < 200 * 1024 * 1024;
  } else if (file.fileType === 'audio') {
    return true;
  }
  
  return false;
};

// 将文件转换为可以预览的文件对象
const shareFileToPreviewFile = (file: ShareInfo | null) => {
  if (!file) return null;
  return {
    id: file.userFileId,
    fileName: file.fileName,
    fileType: file.fileType,
    size: file.fileSize,
    type: file.type
  };
};
</script>

<style scoped>
.share-access {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.share-header {
  padding: 16px;
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
}

.logo a {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: #303133;
}

.logo img {
  height: 32px;
  margin-right: 10px;
}

.logo-text {
  font-size: 18px;
  font-weight: bold;
}

.user-actions {
  display: flex;
  gap: 10px;
}

.share-verify {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
}

.verify-container {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 30px;
  width: 100%;
  max-width: 500px;
  text-align: center;
}

.share-basic-info {
  margin: 20px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.share-icon {
  margin-bottom: 16px;
  color: #409EFF;
}

.share-name {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 8px;
}

.share-meta {
  color: #909399;
  font-size: 14px;
}

.verify-form {
  margin-top: 20px;
}

.code-input {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

.share-content {
  flex: 1;
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.share-info {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 24px;
  margin-bottom: 24px;
}

.share-owner {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #409EFF;
  color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 18px;
  margin-right: 12px;
}

.owner-info {
  display: flex;
  flex-direction: column;
}

.owner-name {
  font-weight: bold;
  font-size: 16px;
}

.share-time {
  color: #909399;
  font-size: 12px;
}

.share-title h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
}

.share-description {
  margin-top: 12px;
  color: #606266;
  padding: 8px 0;
  border-top: 1px solid #ebeef5;
}

.share-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.share-preview {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 24px;
}

.folder-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  padding: 20px;
}

.audio-placeholder {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #606266;
}

.file-info-card {
  display: flex;
  align-items: center;
  padding: 24px;
  gap: 24px;
}

.file-type-icon {
  color: #409EFF;
}

.file-details {
  flex: 1;
}

.file-details h3 {
  margin: 0 0 10px 0;
}

.file-details p {
  margin: 5px 0;
  color: #606266;
}

.share-error {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
  color: #f56c6c;
}

.preview-dialog-content {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 70vh;
  overflow: hidden;
}
</style> 