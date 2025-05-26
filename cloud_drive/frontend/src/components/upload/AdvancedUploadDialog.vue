<template>
  <el-dialog
    v-model="dialogVisible"
    title="文件上传"
    width="650px"
    :close-on-click-modal="false"
    :before-close="handleClose"
  >
    <div class="advanced-upload-dialog">
      <!-- 上传方式切换 -->
      <el-tabs v-model="activeTab">
        <el-tab-pane label="普通上传" name="normal">
          <!-- 普通上传组件内容将在这里集成 -->
          <div class="normal-upload-container">
            <el-upload
              ref="uploadRef"
              class="upload-area"
              drag
              multiple
              :action="uploadUrl"
              :headers="uploadHeaders"
              :data="uploadData"
              :before-upload="beforeUpload"
              :on-success="handleSuccess"
              :on-error="handleError"
              :on-progress="handleProgress"
              :show-file-list="false" 
            >
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">
                拖拽文件到此处或 <em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  支持同时上传多个文件，单个文件大小不超过 {{ maxFileSizeDisplay }}。
                </div>
              </template>
            </el-upload>

            <div v-if="uploadingFiles.length > 0" class="upload-progress">
              <h4>上传进度</h4>
              <div v-for="file in uploadingFiles" :key="file.uid" class="progress-item">
                <div class="file-info">
                  <span class="file-name">{{ file.name }}</span>
                  <span class="file-size">{{ formatSize(file.size) }}</span>
                </div>
                <el-progress :percentage="file.percentage" :status="file.status" />
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="大文件上传" name="chunk">
          <chunk-uploader 
            :folder-id="folderId"
            @upload-success="handleUploadSuccess"
          />
          <div class="tip-box">
            <el-alert
              type="info"
              :closable="false"
              show-icon
            >
              <template #title>
                <p>大文件上传说明：</p>
                <ul>
                  <li>支持断点续传和文件秒传</li>
                  <li>建议超过100MB的文件使用此方式上传</li>
                  <li>上传过程中请勿关闭浏览器</li>
                </ul>
              </template>
            </el-alert>
          </div>
        </el-tab-pane>
        <el-tab-pane label="文件夹上传" name="folder">
          <div class="folder-upload">
            <div class="upload-options single-option">
              <div 
                class="upload-area el-upload-dragger-like" 
                @click="handleShowDirectoryPicker" 
                @dragover.prevent 
                @drop.prevent="handleFolderDrop"
                v-if="isFileSystemAccessSupported"
              >
                <div class="folder-upload-content">
                  <el-icon :size="40"><FolderAdd /></el-icon>
                  <div class="upload-text">点击选择文件夹或拖拽文件夹到此处上传</div>
                  <div class="upload-tip">上传后将保留完整的文件夹结构</div>
                </div>
              </div>
              <div class="upload-area el-upload-dragger-like" v-if="!isFileSystemAccessSupported">
                  <div class="folder-upload-content">
                    <el-icon :size="40"><FolderRemove /></el-icon>
                    <div class="upload-text">文件夹上传不可用</div>
                    <div class="api-support-tip">您的浏览器不支持此功能，请更新或更换浏览器。</div>
                  </div>
              </div>
            </div>
            
            <div class="folder-files" v-if="folderFiles.length > 0">
              <h4>已选文件({{ folderFiles.length }}个)：</h4>
              <div class="folder-file-list">
                <div v-for="(file, index) in folderDisplayFiles" :key="index" class="folder-file-item">
                  <span class="folder-file-path">{{ file.webkitRelativePath || (file as any).path || '未知路径' }}</span>
                  <span class="folder-file-size">{{ formatSize(file.size) }}</span>
                </div>
                <div v-if="folderFiles.length > 5" class="folder-more">
                  还有{{ folderFiles.length - 5 }}个文件...
                </div>
              </div>
              
              <div class="folder-upload-actions">
                <el-button @click="resetFolderUpload">清空</el-button>
                <el-button 
                  type="primary" 
                  @click="handleFolderUpload" 
                  :loading="folderUploading"
                >
                  {{ folderUploading ? '上传中...' : '开始上传' }}
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { UploadFilled, Folder, FolderAdd, FolderRemove } from '@element-plus/icons-vue';
import ChunkUploader from './ChunkUploader.vue';
import { uploadFile, createFolder } from '../../api/file';

