<template>
  <div class="recycle-container">
    <div class="page-header">
      <div class="header-left">
      <h2>回收站</h2>
        <!-- 面包屑导航 -->
        <div class="breadcrumb-container" v-if="currentFolderId !== 0">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item @click="goToRoot">回收站</el-breadcrumb-item>
            <el-breadcrumb-item 
              v-for="(item, index) in breadcrumbList" 
              :key="index" 
              @click="navigateToFolder(item.id)"
            >
              {{ item.name }}
            </el-breadcrumb-item>
          </el-breadcrumb>
          
          <!-- 返回上一级按钮 -->
          <el-button 
            v-if="currentFolderId !== 0" 
            size="small" 
            icon="ArrowLeft" 
            @click="goToParentFolder"
            class="back-button"
          >
            返回上一级
          </el-button>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="danger" @click="handleEmptyBin" :disabled="fileList.length === 0">
          清空回收站
        </el-button>
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button label="list">
            <el-icon><List /></el-icon>
          </el-radio-button>
          <el-radio-button label="grid">
            <el-icon><Grid /></el-icon>
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 文件列表 -->
    <div class="file-list-container">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 空状态 -->
      <el-empty v-else-if="fileList.length === 0" description="回收站为空" />

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
              <el-icon v-else><Document /></el-icon>
              <span class="file-name">{{ row.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="删除时间" width="180">
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
              <el-button size="small" type="primary" text @click.stop="handleRestore(row)">
                <el-icon><RefreshRight /></el-icon>恢复
              </el-button>
              <el-button size="small" type="danger" text @click.stop="handleDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
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
            <el-icon v-else><Document /></el-icon>
          </div>
          <div class="file-info">
            <div class="file-name" :title="file.fileName">{{ file.fileName }}</div>
            <div class="file-meta">{{ formatDate(file.updateTime) }}</div>
          </div>
          <div class="file-actions">
            <el-button size="small" type="primary" @click.stop="handleRestore(file)">恢复</el-button>
            <el-button size="small" type="danger" @click.stop="handleDelete(file)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 清空回收站确认对话框 -->
    <el-dialog
      v-model="emptyBinDialogVisible"
      title="确认清空"
      width="30%"
    >
      <span>确定要清空回收站吗？此操作不可逆，将永久删除所有项目。</span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="emptyBinDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmEmptyBin">确定清空</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getRecycleBinList, getRecycleBinFolderContents, restoreFile, removeFile, clearRecycleBin } from '../api/file';
import type { FileInfo, Breadcrumb } from '../types/file';

const loading = ref(true);
const fileList = ref<FileInfo[]>([]);
const viewMode = ref('list');
const emptyBinDialogVisible = ref(false);
const currentFolderId = ref(0);
const breadcrumbList = ref<Breadcrumb[]>([]);

// 加载回收站文件列表
const loadRecycleBinFiles = async () => {
  loading.value = true;
  try {
    if (currentFolderId.value === 0) {
      // 加载回收站根目录
      const res: any = await getRecycleBinList({});
      console.log('回收站数据:', res); 
      console.log('回收站数据类型:', typeof res);
      
      // 尝试多种可能的数据结构
      if (res && Array.isArray(res.list)) {
        fileList.value = res.list;
      } else if (res && res.data && Array.isArray(res.data.list)) {
        fileList.value = res.data.list;
      } else {
        console.warn('回收站数据格式不符合预期');
        fileList.value = [];
      }
      
      breadcrumbList.value = [];
    } else {
      // 加载回收站中特定文件夹的内容
      const res: any = await getRecycleBinFolderContents(currentFolderId.value, {});
      console.log('文件夹内容数据:', res);
      console.log('文件夹内容数据类型:', typeof res);
      
      // 尝试多种可能的数据结构
      if (res && Array.isArray(res.list)) {
        fileList.value = res.list;
        breadcrumbList.value = Array.isArray(res.path) ? res.path : [];
      } else if (res && res.data && Array.isArray(res.data.list)) {
        fileList.value = res.data.list;
        breadcrumbList.value = Array.isArray(res.data.path) ? res.data.path : [];
      } else {
        console.warn('文件夹内容数据格式不符合预期');
        fileList.value = [];
        breadcrumbList.value = [];
      }
    }
  } catch (error) {
    console.error('Failed to load recycle bin files', error);
    ElMessage.error('加载回收站文件失败');
    fileList.value = []; // 确保在错误情况下fileList是空数组
    breadcrumbList.value = [];
  } finally {
    loading.value = false;
  }
};

// 处理行点击事件
const handleRowClick = (row: FileInfo) => {
  if (row.isDir) {
    // 如果是文件夹，进入文件夹
    currentFolderId.value = row.id;
    loadRecycleBinFiles();
  }
};

// 返回回收站根目录
const goToRoot = () => {
  currentFolderId.value = 0;
  loadRecycleBinFiles();
};

// 导航到特定文件夹
const navigateToFolder = (folderId: number) => {
  currentFolderId.value = folderId;
  loadRecycleBinFiles();
};

// 返回上一级文件夹
const goToParentFolder = () => {
  // 如果有面包屑导航，则获取当前文件夹的父文件夹ID
  if (breadcrumbList.value.length > 0) {
    // 如果只有一级，则返回根目录
    if (breadcrumbList.value.length === 1) {
      goToRoot();
    } else {
      // 否则返回上一级文件夹
      const parentIndex = breadcrumbList.value.length - 2;
      const parentFolder = breadcrumbList.value[parentIndex];
      navigateToFolder(parentFolder.id);
    }
  } else {
    // 如果没有面包屑导航，则返回根目录
    goToRoot();
  }
};

// 恢复文件
const handleRestore = async (file: FileInfo) => {
  try {
    await restoreFile([file.id]);
    ElMessage.success('文件已恢复');
    loadRecycleBinFiles(); // 重新加载列表
  } catch (error) {
    console.error('Failed to restore file', error);
    ElMessage.error('恢复文件失败');
  }
};

// 删除文件
const handleDelete = (file: FileInfo) => {
  ElMessageBox.confirm(
    `确定要永久删除${file.isDir ? '文件夹' : '文件'} "${file.fileName}" 吗？此操作不可逆。`,
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await removeFile([file.id]);
      ElMessage.success('删除成功');
      loadRecycleBinFiles(); // 重新加载列表
    } catch (error) {
      console.error('Failed to delete file', error);
      ElMessage.error('删除文件失败');
    }
  });
};

