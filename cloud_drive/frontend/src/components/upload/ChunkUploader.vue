<template>
  <div class="chunk-uploader">
    <div class="upload-header">
      <h3>大文件上传</h3>
      <div class="upload-controls">
        <el-button @click="selectFile" :disabled="isUploading">选择文件</el-button>
        <input 
          ref="fileInputRef"
          type="file" 
          class="hidden-input"
          @change="handleFileChange"
        />
      </div>
    </div>

    <div v-if="file" class="file-info">
      <div class="file-name">{{ file.name }}</div>
      <div class="file-size">{{ formatSize(file.size) }}</div>
      <div class="upload-progress">
        <el-progress 
          :text-inside="true"
          :stroke-width="18" 
          :percentage="uploadProgress" 
          :status="uploadStatus"
        />
        <div class="progress-text">
          <template v-if="uploadState === 'idle'">
            等待上传
          </template>
          <template v-else-if="uploadState === 'checking'">
            校验文件MD5...
          </template>
          <template v-else-if="uploadState === 'uploading'">
            {{ formatSize(uploadedSize) }} / {{ formatSize(file.size) }} 
            ({{ getChunkStatus() }})
          </template>
          <template v-else-if="uploadState === 'merging'">
            合并分片中...
          </template>
          <template v-else-if="uploadState === 'success'">
            上传完成
          </template>
          <template v-else-if="uploadState === 'error'">
            上传失败: {{ errorMessage }}
          </template>
        </div>
      </div>
    </div>

    <div class="upload-actions" v-if="file">
      <el-button 
        type="primary" 
        @click="startUpload" 
        :disabled="isUploading || uploadState === 'success'"
        v-if="uploadState === 'idle' || uploadState === 'error'"
      >
        开始上传
      </el-button>
      <el-button 
        type="danger" 
        @click="cancelUpload" 
        v-if="isUploading"
      >
        取消上传
      </el-button>
      <el-button 
        @click="resetUpload" 
        v-if="uploadState === 'success' || uploadState === 'error'"
      >
        重新选择
      </el-button>
    </div>

    <!-- 上传完成提示 -->
    <div class="upload-message" v-if="uploadState === 'success'">
      <el-alert
        title="文件上传成功"
        type="success"
        :closable="false"
        show-icon
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import SparkMD5 from 'spark-md5';
import { checkFileMd5, uploadChunk, mergeChunks } from '../../api/file';

// 定义组件属性
interface Props {
  folderId: number; // 父文件夹ID
  chunkSize?: number; // 分片大小，单位为字节
  concurrentRequests?: number; // 并发请求数
}

const props = withDefaults(defineProps<Props>(), {
  folderId: 0,
  chunkSize: 5 * 1024 * 1024, // 默认5MB
  concurrentRequests: 3 // 默认并发3个请求
});

// 定义事件
const emit = defineEmits(['upload-progress', 'upload-success', 'upload-error']);

// 文件上传状态
type UploadState = 'idle' | 'checking' | 'uploading' | 'merging' | 'success' | 'error';

// 内部状态
const fileInputRef = ref<HTMLInputElement | null>(null);
const file = ref<File | null>(null);
const chunks = ref<Blob[]>([]);
const fileMd5 = ref<string>('');
const uploadState = ref<UploadState>('idle');
const uploadProgress = ref(0);
const uploadedSize = ref(0);
const uploadedChunks = ref<number[]>([]);
const errorMessage = ref('');
const abortController = ref<AbortController | null>(null);

// 计算属性
const uploadStatus = computed(() => {
  switch (uploadState.value) {
    case 'checking':
    case 'uploading':
    case 'merging':
      return 'primary';
    case 'success':
      return 'success';
    case 'error':
      return 'exception';
    default:
      return '';
  }
});

const isUploading = computed(() => {
  return ['checking', 'uploading', 'merging'].includes(uploadState.value);
});

// 选择文件
const selectFile = () => {
  fileInputRef.value?.click();
};

// 处理文件选择
const handleFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement;
  if (input.files && input.files.length > 0) {
    file.value = input.files[0];
    resetUploadState();
  }
};

// 重置上传状态
const resetUploadState = () => {
  chunks.value = [];
  fileMd5.value = '';
  uploadState.value = 'idle';
  uploadProgress.value = 0;
  uploadedSize.value = 0;
  uploadedChunks.value = [];
  errorMessage.value = '';
};

