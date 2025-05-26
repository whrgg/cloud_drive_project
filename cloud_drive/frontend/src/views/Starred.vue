<template>
  <div class="starred-container">
    <div class="page-header">
      <h2>收藏夹</h2>
      <div class="header-actions">
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button label="list">
            <el-icon><List /></el-icon>
          </el-radio-button>
          <el-radio-button label="grid">
            <el-icon><Grid /></el-icon>
          </el-radio-button>
        </el-radio-group>
        <el-dropdown @command="handleSort">
          <el-button>
            排序方式<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="name_asc">名称升序</el-dropdown-item>
              <el-dropdown-item command="name_desc">名称降序</el-dropdown-item>
              <el-dropdown-item command="time_asc">时间升序</el-dropdown-item>
              <el-dropdown-item command="time_desc">时间降序</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 文件列表 -->
    <div class="file-list-container">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 空状态 -->
      <el-empty v-else-if="fileList.length === 0" description="暂无收藏文件">
        <el-button type="primary" @click="goToFiles">浏览文件</el-button>
      </el-empty>

      <!-- 表格视图 -->
      <el-table 
        v-else-if="viewMode === 'list'"
        :data="fileList"
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column prop="fileName" label="文件名" min-width="220">
          <template #default="{ row }">
            <div class="file-item">
              <el-icon v-if="row.isDir"><Folder /></el-icon>
              <el-icon v-else-if="row.fileType === 'image'"><Picture /></el-icon>
              <el-icon v-else-if="row.fileType === 'video'"><VideoPlay /></el-icon>
              <el-icon v-else-if="row.fileType === 'audio'"><Headset /></el-icon>
              <el-icon v-else-if="row.fileType === 'document'"><Document /></el-icon>
              <el-icon v-else-if="row.fileType === 'archive'"><Box /></el-icon>
              <el-icon v-else><Document /></el-icon>
              <span class="file-name">{{ row.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="修改时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="150">
          <template #default="{ row }">
            {{ row.isDir ? '-' : formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button size="small" type="primary" text @click.stop="handleUnstar(row)">
                <el-icon><StarFilled /></el-icon>
              </el-button>
              <el-button size="small" type="primary" text v-if="!row.isDir" @click.stop="handleDownload(row)">
                <el-icon><Download /></el-icon>
              </el-button>
              <el-dropdown trigger="click">
                <el-button size="small" type="primary" text @click.stop>
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleViewDetail(row)">查看详情</el-dropdown-item>
                    <el-dropdown-item @click="handleShare(row)">分享</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 网格视图 -->
      <div v-else class="grid-container">
        <div 
          v-for="file in fileList" 
          :key="file.id" 
          class="file-grid-item"
          @click="handleRowClick(file)"
        >
          <div class="file-icon">
            <el-icon v-if="file.isDir"><FolderOpened /></el-icon>
            <el-icon v-else-if="file.fileType === 'image'"><Picture /></el-icon>
            <el-icon v-else-if="file.fileType === 'video'"><VideoPlay /></el-icon>
            <el-icon v-else-if="file.fileType === 'audio'"><Headset /></el-icon>
            <el-icon v-else-if="file.fileType === 'document'"><Document /></el-icon>
            <el-icon v-else-if="file.fileType === 'archive'"><Box /></el-icon>
            <el-icon v-else><Document /></el-icon>
          </div>
          <div class="file-info">
            <div class="file-name" :title="file.fileName">{{ file.fileName }}</div>
            <div class="file-meta">{{ formatDate(file.updateTime) }}</div>
          </div>
          <div class="file-actions-overlay">
            <el-button circle size="small" @click.stop="handleUnstar(file)">
              <el-icon><StarFilled /></el-icon>
            </el-button>
            <el-button circle size="small" v-if="!file.isDir" @click.stop="handleDownload(file)">
              <el-icon><Download /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { 
  Document, 
  Folder, 
  FolderOpened, 
  Download, 
  Star, 
  StarFilled,
  Picture, 
  VideoPlay, 
  Headset,
  Box,
  List,
  Grid,
  ArrowDown,
  MoreFilled
} from '@element-plus/icons-vue';
import { getStarredFiles, getFileDownloadUrl, toggleStarFile } from '../api/file';
import type { FileInfo } from '../types/file';

const router = useRouter();
const loading = ref(true);
const fileList = ref<FileInfo[]>([]);
const viewMode = ref('list');
const sortKey = ref('fileName');
const sortOrder = ref('asc');

// 加载收藏文件列表
const loadStarredFiles = async () => {
  loading.value = true;
  try {
    const res = await getStarredFiles({
      orderBy: sortKey.value,
      orderDirection: sortOrder.value as 'asc' | 'desc',
    }) as any;
    
    // 直接处理后端返回的结构
    let rawFileList: any[] = [];
    
    // 处理后端返回的结构：{path: Array, list: Array}
    if (res && typeof res === 'object' && res.list && Array.isArray(res.list)) {
      rawFileList = res.list;
    } 
    // 备用处理方式，以防结构不同
    else if (res && Array.isArray(res)) {
      rawFileList = res;
    } 
    else {
      rawFileList = [];
    }
    
    // 转换为FileInfo类型并过滤掉无效项
    fileList.value = rawFileList
      .filter(file => file && file.id) // 过滤掉null或没有id的项
      .map(normalizeFileObject);
    
  } catch (error) {
    ElMessage.error('加载收藏文件失败');
    fileList.value = []; // 确保在错误情况下fileList是空数组
  } finally {
    loading.value = false;
  }
};

// 规范化文件对象，确保符合FileInfo类型
const normalizeFileObject = (file: any): FileInfo => {
  // 确保所有必要的属性都存在
  const fileType = determineFileType(file.fileName || file.name || '');
  
  return {
    id: file.id || 0,
    fileName: file.fileName || file.name || '未命名文件',
    fileSize: typeof file.fileSize === 'number' ? file.fileSize : (file.size || 0),
    type: file.type || '', // MIME类型
    fileType: file.fileType || fileType, // 文件分类类型
    filePath: file.filePath || file.path || '',
    isDir: Boolean(file.isDir), // 转换为布尔值
    parentId: typeof file.parentId === 'number' ? file.parentId : 0,
    userId: file.userId || 0,
    createTime: file.createTime || new Date().toISOString(),
    updateTime: file.updateTime || new Date().toISOString(),
    status: typeof file.status === 'number' ? file.status : 0,
    isStarred: Boolean(file.isStarred), // 转换为布尔值
    isShared: Boolean(file.isShared), // 转换为布尔值
    downloadCount: typeof file.downloadCount === 'number' ? file.downloadCount : 0,
    viewCount: typeof file.viewCount === 'number' ? file.viewCount : 0,
    lastViewTime: file.lastViewTime || '',
    extension: file.extension || getFileExtension(file.fileName || file.name || ''),
    thumbnail: file.thumbnail || ''
  };
};

// 根据文件名确定文件类型
const determineFileType = (fileName: string): string => {
  const extension = getFileExtension(fileName).toLowerCase();
  
  // 图片类型
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'].includes(extension)) {
    return 'image';
  }
  
  // 视频类型
  if (['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv', 'webm'].includes(extension)) {
    return 'video';
  }
  
  // 音频类型
  if (['mp3', 'wav', 'ogg', 'flac', 'aac', 'm4a'].includes(extension)) {
    return 'audio';
  }
  
  // 文档类型
  if (['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'md'].includes(extension)) {
    return 'document';
  }
  
  // 压缩文件
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(extension)) {
    return 'archive';
  }
  
  // 默认类型
  return 'other';
};

// 获取文件扩展名
const getFileExtension = (fileName: string): string => {
  const lastDotIndex = fileName.lastIndexOf('.');
  if (lastDotIndex === -1) return '';
  return fileName.substring(lastDotIndex + 1);
};

// 处理排序
const handleSort = (command: string) => {
  const [key, order] = command.split('_');
  
  // 映射排序字段
  switch (key) {
    case 'name':
      sortKey.value = 'fileName';
      break;
    case 'time':
      sortKey.value = 'updateTime';
      break;
    case 'size':
      sortKey.value = 'fileSize';
      break;
    default:
      sortKey.value = 'fileName';
  }
  
  sortOrder.value = order;
  loadStarredFiles();
};

// 处理行点击
const handleRowClick = (row: FileInfo) => {
  if (row.isDir) {
    router.push(`/folder/${row.id}`);
  } else {
    router.push(`/detail/${row.id}`);
  }
};

// 取消收藏
const handleUnstar = async (file: FileInfo) => {
  try {
    await toggleStarFile(file.id, false);
    
    // 从列表中移除该文件
    fileList.value = fileList.value.filter(item => item.id !== file.id);
    
    ElMessage.success('已取消收藏');
  } catch (error) {
    ElMessage.error('取消收藏失败');
  }
};

// 下载文件
const handleDownload = async (file: FileInfo) => {
  if (file.isDir) {
    ElMessage.warning('文件夹不支持直接下载');
    return;
  }

  try {
    const url = await getFileDownloadUrl(file.id);
    const a = document.createElement('a');
    a.href = url as unknown as string;
    a.download = file.fileName;
    a.click();
  } catch (error) {
    ElMessage.error('下载文件失败');
  }
};

// 查看详情
const handleViewDetail = (file: FileInfo) => {
  router.push(`/detail/${file.id}`);
};

// 分享
const handleShare = (file: FileInfo) => {
  ElMessage.info('分享功能即将上线');
};

// 前往文件列表
const goToFiles = () => {
  router.push('/');
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

onMounted(() => {
  loadStarredFiles();
});

// 添加onActivated钩子，当组件从缓存激活时也加载数据
onActivated(() => {
  loadStarredFiles();
});
</script>

<style scoped>
.starred-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.file-list-container {
  flex: 1;
  overflow: auto;
}

.loading-container {
  padding: 20px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 20px;
  padding: 10px;
}

.file-grid-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.file-grid-item:hover {
  background-color: #f5f7fa;
}

.file-grid-item:hover .file-actions-overlay {
  opacity: 1;
}

.file-icon {
  font-size: 40px;
  color: #409EFF;
  margin-bottom: 8px;
}

.file-info {
  width: 100%;
  text-align: center;
}

.file-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.file-actions-overlay {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.3s;
}
</style> 