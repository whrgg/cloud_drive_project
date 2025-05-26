<template>
  <el-dialog
    v-model="dialogVisible"
    title="创建分享链接"
    width="550px"
    :close-on-click-modal="false"
  >
    <div class="share-dialog">
      <!-- 文件信息 -->
      <div class="file-info" v-if="fileInfo">
        <div class="file-icon">
          <el-icon v-if="fileInfo.isDir" :size="40"><Folder /></el-icon>
          <el-icon v-else-if="fileInfo.fileType === 'image'" :size="40"><Picture /></el-icon>
          <el-icon v-else-if="fileInfo.fileType === 'video'" :size="40"><VideoPlay /></el-icon>
          <el-icon v-else-if="fileInfo.fileType === 'audio'" :size="40"><Headset /></el-icon>
          <el-icon v-else :size="40"><Document /></el-icon>
        </div>
        <div class="file-details">
          <div class="file-name" :title="fileInfo.fileName">{{ fileInfo.fileName }}</div>
          <div class="file-meta">
            {{ fileInfo.isDir ? '文件夹' : formatFileSize(fileInfo.fileSize) }}
          </div>
        </div>
      </div>

      <!-- 分享设置表单 -->
      <el-form :model="shareForm" :rules="shareRules" ref="shareFormRef" label-width="100px">
        <el-form-item label="有效期" prop="expireTime">
          <el-select v-model="shareForm.expireTime" placeholder="请选择有效期">
            <el-option label="1天" :value="1" />
            <el-option label="7天" :value="7" />
            <el-option label="30天" :value="30" />
            <el-option label="永久有效" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item label="提取码">
          <div class="code-row">
            <el-input 
              v-model="shareForm.code" 
              placeholder="为空则不设置提取码"
              :disabled="shareForm.isRandomCode"
              maxlength="6"
            />
            <el-checkbox v-model="shareForm.isRandomCode" class="random-code-checkbox">
              随机生成
            </el-checkbox>
          </div>
        </el-form-item>

        <el-form-item label="分享说明" prop="description">
          <el-input 
            v-model="shareForm.description" 
            type="textarea" 
            placeholder="请输入分享说明（可选）"
            maxlength="200"
            show-word-limit
            :rows="3"
          />
        </el-form-item>
      </el-form>

      <!-- 分享链接信息 -->
      <div v-if="shareResult.id" class="share-result">
        <el-alert
          title="分享创建成功"
          type="success"
          :closable="false"
          show-icon
        />
        
        <div class="share-link-box">
          <div class="link-label">分享链接：</div>
          <el-input 
            v-model="shareUrl" 
            readonly
          >
            <template #append>
              <el-tooltip content="复制链接">
                <el-button @click="copyShareUrl">
                  <el-icon><DocumentCopy /></el-icon>
                </el-button>
              </el-tooltip>
            </template>
          </el-input>
        </div>
        
        <div v-if="shareResult.code" class="share-code-box">
          <div class="code-label">提取码：</div>
          <el-input 
            v-model="shareResult.code" 
            readonly
          >
            <template #append>
              <el-tooltip content="复制提取码">
                <el-button @click="copyShareCode">
                  <el-icon><DocumentCopy /></el-icon>
                </el-button>
              </el-tooltip>
            </template>
          </el-input>
        </div>

        <div class="share-qrcode" v-if="shareQrCodeUrl">
          <div class="qrcode-label">扫码分享：</div>
          <div class="qrcode-img">
            <img :src="shareQrCodeUrl" alt="分享二维码" />
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button 
          type="primary" 
          @click="createShare" 
          :loading="loading"
          v-if="!shareResult.id"
        >
          创建分享链接
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import type { FormInstance } from 'element-plus';
import { createShareLink } from '../../api/share';
import type { FileInfo } from '../../types/file';
import QRCode from 'qrcode';

// 定义组件属性
interface Props {
  visible: boolean;
  fileId: number;
  fileInfo?: FileInfo;
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  fileId: 0,
});

// 定义组件事件
const emit = defineEmits(['update:visible', 'share-created']);

// 内部状态
const dialogVisible = ref(props.visible);
const loading = ref(false);
const shareFormRef = ref<FormInstance>();
const shareResult = ref<{
  id: string;
  url: string;
  code?: string;
}>({
  id: '',
  url: '',
});
const shareQrCodeUrl = ref('');

