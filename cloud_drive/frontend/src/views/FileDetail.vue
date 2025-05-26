<template>
  <div class="file-detail-container">
    <div class="file-detail-header">
      <!-- 返回按钮 -->
      <el-button @click="goBack" icon="ArrowLeft">返回</el-button>
      
      <!-- 文件基本信息 -->
      <div class="file-info">
        <h2>{{ fileInfo?.fileName || '文件详情' }}</h2>
        <div class="file-meta">
          <span v-if="fileInfo">
            {{ formatSize(fileInfo.fileSize) }} | 
            {{ formatDate(fileInfo.updateTime) }}
          </span>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="file-actions">
        <el-button type="primary" @click="handleDownload">
          <el-icon><Download /></el-icon>下载
        </el-button>
        <el-button @click="handleShare">
          <el-icon><Share /></el-icon>分享
        </el-button>
        <el-dropdown trigger="click" @command="handleCommand">
          <el-button>
            更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="rename">重命名</el-dropdown-item>
              <el-dropdown-item command="move">移动</el-dropdown-item>
              <el-dropdown-item command="star" :icon="fileInfo?.isStarred ? 'Star' : 'StarFilled'">
                {{ fileInfo?.isStarred ? '取消收藏' : '收藏' }}
              </el-dropdown-item>
              <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 文件预览区域 -->
    <div class="file-preview">
      <div v-if="loading" class="loading-container">
        <el-skeleton animated :rows="15" />
      </div>
      <div v-else-if="!fileInfo" class="empty-container">
        <el-empty description="文件不存在或已被删除" />
      </div>
      <!-- 使用高级预览组件 -->
      <advanced-preview 
        v-else 
        :file="fileInfo" 
        :preview-url="previewUrl" 
        :loading="previewLoading"
      />
    </div>

    <!-- 文件详情区域 -->
    <div class="file-detail-info">
      <h3>文件详情</h3>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="文件名称">{{ fileInfo?.fileName }}</el-descriptions-item>
        <el-descriptions-item label="文件类型">{{ fileInfo?.type }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ fileInfo ? formatSize(fileInfo.fileSize) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ fileInfo ? formatDate(fileInfo.createTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="修改时间">{{ fileInfo ? formatDate(fileInfo.updateTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="下载次数">{{ fileInfo?.downloadCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="文件路径">{{ filePath }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 重命名对话框 -->
    <el-dialog v-model="renameDialogVisible" title="重命名" width="30%">
      <el-form :model="renameForm" :rules="renameRules" ref="renameFormRef">
        <el-form-item prop="name" label="新名称">
          <el-input v-model="renameForm.name" placeholder="请输入新名称"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="renameDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleRenameConfirm">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 分享对话框 -->
    <share-dialog
      v-model:visible="shareDialogVisible"
      :file-id="fileInfo?.id || 0"
      :file-info="fileInfo"
      @share-created="handleShareCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance } from 'element-plus';
import { getFileDetail, getFilePreviewUrl, renameFile, deleteFile, getFileDownloadUrl, toggleStarFile } from '../api/file';
import type { FileInfo } from '../types/file';
import ShareDialog from '../components/share/ShareDialog.vue';
import AdvancedPreview from '../components/preview/AdvancedPreview.vue';

const route = useRoute();
const router = useRouter();
const loading = ref(true);
const previewLoading = ref(true);
const fileInfo = ref<FileInfo | null>(null);
const previewUrl = ref('');
const textContent = ref('');
const renameDialogVisible = ref(false);
const renameFormRef = ref<FormInstance>();
const renameForm = ref({
  name: '',
  id: 0,
});

// 分享对话框
const shareDialogVisible = ref(false);

// 重命名规则
const renameRules = {
  name: [
    { required: true, message: '请输入新名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' },
  ],
};

// 文件路径
const filePath = computed(() => {
  if (!fileInfo.value || !fileInfo.value.filePath) return '-';
  return fileInfo.value.filePath;
});

// 获取文件详情
const loadFileDetail = async () => {
  loading.value = true;
  previewLoading.value = true;
  try {
    const fileId = Number(route.params.fileId);
    if (isNaN(fileId)) {
      ElMessage.error('无效的文件ID');
      router.push('/');
      return;
    }

    const detail = await getFileDetail(fileId);
    if (!detail) {
      ElMessage.error('文件不存在或已被删除');
      loading.value = false;
      previewLoading.value = false;
      return;
    }
    
    fileInfo.value = detail;

    // 如果是文件夹，返回文件列表页
    if (detail.isDir) {
      router.replace(`/folder/${fileId}`);
      return;
    }

    // 获取预览URL
    try {
      const url = await getFilePreviewUrl(fileId);
      previewUrl.value = url as unknown as string;
    } catch (error) {
      console.error('Failed to get preview URL', error);
      ElMessage.warning('获取预览链接失败，可能不支持此文件类型的预览');
    } finally {
      loading.value = false;
      previewLoading.value = false;
    }
    
    // 更新文件访问次数
    updateFileViewCount(fileId);
  } catch (error) {
    console.error('Failed to load file detail', error);
    ElMessage.error('获取文件详情失败');
    loading.value = false;
    previewLoading.value = false;
  }
};

// 更新文件访问次数
const updateFileViewCount = async (fileId: number) => {
  try {
    // 这里假设后端有一个更新访问次数的API
    // await updateFileView(fileId);
    console.log('File view count updated for', fileId);
  } catch (error) {
    console.error('Failed to update file view count', error);
  }
};

// 处理下载
const handleDownload = async () => {
  if (!fileInfo.value) return;

  try {
    const url = await getFileDownloadUrl(fileInfo.value.id);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileInfo.value.fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url); // 释放URL对象
  } catch (error) {
    console.error('Failed to download file', error);
    ElMessage.error('下载文件失败');
  }
};

// 处理分享
const handleShare = () => {
  if (!fileInfo.value) return;
  shareDialogVisible.value = true;
};

// 处理分享创建成功
const handleShareCreated = (shareResult: any) => {
  ElMessage.success(`分享创建成功，分享ID: ${shareResult.id}`);
};

// 下拉菜单命令处理
const handleCommand = (command: string) => {
  if (!fileInfo.value) return;

  switch (command) {
    case 'rename':
      openRenameDialog();
      break;
    case 'move':
      // 移动功能将在后续实现
      ElMessage.info('移动功能即将上线');
      break;
    case 'star':
      toggleStar();
      break;
    case 'delete':
      confirmDelete();
      break;
    default:
      break;
  }
};

// 打开重命名对话框
const openRenameDialog = () => {
  if (!fileInfo.value) return;
  renameForm.value = {
    name: fileInfo.value.fileName,
    id: fileInfo.value.id,
  };
  renameDialogVisible.value = true;
};

// 确认重命名
const handleRenameConfirm = () => {
  renameFormRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        await renameFile(renameForm.value.id, renameForm.value.name);
        ElMessage.success('重命名成功');
        renameDialogVisible.value = false;
        // 重新加载文件详情
        loadFileDetail();
      } catch (error) {
        console.error('Failed to rename file', error);
        ElMessage.error('重命名失败');
      }
    }
  });
};

// 确认删除
const confirmDelete = () => {
  if (!fileInfo.value) return;

  ElMessageBox.confirm(
    `确定要删除文件 "${fileInfo.value.fileName}" 吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await deleteFile([fileInfo.value!.id]);
      ElMessage.success('删除成功');
      router.push('/');
    } catch (error) {
      console.error('Failed to delete file', error);
      ElMessage.error('删除失败');
    }
  });
};

// 返回上一页
const goBack = () => {
  router.back();
};

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

// 格式化文件大小
const formatSize = (bytes: number) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
};

// 收藏/取消收藏文件
const toggleStar = async () => {
  if (!fileInfo.value) return;
  
  try {
    const isStar = !(fileInfo.value.isStarred || false);
    await toggleStarFile(fileInfo.value.id, isStar);
    
    // 更新本地文件状态
    fileInfo.value.isStarred = isStar;
    
    ElMessage.success(isStar ? '收藏成功' : '已取消收藏');
  } catch (error) {
    console.error('Failed to toggle star', error);
    ElMessage.error(fileInfo.value.isStarred ? '取消收藏失败' : '收藏失败');
  }
};

// 加载初始数据
onMounted(() => {
  loadFileDetail();
});

// 组件卸载前清理资源
onBeforeUnmount(() => {
  // 清除引用，防止内存泄漏
  fileInfo.value = null;
  previewUrl.value = '';
  textContent.value = '';
});
</script>

<style scoped>
.file-detail-container {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  height: auto;
  gap: 20px;
  padding-bottom: 20px;
}

.file-detail-header {
  display: flex;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.file-info {
  flex: 1;
  margin-left: 16px;
}

.file-info h2 {
  margin: 0;
  font-size: 18px;
}

.file-meta {
  color: #909399;
  font-size: 14px;
  margin-top: 4px;
}

.file-actions {
  display: flex;
  gap: 8px;
}

.file-preview {
  flex: auto;
  overflow: hidden;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f6f8fa;
  min-height: 400px;
  height: auto;
  position: relative;
}

.loading-container, .empty-container {
  width: 100%;
  height: 100%;
  min-height: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style> 