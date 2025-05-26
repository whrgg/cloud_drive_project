<template>
  <div class="home-container">
    <!-- 面包屑导航 -->
    <div class="breadcrumb-container">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">全部文件</el-breadcrumb-item>
        <el-breadcrumb-item 
          v-for="(item, index) in breadcrumbList" 
          :key="index" 
          :to="{ path: `/folder/${item.id}` }"
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

    <!-- 操作工具栏 -->
    <div class="toolbar">
      <div class="left-actions">
        <el-button type="primary" @click="openUploadDialog">
          <el-icon><Upload /></el-icon>上传
        </el-button>
        <el-button @click="openFolderDialog">
          <el-icon><FolderAdd /></el-icon>新建文件夹
        </el-button>
        <!-- 批量操作按钮，只在有选中项时显示 -->
        <el-button 
          v-if="selectedFiles.length > 0" 
          type="danger" 
          @click="batchDelete"
        >
          <el-icon><Delete /></el-icon>批量删除
        </el-button>
      </div>
      <div class="right-actions">
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
              <el-dropdown-item command="size_asc">大小升序</el-dropdown-item>
              <el-dropdown-item command="size_desc">大小降序</el-dropdown-item>
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
      <el-empty v-else-if="fileList.length === 0" description="暂无文件">
        <el-button type="primary" @click="openUploadDialog">上传文件</el-button>
      </el-empty>

      <!-- 表格视图 -->
      <el-table 
        v-else-if="viewMode === 'list'"
        :data="fileList"
        style="width: 100%"
        @row-click="handleRowClick"
        @row-contextmenu="handleContextMenu"
        @selection-change="handleSelectionChange"
      >
        <!-- 添加多选列 -->
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="fileName" label="文件名" min-width="220">
          <template #default="{ row }">
            <div class="file-item">
              <el-icon v-if="row.isDir"><Folder /></el-icon>
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
              <el-button size="small" type="primary" text @click.stop="handleFileAction(row, 'download')" v-if="!row.isDir">
                <el-icon><Download /></el-icon>
              </el-button>
              <el-button size="small" type="primary" text @click.stop="handleFileAction(row, 'share')">
                <el-icon><Share /></el-icon>
              </el-button>
              <el-dropdown trigger="click" @command="(command: string) => handleFileAction(row, command)">
                <el-button size="small" type="primary" text @click.stop>
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="rename">重命名</el-dropdown-item>
                    <el-dropdown-item command="move">移动</el-dropdown-item>
                    <el-dropdown-item command="star">{{ row.isStarred ? '取消收藏' : '收藏' }}</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
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
          :class="{ 'selected': selectedFiles.some(item => item.id === file.id) }"
          @click="handleGridItemClick(file, $event)"
          @contextmenu="(e) => handleContextMenu(file, e)"
        >
          <div class="file-selection-indicator" v-if="selectedFiles.some(item => item.id === file.id)">
            <el-icon><Check /></el-icon>
          </div>
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
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div v-show="contextMenuVisible" class="context-menu" :style="contextMenuStyle">
      <ul>
        <li v-if="!selectedFile.isDir" @click="handleContextMenuAction('download')">下载</li>
        <li @click="handleContextMenuAction('share')">分享</li>
        <li @click="handleContextMenuAction('rename')">重命名</li>
        <li @click="handleContextMenuAction('move')">移动</li>
        <li @click="handleContextMenuAction('star')">{{ selectedFile.isStarred ? '取消收藏' : '收藏' }}</li>
        <li @click="handleContextMenuAction('delete')">删除</li>
      </ul>
    </div>

    <!-- 新建文件夹对话框 -->
    <el-dialog v-model="folderDialogVisible" title="新建文件夹" width="30%">
      <el-form :model="folderForm" :rules="folderRules" ref="folderFormRef">
        <el-form-item prop="name" label="文件夹名称">
          <el-input v-model="folderForm.name" placeholder="请输入文件夹名称"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="folderDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleCreateFolder">确认</el-button>
        </span>
      </template>
    </el-dialog>

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
          <el-button type="primary" @click="handleRename">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 上传文件对话框 -->
    <advanced-upload-dialog
      v-model:visible="uploadDialogVisible"
      :folder-id="currentFolderId"
      @upload-success="handleUploadSuccess"
    />

    <!-- 分享对话框 -->
    <share-dialog
      v-model:visible="shareDialogVisible"
      :file-id="selectedFileId"
      :file-info="selectedFile"
      @share-created="handleShareCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onActivated, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance } from 'element-plus';