// 随机生成4位提取码
const generateRandomCode = () => {
  const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  let result = '';
  for (let i = 0; i < 4; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
};

// 分享表单
const shareForm = reactive({
  expireTime: 7, // 默认有效期7天
  code: '',
  isRandomCode: true, // 默认随机生成提取码
  description: '',
});

// 表单验证规则
const shareRules = {
  expireTime: [
    { required: true, message: '请选择有效期', trigger: 'change' }
  ],
  description: [
    { max: 200, message: '分享说明不能超过200个字符', trigger: 'blur' }
  ],
};

// 完整分享链接
const shareUrl = computed(() => {
  if (!shareResult.value.url) return '';
  // 完整前端URL，如：http://localhost:5173/s/abc123
  return `${window.location.origin}/s/${shareResult.value.id}`;
});

// 监听visible变化
watch(
  () => props.visible,
  (newVal) => {
    dialogVisible.value = newVal;
    if (newVal && shareForm.isRandomCode) {
      shareForm.code = generateRandomCode();
    }
  }
);

// 监听dialogVisible变化
watch(
  () => dialogVisible.value,
  (newVal) => {
    emit('update:visible', newVal);
  }
);

// 监听isRandomCode变化
watch(
  () => shareForm.isRandomCode,
  (newVal) => {
    if (newVal) {
      shareForm.code = generateRandomCode();
    } else {
      shareForm.code = '';
    }
  }
);

// 创建分享链接
const createShare = async () => {
  if (!props.fileId) {
    ElMessage.error('未指定要分享的文件');
    return;
  }

  await shareFormRef.value?.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        // 调用API创建分享
        const result = await createShareLink({
          userFileId: props.fileId,
          expireType: shareForm.expireTime === 0 ? 0 : shareForm.expireTime,
          hasExtraction: !!shareForm.code,
          extractionCode: shareForm.isRandomCode ? shareForm.code : (shareForm.code || null),
          description: shareForm.description || null
        });
        
        // 设置分享结果
        shareResult.value = {
          id: result.shareCode,
          url: result.shareCode,
          code: result.extractionCode
        };
        
        // 生成二维码
        generateQrCode();
        
        // 通知父组件分享创建成功
        emit('share-created', shareResult.value);
        
        ElMessage.success('分享创建成功');
      } catch (error) {
        console.error('创建分享链接失败:', error);
        ElMessage.error('创建分享链接失败');
      } finally {
        loading.value = false;
      }
    }
  });
};

// 生成二维码
const generateQrCode = async () => {
  if (!shareUrl.value) return;
  
  try {
    // 生成二维码图片URL
    const url = await QRCode.toDataURL(shareUrl.value, {
      width: 200,
      margin: 1,
      color: {
        dark: '#000000',
        light: '#ffffff'
      }
    });
    
    shareQrCodeUrl.value = url;
  } catch (error) {
    console.error('生成二维码失败:', error);
  }
};

// 复制分享链接
const copyShareUrl = () => {
  if (!shareUrl.value) return;
  
  navigator.clipboard.writeText(shareUrl.value)
    .then(() => {
      ElMessage.success('分享链接已复制');
    })
    .catch((err) => {
      console.error('复制失败:', err);
      ElMessage.error('复制失败，请手动复制');
    });
};

// 复制提取码
const copyShareCode = () => {
  if (!shareResult.value.code) return;
  
  navigator.clipboard.writeText(shareResult.value.code)
    .then(() => {
      ElMessage.success('提取码已复制');
    })
    .catch((err) => {
      console.error('复制失败:', err);
      ElMessage.error('复制失败，请手动复制');
    });
};

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false;
  
  // 重置表单和状态
  if (!shareResult.value.id) {
    shareForm.expireTime = 7;
    shareForm.description = '';
    shareForm.isRandomCode = true;
    shareForm.code = generateRandomCode();
  }
};

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
};
</script>

<style scoped>
.share-dialog {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #f8f9fa;
}

.file-details {
  flex: 1;
  overflow: hidden;
}

.file-name {
  font-weight: bold;
  font-size: 16px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  color: #909399;
  font-size: 14px;
}

.code-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.random-code-checkbox {
  margin-left: 12px;
}

.share-result {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.share-link-box, 
.share-code-box {
  margin-top: 16px;
}

.link-label,
.code-label {
  margin-bottom: 8px;
  font-weight: bold;
}

.share-qrcode {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 16px;
}

.qrcode-label {
  align-self: flex-start;
  margin-bottom: 8px;
  font-weight: bold;
}

.qrcode-img {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 8px;
  background-color: #ffffff;
}

.qrcode-img img {
  max-width: 200px;
  max-height: 200px;
}
</style> 