// 清空回收站对话框
const handleEmptyBin = () => {
  if (fileList.value.length === 0) {
    ElMessage.info('回收站已经是空的');
    return;
  }
  emptyBinDialogVisible.value = true;
};

// 确认清空回收站
const confirmEmptyBin = async () => {
  try {
    await clearRecycleBin();
    ElMessage.success('回收站已清空');
    fileList.value = [];
    emptyBinDialogVisible.value = false;
  } catch (error) {
    console.error('Failed to empty recycle bin', error);
    ElMessage.error('清空回收站失败');
  }
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
  loadRecycleBinFiles();
});

// 添加onActivated钩子，当组件从缓存激活时也加载数据
onActivated(() => {
  loadRecycleBinFiles();
});
</script>

<style scoped>
.recycle-container {
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

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
}

.breadcrumb-container {
  display: flex;
  align-items: center;
  gap: 10px;
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
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  padding: 10px;
}

.file-grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  border-radius: 4px;
  background-color: #f5f7fa;
  cursor: pointer;
}

.file-icon {
  font-size: 40px;
  color: #909399;
  margin-bottom: 8px;
}

.file-info {
  width: 100%;
  text-align: center;
  margin-bottom: 12px;
}

.file-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.file-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.back-button {
  margin-left: 10px;
}
</style> 