import { Delete, ArrowDown, Upload, FolderAdd, List, Grid, Document, Folder, 
         FolderOpened, Picture, VideoPlay, Headset, Download, Share, MoreFilled, Check } from '@element-plus/icons-vue';
import { getFileList, createFolder as createFolderApi, renameFile, deleteFile, getFileDownloadUrl, toggleStarFile, batchDeleteFiles } from '../api/file';
import type { FileInfo, Breadcrumb } from '../types/file';
import AdvancedUploadDialog from '../components/upload/AdvancedUploadDialog.vue';
import ShareDialog from '../components/share/ShareDialog.vue';

const route = useRoute();
const router = useRouter();

// 状态管理
const loading = ref(false);
const fileList = ref<FileInfo[]>([]);
const breadcrumbList = ref<Breadcrumb[]>([]);
const viewMode = ref('list'); // 视图模式：list 或 grid
const sortKey = ref('name'); // 排序字段
const sortOrder = ref('asc'); // 排序方向
// 选中的文件列表
const selectedFiles = ref<FileInfo[]>([]);

// 当前文件夹ID
const currentFolderId = computed(() => {
  return route.params.folderId ? Number(route.params.folderId) : 0;
});

// 上下文菜单
const contextMenuVisible = ref(false);
const contextMenuStyle = ref({
  top: '0px',
  left: '0px',
});
const selectedFile = ref<FileInfo>({} as FileInfo);

// 文件夹表单
const folderDialogVisible = ref(false);
const folderForm = reactive({
  name: '',
});
const folderRules = {
  name: [
    { required: true, message: '请输入文件夹名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' },
  ],
};
const folderFormRef = ref<FormInstance>();

// 重命名表单
const renameDialogVisible = ref(false);
const renameForm = reactive({
  name: '',
  id: 0,
});
const renameRules = {
  name: [
    { required: true, message: '请输入新名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' },
  ],
};
const renameFormRef = ref<FormInstance>();

// 上传对话框
const uploadDialogVisible = ref(false);

// 分享对话框
const shareDialogVisible = ref(false);
const selectedFileId = ref(0);

// 加载文件列表
const loadFileList = async () => {
  loading.value = true;
  try {
    const res = await getFileList({
      parentId: currentFolderId.value,
      orderBy: sortKey.value,
      orderDirection: sortOrder.value as 'asc' | 'desc',
    }) as any;

    console.log('文件列表加载结果:', res);  // 添加调试输出
    
    // 防御性编程，确保fileList始终是数组
    if (res && res.list) {
      fileList.value = res.list;
    } else {
      fileList.value = [];
      console.warn('文件列表为空或格式不正确:', res);
    }
    
    // 防御性编程，确保breadcrumbList始终是数组
    if (res && res.path) {
      breadcrumbList.value = res.path;
    } else {
      breadcrumbList.value = [];
      console.warn('路径列表为空或格式不正确:', res);
    }
  } catch (error) {
    console.error('Failed to load file list', error);
    ElMessage.error('加载文件列表失败');
    // 确保即使发生错误，fileList也是数组而不是undefined
    fileList.value = [];
    breadcrumbList.value = [];
  } finally {
    loading.value = false;
  }
};

// 监听路由参数变化
watch(() => route.params.folderId, () => {
  loadFileList();
});

// 排序处理
const handleSort = (command: string) => {
  const [key, order] = command.split('_');
  sortKey.value = key;
  sortOrder.value = order;
  loadFileList();
};

// 处理文件行点击
const handleRowClick = (row: FileInfo) => {
  // 如果是文件夹，进入文件夹
  if (row.isDir) {
    router.push(`/folder/${row.id}`);
  } else {
    // 如果是文件，预览文件
    router.push(`/detail/${row.id}`);
  }
};

// 处理右键菜单
const handleContextMenu = (row: FileInfo, event: MouseEvent) => {
  event.preventDefault();
  selectedFile.value = row;
  contextMenuStyle.value = {
    top: `${event.clientY}px`,
    left: `${event.clientX}px`,
  };
  contextMenuVisible.value = true;

  // 点击空白处关闭菜单
  const closeMenu = () => {
    contextMenuVisible.value = false;
    document.removeEventListener('click', closeMenu);
  };
  setTimeout(() => {
    document.addEventListener('click', closeMenu);
  }, 0);
};