// 开始上传
const startUpload = async () => {
  if (!file.value) {
    ElMessage.warning('请先选择文件');
    return;
  }

  try {
    // 重置中断控制器
    abortController.value = new AbortController();

    // 1. 计算文件MD5
    await calculateFileMd5();

    // 2. 检查文件MD5是否已存在（秒传功能）
    uploadState.value = 'checking';
    const checkResult = await checkFileMd5(
      fileMd5.value, 
      file.value.name,
      file.value.size
    );

    if (checkResult.isExists) {
      // 文件已存在，秒传成功
      uploadProgress.value = 100;
      uploadState.value = 'success';
      uploadedSize.value = file.value.size;
      emit('upload-success', {
        fileId: checkResult.fileId,
        fileName: file.value.name,
        fileSize: file.value.size
      });
      ElMessage.success('文件秒传成功');
      return;
    }

    // 3. 设置已上传的分片
    if (checkResult.uploadedChunks) {
      uploadedChunks.value = checkResult.uploadedChunks;
    }

    // 4. 分片上传
    uploadState.value = 'uploading';
    await uploadFileChunks();

    // 5. 合并分片
    uploadState.value = 'merging';
    await mergeFileChunks();

    // 上传成功
    uploadState.value = 'success';
    uploadProgress.value = 100;
    emit('upload-success', {
      fileName: file.value.name,
      fileSize: file.value.size
    });
  } catch (error: any) {
    // 如果是用户取消，不显示错误
    if (error.name === 'AbortError') {
      return;
    }

    uploadState.value = 'error';
    errorMessage.value = error.message || '上传失败';
    emit('upload-error', errorMessage.value);
    ElMessage.error(errorMessage.value);
  }
};

// 计算文件MD5
const calculateFileMd5 = () => {
  if (!file.value) return Promise.reject('未选择文件');

  return new Promise<void>((resolve, reject) => {
    const chunkSize = props.chunkSize;
    const fileSize = file.value.size;
    const totalChunks = Math.ceil(fileSize / chunkSize);
    const spark = new SparkMD5.ArrayBuffer();
    const fileReader = new FileReader();
    let currentChunk = 0;
    
    // 分片MD5计算完成的回调
    const onLoadHandler = (e: ProgressEvent<FileReader>) => {
      if (e.target?.result) {
        spark.append(e.target.result as ArrayBuffer);
      }
      
      currentChunk++;
      
      if (currentChunk < totalChunks) {
        // 更新进度
        uploadProgress.value = Math.floor((currentChunk / totalChunks) * 40); // MD5计算占进度的40%
        emit('upload-progress', uploadProgress.value);

        // 继续计算下一个分片的MD5
        loadNext();
      } else {
        // MD5计算完成
        fileMd5.value = spark.end();
        
        // 创建所有分片
        createFileChunks();
        
        resolve();
      }
    };
    
    // 分片MD5计算错误的回调
    const onErrorHandler = () => {
      reject('文件MD5计算失败');
    };
    
    // 加载下一个分片
    const loadNext = () => {
      const start = currentChunk * chunkSize;
      const end = Math.min(start + chunkSize, fileSize);
      const chunk = file.value!.slice(start, end);
      
      fileReader.readAsArrayBuffer(chunk);
    };
    
    // 注册事件处理器
    fileReader.onload = onLoadHandler;
    fileReader.onerror = onErrorHandler;
    
    // 开始计算MD5
    loadNext();
  });
};

// 创建文件分片
const createFileChunks = () => {
  if (!file.value) return;

  const chunkSize = props.chunkSize;
  const fileSize = file.value.size;
  const totalChunks = Math.ceil(fileSize / chunkSize);
  
  chunks.value = [];
  
  for (let i = 0; i < totalChunks; i++) {
    const start = i * chunkSize;
    const end = Math.min(start + chunkSize, fileSize);
    const chunk = file.value.slice(start, end);
    chunks.value.push(chunk);
  }
  
  console.log(`文件大小: ${fileSize} 字节, 分片大小: ${chunkSize} 字节, 计算得到分片数: ${totalChunks}`);
};

// 上传文件分片
const uploadFileChunks = async () => {
  if (chunks.value.length === 0) return Promise.reject('未创建文件分片');
  
  // 创建并发队列
  const totalChunks = chunks.value.length;
  const pendingChunks = [];
  
  console.log(`开始上传分片，总分片数: ${totalChunks}`);
  
  // 将未上传的分片加入待上传队列
  for (let i = 0; i < totalChunks; i++) {
    if (!uploadedChunks.value.includes(i)) {
      pendingChunks.push(i);
    } else {
      // 计算已上传分片的大小
      uploadedSize.value += chunks.value[i].size;
    }
  }
  
  // 如果所有分片都已上传，直接返回
  if (pendingChunks.length === 0) {
    uploadProgress.value = 80; // 分片上传完成占80%
    return Promise.resolve();
  }
  
  // 更新进度
  updateUploadProgress();
  
  // 创建分片上传任务
  return new Promise<void>((resolve, reject) => {
    let completedChunks = 0;
    let activeRequests = 0;
    let rejected = false;
    
    // 执行上传任务
    const executeTask = async () => {
      if (rejected || !abortController.value) return;
      
      // 如果所有分片都已处理，等待上传完成
      if (completedChunks >= pendingChunks.length) {
        if (activeRequests === 0) {
          console.log(`所有分片上传完成，实际上传分片数: ${uploadedChunks.value.length}`);
          resolve();
        }
        return;
      }
      
      // 如果有可用的并发槽位，继续上传
      while (activeRequests < props.concurrentRequests && pendingChunks.length > 0) {
        const chunkIndex = pendingChunks.shift()!;
        activeRequests++;
        
        uploadChunkFile(chunkIndex)
          .then(() => {
            completedChunks++;
            activeRequests--;
            uploadedChunks.value.push(chunkIndex);
            uploadedSize.value += chunks.value[chunkIndex].size;
            updateUploadProgress();
            executeTask();
          })
          .catch(err => {
            if (!rejected) {
              rejected = true;
              reject(err);
            }
          });
      }
    };
    
    // 开始执行上传任务
    executeTask();
  });
};