// 定义组件属性
interface Props {
  visible: boolean;
  folderId: number;
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  folderId: 0,
});

// 定义事件
const emit = defineEmits(['update:visible', 'upload-success', 'close']);

// 内部状态
const dialogVisible = ref(props.visible);
const activeTab = ref('normal'); // 设回 normal 或其他，除非文件夹上传是唯一选项
const folderFiles = ref<File[]>([]);
const folderUploading = ref(false);

// 普通上传相关状态
const uploadRef = ref();
const uploadingFiles = ref<any[]>([]); // 用于跟踪上传中的文件
const maxFileSize = 1024 * 1024 * 500; // 500MB，根据实际需求调整
const uploadUrl = '/file/upload'; // 修正上传API端点
const uploadHeaders = ref({
  // 如果需要token或其他头部信息
  Authorization: `Bearer ${localStorage.getItem('token')}`, 
});
const uploadData = computed(() => ({
  parentId: props.folderId,
  path: '', // 如果需要，可以设置路径
}));

// 计算属性 - 展示前5个文件
const folderDisplayFiles = computed(() => {
  return folderFiles.value.slice(0, 5);
});

// 检查 File System Access API 支持情况
const isFileSystemAccessSupported = computed(() => {
  return (window as any).showDirectoryPicker !== undefined;
});

// 监听visible属性变更
watch(
  () => props.visible,
  (newVal) => {
    dialogVisible.value = newVal;
    if (newVal) {
      // 弹窗打开时，更新token
      uploadHeaders.value = {
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      };
      // 弹窗打开时，清空之前的上传列表（如果需要）
      // uploadingFiles.value = [];
      // folderFiles.value = [];
    }
  }
);

// 监听对话框状态变更
watch(
  () => dialogVisible.value,
  (newVal) => {
    emit('update:visible', newVal);
    if (!newVal) {
      emit('close');
    }
  }
);

// 处理上传成功事件
const handleUploadSuccess = () => {
  emit('upload-success');
};

// 使用 showDirectoryPicker API 选择文件夹并读取文件
const handleShowDirectoryPicker = async () => {
  if (!isFileSystemAccessSupported.value) {
    ElMessage.warning('您的浏览器不支持新的文件系统访问API，请使用传统方式上传文件夹。');
    return;
  }

  try {
    const directoryHandle = await (window as any).showDirectoryPicker();
    const newFiles: File[] = [];
    ElMessage.info('正在读取文件夹内容，请稍候...');
    
    // 使用文件夹名称作为根路径前缀
    const rootFolderName = directoryHandle.name;
    await processDirectoryHandle(directoryHandle, rootFolderName, newFiles);
    
    if (newFiles.length === 0) {
      ElMessage.info('选择的文件夹为空或未能读取到文件。');
      return;
    }

    // 与现有 folderFiles 合并或替换 (这里采用替换方式，并提示用户)
    if (folderFiles.value.length > 0) {
      await ElMessageBox.confirm(
        '已存在待上传文件列表，是否清空并使用新选择的文件夹内容？',
        '提示',
        {
          confirmButtonText: '确定清空并使用',
          cancelButtonText: '取消',
          type: 'warning',
        }
      );
      folderFiles.value = []; // 清空旧的
    }

    folderFiles.value.push(...newFiles);
    ElMessage.success(`已成功读取 ${newFiles.length} 个文件待上传。`);

  } catch (err: any) {
    if (err.name === 'AbortError') {
      ElMessage.info('文件夹选择已取消');
    } else {
      console.error('Error using showDirectoryPicker:', err);
      ElMessage.error('选择文件夹失败或读取内容出错，请检查浏览器权限或尝试传统方式。错误信息：' + err.message);
    }
  }
};