// 处理右键菜单操作
const handleContextMenuAction = (action: string) => {
  handleFileAction(selectedFile.value, action);
  contextMenuVisible.value = false;
};

// 文件操作
const handleFileAction = (file: FileInfo, action: string) => {
  switch (action) {
    case 'download':
      downloadFile(file);
      break;
    case 'share':
      handleShare(file);
      break;
    case 'rename':
      openRenameDialog(file);
      break;
    case 'move':
      // 移动功能将在后续实现
      ElMessage.info('移动功能即将上线');
      break;
    case 'star':
      toggleStar(file);
      break;
    case 'delete':
      confirmDelete(file);
      break;
    default:
      break;
  }
};

// 下载文件
const downloadFile = async (file: FileInfo) => {
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
    console.error('Failed to download file', error);
    ElMessage.error('下载文件失败');
  }
};

// 处理上传成功
const handleUploadSuccess = () => {
  // 关闭对话框
  uploadDialogVisible.value = false;
  
  // 重新加载文件列表
  loadFileList();
  
  // 提示用户
  ElMessage.success('文件上传成功');
};

// 打开上传对话框
const openUploadDialog = () => {
  uploadDialogVisible.value = true;
};

// 打开新建文件夹对话框
const openFolderDialog = () => {
  folderForm.name = '';
  folderDialogVisible.value = true;
};

// 创建文件夹
const handleCreateFolder = async () => {
  if (!folderFormRef.value) return;
  
  try {
    // 表单验证
    const valid = await folderFormRef.value.validate();
    if (!valid) return;
    
    try {
      const response = await createFolderApi(currentFolderId.value, folderForm.name) as any;
      ElMessage.success('创建文件夹成功');
      folderDialogVisible.value = false;
      
      // 检查返回的数据格式，如果是预期的格式，直接更新fileList
      if (response && response.list && Array.isArray(response.list)) {
        // 将新创建的文件夹添加到当前文件列表
        const newFolder = response.list[0];
        if (newFolder) {
          const exists = fileList.value.some(file => file.id === newFolder.id);
          if (!exists) {
            fileList.value.push(newFolder);
          }
        }
      } else {
        // 如果返回格式不符合预期，重新加载整个文件列表
        loadFileList();
      }
    } catch (error) {
      ElMessage.error('创建文件夹失败');
      folderDialogVisible.value = false;
    }
  } catch (error) {
    ElMessage.error('表单验证失败');
  }
};

// 打开重命名对话框
const openRenameDialog = (file: FileInfo) => {
  renameForm.name = file.fileName;
  renameForm.id = file.id;
  renameDialogVisible.value = true;
};

// 重命名文件/文件夹
const handleRename = () => {
  renameFormRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        await renameFile(renameForm.id, renameForm.name);
        ElMessage.success('重命名成功');
        renameDialogVisible.value = false;
        loadFileList(); // 重新加载文件列表
      } catch (error) {
        console.error('Failed to rename', error);
        ElMessage.error('重命名失败');
      }
    }
  });
};