// 上传单个分片
const uploadChunkFile = async (chunkIndex: number) => {
  if (!file.value || !abortController.value) {
    return Promise.reject('上传已取消');
  }
  
  const chunk = chunks.value[chunkIndex];
  const totalChunks = chunks.value.length;
  
  // 创建一个新的文件对象，以包含分片索引信息
  const chunkFile = new File([chunk], file.value.name, { 
    type: file.value.type 
  });
  
  // 上传分片
  try {
    console.log(`上传分片 ${chunkIndex}/${totalChunks-1}`);
    await uploadChunk({
      file: chunkFile,
      parentId: props.folderId,
      md5: fileMd5.value,
      chunkIndex: chunkIndex,
      chunks: totalChunks
    });
    
    return Promise.resolve();
  } catch (error) {
    console.error(`分片 ${chunkIndex} 上传失败:`, error);
    return Promise.reject(`分片 ${chunkIndex} 上传失败`);
  }
};

// 合并文件分片
const mergeFileChunks = async () => {
  if (!file.value) return Promise.reject('未选择文件');
  
  try {
    uploadProgress.value = 90; // 合并分片占到90%
    const actualChunksCount = chunks.value.length;
    console.log(`请求合并分片，文件名: ${file.value.name}, 分片数: ${actualChunksCount}, 已上传分片数: ${uploadedChunks.value.length}`);
    
    // 确保所有分片都已上传
    if (uploadedChunks.value.length !== actualChunksCount) {
      console.warn(`警告: 已上传分片数(${uploadedChunks.value.length})与总分片数(${actualChunksCount})不一致`);
      
      // 查找缺失的分片
      const missingChunks = [];
      for (let i = 0; i < actualChunksCount; i++) {
        if (!uploadedChunks.value.includes(i)) {
          missingChunks.push(i);
        }
      }
      
      if (missingChunks.length > 0) {
        console.error(`缺少分片: ${missingChunks.join(', ')}`);
        return Promise.reject(`合并失败: 缺少 ${missingChunks.length} 个分片`);
      }
    }
    
    await mergeChunks(
      fileMd5.value,
      file.value.name,
      props.folderId,
      actualChunksCount
    );
    return Promise.resolve();
  } catch (error) {
    console.error('合并文件分片失败:', error);
    return Promise.reject('合并文件分片失败: ' + (error instanceof Error ? error.message : String(error)));
  }
};

// 更新上传进度
const updateUploadProgress = () => {
  if (!file.value) return;
  
  const progress = Math.floor(40 + (uploadedSize.value / file.value.size) * 40); // 分片上传占进度的40%
  uploadProgress.value = Math.min(progress, 80); // 最多到80%，合并分片需要额外进度
  emit('upload-progress', uploadProgress.value);
};

// 获取分片上传状态描述
const getChunkStatus = () => {
  if (chunks.value.length === 0) return '';
  return `已上传 ${uploadedChunks.value.length}/${chunks.value.length} 个分片`;
};

// 取消上传
const cancelUpload = () => {
  if (abortController.value) {
    abortController.value.abort();
    abortController.value = null;
  }
  
  uploadState.value = 'idle';
  uploadProgress.value = 0;
  ElMessage.info('上传已取消');
};

// 重置上传
const resetUpload = () => {
  file.value = null;
  resetUploadState();
};

// 格式化文件大小
const formatSize = (bytes: number) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
};
</script>

<style scoped>
.chunk-uploader {
  padding: 20px;
  border: 1px dashed #e0e0e0;
  border-radius: 4px;
  background-color: #f8f9fa;
}

.upload-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.upload-header h3 {
  margin: 0;
}

.hidden-input {
  display: none;
}

.file-info {
  margin-bottom: 20px;
}

.file-name {
  font-weight: bold;
  font-size: 16px;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  color: #909399;
  margin-bottom: 10px;
}

.upload-progress {
  margin-bottom: 20px;
}

.progress-text {
  margin-top: 5px;
  color: #606266;
  text-align: center;
}

.upload-actions {
  display: flex;
  gap: 10px;
}

.upload-message {
  margin-top: 20px;
}
</style> 