// 处理拖拽上传文件夹
const handleFolderDrop = async (event: DragEvent) => {
  if (!event.dataTransfer) return;
  
  // 阻止默认行为
  event.preventDefault();
  
  // 检查是否有文件夹
  const items = event.dataTransfer.items;
  if (!items || items.length === 0) {
    ElMessage.warning('没有检测到文件或文件夹');
    return;
  }
  
  // 检查是否支持webkitGetAsEntry API
  if (!items[0].webkitGetAsEntry) {
    ElMessage.warning('您的浏览器不支持文件夹拖拽上传');
    return;
  }
  
  // 清空现有文件列表（如果需要确认，可以添加确认对话框）
  if (folderFiles.value.length > 0) {
    try {
      await ElMessageBox.confirm(
        '已存在待上传文件列表，是否清空并使用新拖入的文件夹内容？',
        '提示',
        {
          confirmButtonText: '确定清空并使用',
          cancelButtonText: '取消',
          type: 'warning',
        }
      );
      folderFiles.value = []; // 清空旧的
    } catch (err) {
      // 用户取消了操作
      return;
    }
  }
  
  ElMessage.info('正在读取拖入的文件夹内容，请稍候...');
  
  // 处理所有拖入的项目
  const newFiles: File[] = [];
  const promises: Promise<void>[] = [];
  
  for (let i = 0; i < items.length; i++) {
    const entry = items[i].webkitGetAsEntry();
    if (entry) {
      // 如果是文件夹，递归处理
      if (entry.isDirectory) {
        const directoryEntry = entry as any; // FileSystemDirectoryEntry
        promises.push(processDirectoryEntry(directoryEntry, directoryEntry.name, newFiles));
      } else {
        // 如果是单个文件，也添加到列表中
        const fileEntry = entry as any; // FileSystemFileEntry
        promises.push(
          new Promise<void>((resolve) => {
            fileEntry.file((file: File) => {
              // 添加相对路径属性
              Object.defineProperty(file, 'webkitRelativePath', {
                value: fileEntry.fullPath.substring(1), // 去掉开头的斜杠
                writable: true,
                enumerable: true,
                configurable: true,
              });
              newFiles.push(file);
              resolve();
            });
          })
        );
      }
    }
  }
  
  // 等待所有文件处理完成
  await Promise.all(promises);
  
  if (newFiles.length === 0) {
    ElMessage.info('拖入的文件夹为空或未能读取到文件。');
    return;
  }
  
  folderFiles.value.push(...newFiles);
  ElMessage.success(`已成功读取 ${newFiles.length} 个文件待上传。`);
};

// 递归处理拖拽的目录条目
async function processDirectoryEntry(
  directoryEntry: any, // FileSystemDirectoryEntry
  currentPath: string,
  fileList: File[]
): Promise<void> {
  const reader = directoryEntry.createReader();
  
  // 读取目录内容（可能需要多次读取）
  const readEntries = async (): Promise<any[]> => {
    return new Promise((resolve) => {
      reader.readEntries((entries: any[]) => {
        resolve(entries);
      });
    });
  };
  
  // 可能需要多次读取才能获取所有条目
  let entries: any[] = [];
  let batchEntries = await readEntries();
  while (batchEntries.length > 0) {
    entries = entries.concat(batchEntries);
    batchEntries = await readEntries();
  }
  
  // 处理所有条目
  const promises: Promise<void>[] = [];
  
  for (const entry of entries) {
    const entryPath = currentPath ? `${currentPath}/${entry.name}` : entry.name;
    
    if (entry.isFile) {
      // 处理文件
      promises.push(
        new Promise<void>((resolve) => {
          entry.file((file: File) => {
            // 添加相对路径属性
            Object.defineProperty(file, 'webkitRelativePath', {
              value: entryPath,
              writable: true,
              enumerable: true,
              configurable: true,
            });
            // 打印文件路径
            console.log(`[文件夹上传-拖拽] 文件路径: ${entryPath}`);
            fileList.push(file);
            resolve();
          });
        })
      );
    } else if (entry.isDirectory) {
      // 避免读取常见的隐藏文件夹或特殊文件夹
      if (entry.name === '.git' || entry.name === 'node_modules' || entry.name.startsWith('.')) {
        console.log(`Skipping directory: ${entryPath}`);
        continue;
      }
      // 打印目录路径
      console.log(`[文件夹上传-拖拽] 目录路径: ${entryPath}/`);
      // 递归处理子目录
      promises.push(processDirectoryEntry(entry, entryPath, fileList));
    }
  }
  
  await Promise.all(promises);
}