// 确认删除
const confirmDelete = (file: FileInfo) => {
  ElMessageBox.confirm(
    `确定要删除${file.isDir ? '文件夹' : '文件'} "${file.fileName}" 吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await deleteFile([file.id]);
      ElMessage.success('删除成功');
      loadFileList(); // 重新加载文件列表
    } catch (error) {
      console.error('Failed to delete', error);
      ElMessage.error('删除失败');
    }
  });
};

// 处理分享
const handleShare = (file: FileInfo) => {
  selectedFileId.value = file.id;
  selectedFile.value = file;
  shareDialogVisible.value = true;
};

// 处理分享创建成功
const handleShareCreated = (shareResult: any) => {
  ElMessage.success(`分享创建成功，分享ID: ${shareResult.id}`);
};

// 收藏/取消收藏文件
const toggleStar = async (file: FileInfo) => {
  try {
    const isStar = !(file.isStarred || false);
    await toggleStarFile(file.id, isStar);
    
    // 更新本地文件状态
    file.isStarred = isStar;
    
    ElMessage.success(isStar ? '收藏成功' : '已取消收藏');
  } catch (error) {
    console.error('Failed to toggle star', error);
    ElMessage.error(file.isStarred ? '取消收藏失败' : '收藏失败');
  }
};

// 工具函数：格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

// 工具函数：格式化文件大小
const formatSize = (bytes: number) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
};

// 返回上一级文件夹
const goToParentFolder = () => {
  // 如果有面包屑导航，则获取当前文件夹的父文件夹ID
  if (breadcrumbList.value.length > 0) {
    // 如果只有一级，则返回根目录
    if (breadcrumbList.value.length === 1) {
      router.push('/');
    } else {
      // 否则返回上一级文件夹
      const parentIndex = breadcrumbList.value.length - 2;
      const parentFolder = breadcrumbList.value[parentIndex];
      router.push(`/folder/${parentFolder.id}`);
    }
  } else {
    // 如果没有面包屑导航，则返回根目录
    router.push('/');
  }
};

// 处理表格选择变化
const handleSelectionChange = (selection: FileInfo[]) => {
  selectedFiles.value = selection;
};

// 处理网格视图项点击
const handleGridItemClick = (file: FileInfo, event: MouseEvent) => {
  // 如果按住Ctrl键，则切换选中状态
  if (event.ctrlKey || event.metaKey) {
    // 查找是否已选中
    const index = selectedFiles.value.findIndex(item => item.id === file.id);
    if (index !== -1) {
      // 已选中，则取消选中
      selectedFiles.value.splice(index, 1);
    } else {
      // 未选中，则添加到选中列表
      selectedFiles.value.push(file);
    }
  } else {
    // 如果是已选中的文件，并且只选中了这一个，则进入文件夹或预览文件
    if (selectedFiles.value.length === 1 && selectedFiles.value[0].id === file.id) {
      handleRowClick(file);
    } else {
      // 否则，清空选择并选中当前文件
      selectedFiles.value = [file];
    }
  }
  event.stopPropagation(); // 阻止事件冒泡
};

// 批量删除文件
const batchDelete = async () => {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请先选择要删除的文件');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedFiles.value.length} 个文件吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );

    const fileIds = selectedFiles.value.map(file => file.id);
    const result = await batchDeleteFiles(fileIds);
    
    if (result) {
      ElMessage.success('删除成功');
      loadFileList(); // 重新加载文件列表
      selectedFiles.value = []; // 清空选择
    } else {
      ElMessage.error('删除失败');
    }
  } catch (error) {
    console.error('批量删除错误', error);
    // 用户取消操作不提示错误
    if (error !== 'cancel') {
      ElMessage.error('操作取消或发生错误');
    }
  }
};

// 加载初始数据
onMounted(() => {
  loadFileList();

  // 点击空白处时清除右键菜单
  document.addEventListener('click', (event) => {
    // 清除右键菜单
    contextMenuVisible.value = false;
    
    // 检查是否点击在文件列表区域外
    const target = event.target as HTMLElement;
    const tableContainer = document.querySelector('.file-list-container');
    const gridContainer = document.querySelector('.grid-container');
    const toolbar = document.querySelector('.toolbar');
    
    // 如果点击在表格区域、网格区域或工具栏内，则不清空选择
    if ((tableContainer && tableContainer.contains(target)) || 
        (gridContainer && gridContainer.contains(target)) || 
        (toolbar && toolbar.contains(target))) {
      return;
    }
    
    // 清空选择
    selectedFiles.value = [];
  });
});

// 添加onActivated钩子，当组件从缓存激活时也加载数据
onActivated(() => {
  loadFileList();
});
</script>

<style scoped>
.home-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.breadcrumb-container {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.back-button {
  margin-left: 10px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.left-actions,
.right-actions {
  display: flex;
  gap: 10px;
  align-items: center;
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
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 20px;
  padding: 10px;
}

.file-grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
  position: relative;
}

.file-grid-item:hover {
  background-color: #f5f5f5;
}

.file-grid-item.selected {
  background-color: #ecf5ff;
  border: 1px solid #409EFF;
}

.file-selection-indicator {
  position: absolute;
  top: 5px;
  right: 5px;
  background-color: #409EFF;
  color: white;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
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

.file-name {
  font-size: 14px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  font-size: 12px;
  color: #909399;
}

.context-menu {
  position: fixed;
  z-index: 1000;
  background: white;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  padding: 5px 0;
  min-width: 150px;
}

.context-menu ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.context-menu li {
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.context-menu li:hover {
  background-color: #f5f7fa;
}
</style> 