// 递归处理目录句柄 (File System Access API)
async function processDirectoryHandle(
  directoryHandle: any, // FileSystemDirectoryHandle,
  currentPath: string,
  fileList: File[]
) {
  for await (const entry of directoryHandle.values()) {
    const entryPath = currentPath ? `${currentPath}/${entry.name}` : entry.name;
    if (entry.kind === 'file') {
      try {
        const fileHandle = entry as any; // FileSystemFileHandle;
        const file = await fileHandle.getFile();
        // 为 File 对象添加 webkitRelativePath 属性以兼容现有逻辑
        Object.defineProperty(file, 'webkitRelativePath', {
          value: entryPath,
          writable: true,
          enumerable: true,
          configurable: true,
        });
        // 同时添加path属性作为备份
        Object.defineProperty(file, 'path', {
          value: entryPath,
          writable: true,
          enumerable: true,
          configurable: true,
        });
        // 打印文件路径
        console.log(`[文件夹上传] 文件路径: ${entryPath}`);
        fileList.push(file);
      } catch (fileError) {
        console.error(`无法读取文件 ${entryPath}:`, fileError);
        ElMessage.warning(`跳过无法读取的文件：${entryPath}`);
      }
    }
    else if (entry.kind === 'directory') {
      const subDirectoryHandle = entry as any; // FileSystemDirectoryHandle;
      // 避免读取常见的隐藏文件夹或特殊文件夹，可根据需要扩展
      if (entry.name === '.git' || entry.name === 'node_modules' || entry.name.startsWith('.')) {
        console.log(`Skipping directory: ${entryPath}`);
        continue;
      }
      // 打印目录路径
      console.log(`[文件夹上传] 目录路径: ${entryPath}/`);
      await processDirectoryHandle(subDirectoryHandle, entryPath, fileList);
    }
  }
}

// 文件夹上传 - 处理上传
const handleFolderUpload = async () => {
  if (folderFiles.value.length === 0) {
    ElMessage.warning('请先选择文件夹');
    return;
  }
  
  folderUploading.value = true;
  let successCount = 0;
  let errorCount = 0;
  let totalFiles = folderFiles.value.length;
  
  ElMessage.info(`开始上传 ${totalFiles} 个文件，请勿关闭窗口...`);

  // 创建进度通知
  const notificationKey = `folder-upload-${Date.now()}`;
  const updateProgress = (current: number) => {
    const percentage = Math.round((current / totalFiles) * 100);
    ElMessage.closeAll(); // 关闭之前的消息
    if (percentage < 100) {
      ElMessage({
        message: `正在上传: ${current}/${totalFiles} (${percentage}%)`,
        type: 'info',
        duration: 0,
        showClose: true,
        key: notificationKey
      });
    }
  };

  try {
    // 1. 预处理所有文件路径，收集所有目录结构
    const allFolderPaths = new Set<string>();
    const pathMapping: {[path: string]: {parentId: number, exists: boolean}} = {};
    
    // 初始化根目录
    pathMapping[""] = { parentId: props.folderId, exists: true };
    
    // 收集所有文件夹路径
    for (const file of folderFiles.value) {
      const relativePath = file.webkitRelativePath || (file as any).path || '';
      const lastSeparatorIndex = relativePath.lastIndexOf('/');
      
      if (lastSeparatorIndex !== -1) {
        // 有目录结构
        const folderPath = relativePath.substring(0, lastSeparatorIndex);
        // 分解目录层级
        const parts = folderPath.split('/');
        let currentPath = "";
        
        // 构建每一级目录的完整路径
        for (let i = 0; i < parts.length; i++) {
          const part = parts[i];
          if (currentPath) {
            currentPath += "/" + part;
          } else {
            currentPath = part;
          }
          allFolderPaths.add(currentPath);
        }
      }
    }
    
    console.log(`[文件夹上传] 预处理完成，共发现 ${allFolderPaths.size} 个目录层级`);
    console.log(`[文件夹上传] 目录结构:`, Array.from(allFolderPaths));
    
    // 2. 按层级顺序创建目录
    const sortedFolderPaths = Array.from(allFolderPaths).sort((a, b) => {
      return a.split('/').length - b.split('/').length || a.localeCompare(b);
    });
    
    // 逐级创建目录
    for (const folderPath of sortedFolderPaths) {
      const parts = folderPath.split('/');
      const folderName = parts[parts.length - 1];
      const parentPath = parts.slice(0, -1).join('/');
      const parentInfo = pathMapping[parentPath] || pathMapping[""];
      
      console.log(`[文件夹上传] 准备创建目录: ${folderName}, 父路径: ${parentPath}, 父ID: ${parentInfo.parentId}`);
      
      try {
        // 使用导入的createFolder函数
        const result = await createFolder(parentInfo.parentId, folderName);
        
        if (result && result.list && result.list.length > 0) {
          // 找到创建的文件夹或已存在的文件夹
          const folder = result.list.find((item) => 
            item.fileName === folderName && item.isDir && item.parentId === parentInfo.parentId
          );
          
          if (folder) {
            pathMapping[folderPath] = { parentId: folder.id, exists: true };
            console.log(`[文件夹上传] 目录创建成功或已存在: ${folderPath}, ID: ${folder.id}`);
          } else {
            throw new Error(`未能在响应中找到创建的文件夹: ${folderName}`);
          }
        } else {
          throw new Error('创建文件夹失败，返回结果无效');
        }
      } catch (error: any) {
        console.error(`[文件夹上传] 创建目录失败: ${folderPath}`, error);
        ElMessage.warning(`创建目录 ${folderPath} 失败: ${error.message || '未知错误'}`);
        // 标记为不存在，但继续处理其他目录
        pathMapping[folderPath] = { parentId: -1, exists: false };
      }
    }
    
    // 3. 上传文件到对应的目录
    // 并发控制
    const concurrentLimit = 3; // 最多同时上传3个文件
    const queue = [...folderFiles.value];
    const executing = new Set();
    
    const enqueue = async (): Promise<void> => {
      if (queue.length === 0) return;
      
      const file = queue.shift()!;
      executing.add(file);
      
      try {
        // 获取文件的相对路径
        const relativePath = file.webkitRelativePath || (file as any).path || '';
        const lastSeparatorIndex = relativePath.lastIndexOf('/');
        
        let parentId = props.folderId;
        let fileName = relativePath;
        
        if (lastSeparatorIndex !== -1) {
          // 有目录结构
          const folderPath = relativePath.substring(0, lastSeparatorIndex);
          fileName = relativePath.substring(lastSeparatorIndex + 1);
          
          // 获取父目录ID
          if (pathMapping[folderPath] && pathMapping[folderPath].exists) {
            parentId = pathMapping[folderPath].parentId;
          } else {
            // 如果父目录创建失败，跳过该文件
            console.error(`[文件夹上传] 父目录未创建成功，跳过文件: ${relativePath}`);
            errorCount++;
            executing.delete(file);
            updateProgress(successCount + errorCount);
            if (queue.length > 0) {
              await enqueue();
            }
            return;
          }
        }
        
        // 打印上传文件的详细信息
        console.log(`[文件夹上传] 正在上传文件:
          文件名: ${fileName}
          相对路径: ${relativePath}
          父文件夹ID: ${parentId}
          文件大小: ${formatSize(file.size)}
          文件类型: ${file.type || '未知类型'}
        `);
        
        // 使用uploadFile API函数
        await uploadFile({ 
          file: file, 
          parentId: parentId, 
          // 不再需要传递relativePath，因为我们已经处理了目录结构
          onProgress: (percentage) => {
            // 可以在这里添加单个文件的进度显示逻辑
            console.log(`[文件夹上传] 文件 ${fileName} 上传进度: ${percentage}%`);
          }
        });
        successCount++;
      } catch (error) {
        errorCount++;
        console.error(`Failed to upload file ${file.name}:`, error);
      } finally {
        executing.delete(file);
        updateProgress(successCount + errorCount);
        
        // 如果队列中还有文件，继续处理
        if (queue.length > 0) {
          await enqueue();
        }
      }
    };
    
    // 开始并发上传
    const promises: Promise<void>[] = [];
    for (let i = 0; i < Math.min(concurrentLimit, folderFiles.value.length); i++) {
      promises.push(enqueue());
    }
    
    // 等待所有上传完成
    await Promise.all(promises);
    
  } catch (error) {
    console.error(`[文件夹上传] 上传过程中发生错误:`, error);
    ElMessage.error(`上传过程中发生错误: ${error instanceof Error ? error.message : String(error)}`);
  } finally {
    folderUploading.value = false;
    ElMessage.closeAll(); // 关闭进度通知
    
    if (errorCount === 0 && successCount > 0) {
      ElMessage.success(`成功上传 ${successCount} 个文件`);
      emit('upload-success');
      resetFolderUpload(); // 清空已选择的文件列表
    } else if (successCount > 0 && errorCount > 0) {
      ElMessage.warning(`部分文件上传成功：${successCount} 个成功，${errorCount} 个失败`);
      emit('upload-success'); // 部分成功也触发，以便刷新列表
      resetFolderUpload();
    } else if (errorCount > 0 && successCount === 0) {
      ElMessage.error(`所有 ${errorCount} 个文件上传失败`);
      // 失败时不一定需要清空，用户可能想重试
    } else if (successCount === 0 && errorCount === 0 && folderFiles.value.length > 0) {
      ElMessage.info('没有文件被上传，请检查文件或网络状态。');
    }
  }
};

// 重置文件夹上传
const resetFolderUpload = () => {
  folderFiles.value = [];
  folderUploading.value = false;
};

// 格式化文件大小
const formatSize = (bytes: number) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
};

const maxFileSizeDisplay = computed(() => formatSize(maxFileSize));

const beforeUpload = (rawFile: File & { uid?: number }) => {
  if (rawFile.size > maxFileSize) {
    ElMessage.error(`文件大小不能超过 ${maxFileSizeDisplay.value}`);
    return false;
  }
  
  const newFile = {
    uid: rawFile.uid || Date.now(), // 确保每个文件有唯一的uid
    name: rawFile.name,
    size: rawFile.size,
    percentage: 0,
    status: '', // 'uploading', 'success', 'exception'
    raw: rawFile,
  };
  uploadingFiles.value.push(newFile);
  return true;
};

const handleSuccess = (response: any, uploadFile: any) => {
  const fileIndex = uploadingFiles.value.findIndex(f => f.uid === uploadFile.uid);
  if (fileIndex !== -1) {
    uploadingFiles.value[fileIndex].status = 'success';
    uploadingFiles.value[fileIndex].percentage = 100;
  }
  ElMessage.success(`${uploadFile.name} 上传成功`);
  emit('upload-success'); // 触发上传成功事件
  
  // 考虑是否在成功后从列表中移除
  // setTimeout(() => {
  //   uploadingFiles.value = uploadingFiles.value.filter(f => f.uid !== uploadFile.uid);
  // }, 2000);
};

const handleError = (error: any, uploadFile: any) => {
  const fileIndex = uploadingFiles.value.findIndex(f => f.uid === uploadFile.uid);
  if (fileIndex !== -1) {
    uploadingFiles.value[fileIndex].status = 'exception';
  }
  ElMessage.error(`${uploadFile.name} 上传失败: ${error.message || '未知错误'}`);
};

const handleProgress = (event: any, uploadFile: any) => {
  const fileIndex = uploadingFiles.value.findIndex(f => f.uid === uploadFile.uid);
  if (fileIndex !== -1) {
    uploadingFiles.value[fileIndex].percentage = Math.round(event.percent);
    uploadingFiles.value[fileIndex].status = 'uploading';
  }
};

// 处理对话框关闭
const handleClose = (done: () => void) => {
  // 如果正在上传，提示用户
  if (folderUploading.value || uploadingFiles.value.some(f => f.status === 'uploading')) {
    ElMessage.warning('正在上传文件，请等待上传完成或取消上传');
    return;
  }
  
  // 如果有已选文件但未上传，提示用户
  if (folderFiles.value.length > 0 && activeTab.value === 'folder') {
    ElMessageBox.confirm('关闭对话框将清空已选择的文件，是否确认关闭？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
      .then(() => {
        resetFolderUpload();
        done();
      })
      .catch(() => {});
  } else if (uploadingFiles.value.length > 0 && activeTab.value === 'normal' && uploadingFiles.value.some(f => f.status !== 'success' && f.status !== 'exception')) {
      ElMessageBox.confirm('关闭对话框将清空正在上传或待上传的文件列表，是否确认关闭？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
      .then(() => {
        uploadingFiles.value = []; // 清空普通上传列表
        done();
      })
      .catch(() => {});
  } else {
    done();
  }
};
</script>

<style scoped>
.advanced-upload-dialog {
  /* 根据需要添加样式 */
}

.normal-upload-container {
  padding: 20px;
}

.upload-area {
  width: 100%;
  margin-bottom: 20px;
}

.el-upload__tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.upload-progress {
  margin-top: 20px;
}

.upload-progress h4 {
  margin-bottom: 10px;
  font-size: 16px;
}

.progress-item {
  margin-bottom: 15px;
}

.progress-item .file-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
  font-size: 14px;
}

.progress-item .file-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 70%;
}

.progress-item .file-size {
  color: #909399;
}

.folder-upload {
  padding: 20px;
}

.folder-upload-area {
  width: 100%;
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.3s;
}

.folder-upload-area:hover {
  border-color: #409eff;
}

/* 使普通div看起来像el-upload的拖拽区域 */
.el-upload-dragger-like {
  padding: 20px; /* 根据el-upload的样式调整 */
  background-color: #fff; /* 根据el-upload的样式调整 */
  border: 1px dashed #d9d9d9; /* 根据el-upload的样式调整 */
  border-radius: 6px; /* 根据el-upload的样式调整 */
  box-sizing: border-box;
  width: 100%; /* 或者具体的宽度 */
  height: 180px; /* 与el-upload拖拽区高度一致或自适应 */
  text-align: center;
  cursor: pointer;
  display: flex; /* 用于内部元素居中 */
  align-items: center; /* 用于内部元素居中 */
  justify-content: center; /* 用于内部元素居中 */
  flex-direction: column; /* 用于内部元素居中 */
  transition: border-color .3s;
}

.el-upload-dragger-like:hover {
  border-color: #409eff;
}

.folder-upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #606266;
}

.folder-upload-content .el-icon {
  margin-bottom: 10px;
}

.upload-text {
  font-size: 16px;
  margin-bottom: 8px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
}

.folder-files {
  margin-top: 20px;
}

.folder-files h4 {
  margin-bottom: 10px;
}

.folder-file-list {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #eee;
  padding: 10px;
  border-radius: 4px;
  background-color: #f9f9f9;
}

.folder-file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 0;
  font-size: 13px;
  border-bottom: 1px dashed #eee;
}

.folder-file-item:last-child {
  border-bottom: none;
}

.folder-file-path {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 10px;
}

.folder-more {
  margin-top: 5px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.folder-upload-actions {
  margin-top: 15px;
  text-align: right;
}

.tip-box {
  margin-top: 15px;
  padding: 10px;
  background-color: #f4f4f5;
  border-radius: 4px;
}

.tip-box ul {
  padding-left: 20px;
  margin: 5px 0 0 0;
}

.tip-box li {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}

.upload-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.options-divider {
  height: 20px;
  background-color: #d9d9d9;
}

.api-support-tip {
  font-size: 12px;
  color: #909399;
}

.upload-options.single-option {
  justify-content: center;
}

.options-divider {
  /* display: none; */
}
</style> 