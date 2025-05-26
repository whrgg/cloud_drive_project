<template>
  <div class="advanced-preview-container" ref="previewContainer">
    <!-- 全屏按钮 -->
    <div class="preview-toolbar">
      <el-tooltip content="全屏预览" placement="top">
        <el-button 
          class="fullscreen-btn" 
          circle 
          @click="toggleFullscreen"
          v-if="isFullscreenSupported && canFullscreen"
        >
          <el-icon v-if="isFullscreen"><FullscreenExit /></el-icon>
          <el-icon v-else><FullScreen /></el-icon>
        </el-button>
      </el-tooltip>
      
      <!-- 图片专用控制按钮 -->
      <template v-if="isImage">
        <el-tooltip content="放大" placement="top">
          <el-button circle @click="zoomIn" :disabled="zoomLevel >= 3">
            <el-icon><ZoomIn /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="缩小" placement="top">
          <el-button circle @click="zoomOut" :disabled="zoomLevel <= 0.2">
            <el-icon><ZoomOut /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="重置" placement="top">
          <el-button circle @click="resetZoom">
            <el-icon><RefreshRight /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="旋转" placement="top">
          <el-button circle @click="rotateImage">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </el-tooltip>
      </template>
      
      <!-- 下载按钮 -->
      <el-tooltip content="下载文件" placement="top">
        <el-button circle @click="downloadFile">
          <el-icon><Download /></el-icon>
        </el-button>
      </el-tooltip>
    </div>

    <!-- 预览加载中 -->
    <div v-if="loading" class="preview-loading">
      <el-skeleton animated :rows="15" />
    </div>

    <!-- 不支持预览 -->
    <div v-else-if="!previewSupported" class="unsupported-preview">
      <el-empty description="该文件类型暂不支持预览">
        <el-button type="primary" @click="downloadFile">下载文件</el-button>
      </el-empty>
    </div>

    <!-- 图片预览 -->
    <div 
      v-else-if="isImage" 
      class="image-preview-wrapper"
      :class="{ 'fullscreen': isFullscreen }"
      @wheel.prevent="handleMouseWheel"
    >
      <img 
        :src="previewUrl" 
        :alt="fileName"
        :style="{
          transform: `scale(${zoomLevel}) rotate(${rotation}deg)`,
          transition: isAnimating ? 'transform 0.3s' : 'none',
        }"
        @load="imageLoaded"
        ref="imageRef"
        draggable="false"
        @mousedown="startDrag"
        @mouseup="stopDrag"
        @mouseleave="stopDrag"
        @mousemove="onDrag"
      />
    </div>

    <!-- 视频预览 -->
    <div v-else-if="isVideo" class="video-preview-wrapper" :class="{ 'fullscreen-container': isFullscreen }">
      <video 
        controls 
        :src="previewUrl" 
        ref="videoRef" 
        class="video-player"
      >
        您的浏览器不支持 HTML5 视频播放
      </video>
    </div>

    <!-- 音频预览 -->
    <div v-else-if="isAudio" class="audio-preview-wrapper">
      <audio controls :src="previewUrl" ref="audioRef"></audio>
      <div class="audio-placeholder">
        <el-icon :size="64"><Headset /></el-icon>
        <p>{{ fileName }}</p>
      </div>
    </div>

    <!-- PDF预览 -->
    <div v-else-if="isPdf" class="pdf-preview-wrapper">
      <iframe 
        :src="previewUrl"
        ref="pdfRef"
        class="pdf-viewer"
        :class="{ 'fullscreen': isFullscreen }"
      ></iframe>
    </div>

    <!-- Office文档预览 -->
    <div v-else-if="isOffice" class="office-preview-wrapper">
      <iframe 
        :src="officePreviewUrl" 
        ref="officeRef"
        class="office-viewer"
        :class="{ 'fullscreen': isFullscreen }"
      ></iframe>
    </div>

    <!-- 3D模型预览 -->
    <div v-else-if="is3DModel" class="model-preview-wrapper">
      <div class="model-viewer-container" ref="modelViewerRef">
        <!-- 3D模型预览将在组件挂载后通过Three.js渲染 -->
        <div v-if="!modelLoaded" class="model-loading">
          <el-icon :size="64" class="rotating"><Loading /></el-icon>
          <p>3D模型加载中...</p>
        </div>
      </div>
      <div class="model-controls">
        <el-button-group>
          <el-button size="small" @click="rotateModel('left')">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <el-button size="small" @click="rotateModel('right')">
            <el-icon><ArrowRight /></el-icon>
          </el-button>
          <el-button size="small" @click="resetModelView">
            <el-icon><RefreshRight /></el-icon>
          </el-button>
        </el-button-group>
      </div>
    </div>

    <!-- CAD文件预览 -->
    <div v-else-if="isCAD" class="cad-preview-wrapper">
      <div class="cad-info">
        <el-alert
          title="CAD文件预览"
          type="info"
          :closable="false"
          description="CAD文件需要专业软件查看，您可以下载后使用AutoCAD或其他CAD软件打开。"
        />
      </div>
      <div class="cad-thumbnail" v-if="props.file.thumbnail">
        <img :src="props.file.thumbnail" alt="CAD预览缩略图" />
      </div>
      <div class="cad-actions">
        <el-button type="primary" @click="downloadFile">
          <el-icon><Download /></el-icon>下载CAD文件
        </el-button>
      </div>
    </div>

    <!-- 文本预览 -->
    <div v-else-if="isText" class="text-preview-wrapper">
      <!-- 代码预览 -->
      <div v-if="isCode" class="code-preview">
        <div class="code-header">
          <span class="file-name">{{ fileName }}</span>
          <div class="code-tools">
            <el-tooltip content="复制代码" placement="top">
              <el-button size="small" @click="copyText">
                <el-icon><DocumentCopy /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
        <div class="code-container" ref="codeRef"></div>
      </div>
      <!-- 普通文本预览 -->
      <div v-else class="text-content">
        <pre>{{ textContent }}</pre>
      </div>
    </div>

    <!-- Markdown预览 -->
    <div v-else-if="isMarkdown" class="markdown-preview-wrapper">
      <div class="markdown-container" ref="markdownRef"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, onBeforeUnmount } from 'vue';
import { ElMessage } from 'element-plus';
import { getFileDownloadUrl } from '../../api/file';
import type { FileInfo } from '../../types/file';
import type { FileType } from '../../types/common';
import 'highlight.js/styles/github.css'; // 引入代码高亮样式
import 'highlight.js/lib/common'; // 引入代码高亮库

// 定义组件属性
interface Props {
  file: FileInfo;
  previewUrl: string;
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
});

// 状态管理
const loading = ref(props.loading);
const previewContainer = ref<HTMLElement | null>(null);
const textContent = ref('');
const isFullscreen = ref(false);
const zoomLevel = ref(1);
const rotation = ref(0);
const isAnimating = ref(true);
const imageRef = ref<HTMLImageElement | null>(null);
const videoRef = ref<HTMLVideoElement | null>(null);
const audioRef = ref<HTMLAudioElement | null>(null);
const pdfRef = ref<HTMLIFrameElement | null>(null);
const officeRef = ref<HTMLIFrameElement | null>(null);
const codeRef = ref<HTMLElement | null>(null);
const markdownRef = ref<HTMLElement | null>(null);
const modelViewerRef = ref<HTMLElement | null>(null);
const modelLoaded = ref(false);

// 拖拽功能状态
const isDragging = ref(false);
const dragStart = ref({ x: 0, y: 0 });
const dragOffset = ref({ x: 0, y: 0 });

// 计算属性
const fileType = computed<FileType>(() => props.file?.fileType || 'unknown');
const fileName = computed(() => props.file?.fileName || '');
const fileExtension = computed(() => {
  if (!props.file || !props.file.fileName) return '';
  const name = props.file.fileName;
  const dotIndex = name.lastIndexOf('.');
  return dotIndex > 0 ? name.substring(dotIndex + 1).toLowerCase() : '';
});

// 判断文件类型
const isImage = computed(() => {
  if (fileType.value === 'image') return true;
  // 检查扩展名
  const imgExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'ico', 'tiff', 'tif'];
  return imgExts.includes(fileExtension.value);
});

const isVideo = computed(() => {
  if (fileType.value === 'video') return true;
  // 检查扩展名
  const videoExts = ['mp4', 'webm', 'ogg', 'mov', 'avi', 'wmv', 'flv', 'mkv', 'm4v', '3gp'];
  return videoExts.includes(fileExtension.value);
});

const isAudio = computed(() => {
  if (fileType.value === 'audio') return true;
  // 检查扩展名
  const audioExts = ['mp3', 'wav', 'ogg', 'flac', 'aac', 'm4a', 'wma'];
  return audioExts.includes(fileExtension.value);
});

const isPdf = computed(() => {
  if (props.file?.type === 'application/pdf') return true;
  return fileExtension.value === 'pdf';
});

const isText = computed(() => {
  if (!props.file) return false;
  // 检查MIME类型
  const textMimes = [
    'text/plain', 'text/html', 'text/css', 'text/javascript',
    'application/json', 'application/xml', 'text/csv', 'text/x-log',
    'text/x-markdown', 'text/markdown'
  ];
  if (textMimes.includes(props.file.type)) return true;
  
  // 检查扩展名
  return codeFileTypes.includes(fileExtension.value) || ['txt', 'log', 'ini', 'conf'].includes(fileExtension.value);
});

const isCode = computed(() => codeFileTypes.includes(fileExtension.value));

const isMarkdown = computed(() => {
  if (['text/markdown', 'text/x-markdown'].includes(props.file?.type || '')) return true;
  return ['md', 'markdown', 'mdown', 'mkd'].includes(fileExtension.value);
});

const isOffice = computed(() => {
  // Word文档
  const wordExts = ['doc', 'docx', 'docm', 'dot', 'dotx', 'dotm', 'rtf'];
  // Excel表格
  const excelExts = ['xls', 'xlsx', 'xlsm', 'xlt', 'xltx', 'xltm', 'xlsb', 'csv'];
  // PowerPoint演示文稿
  const pptExts = ['ppt', 'pptx', 'pptm', 'pot', 'potx', 'potm', 'pps', 'ppsx', 'ppsm'];
  
  const officeExts = [...wordExts, ...excelExts, ...pptExts];
  return officeExts.includes(fileExtension.value);
});

// 3D模型文件
const is3DModel = computed(() => {
  const modelExts = ['obj', 'stl', 'fbx', 'gltf', 'glb', '3ds', 'dae', 'ply'];
  return modelExts.includes(fileExtension.value);
});

// CAD文件
const isCAD = computed(() => {
  const cadExts = ['dwg', 'dxf', 'dwf', 'ifc', 'rvt', 'rfa'];
  return cadExts.includes(fileExtension.value);
});

// 可以使用全屏的文件类型
const canFullscreen = computed(() => {
  return isImage.value || isVideo.value || isPdf.value || isOffice.value || is3DModel.value;
});

// 是否支持预览
const previewSupported = computed(() => {
  return isImage.value || isVideo.value || isAudio.value || isPdf.value || 
         isText.value || isOffice.value || isMarkdown.value || is3DModel.value || isCAD.value;
});

// 代码文件类型列表
const codeFileTypes = [
  // Web开发
  'js', 'ts', 'jsx', 'tsx', 'html', 'css', 'scss', 'less', 'vue', 'svelte',
  'json', 'xml', 'yaml', 'yml', 'toml', 'graphql', 'wasm',
  
  // 后端/系统编程
  'php', 'py', 'java', 'c', 'cpp', 'cc', 'cxx', 'h', 'hpp', 'hxx',
  'cs', 'go', 'rb', 'rs', 'swift', 'kt', 'scala', 'groovy', 'perl', 'pl',
  'lua', 'r', 'dart', 'ex', 'exs', 'erl', 'hrl', 'clj', 'cljs',
  
  // 数据库/查询语言
  'sql', 'mysql', 'pgsql', 'mongodb', 'hql',
  
  // 配置/构建文件
  'ini', 'conf', 'config', 'properties', 'env', 'htaccess',
  'gradle', 'dockerfile', 'makefile', 'cmake',
  
  // Shell脚本
  'sh', 'bash', 'zsh', 'bat', 'cmd', 'ps1',
  
  // 其他
  'tex', 'diff', 'patch'
];

// 是否支持全屏API
const isFullscreenSupported = computed(() => {
  return document.fullscreenEnabled || 
         // @ts-ignore
         document.webkitFullscreenEnabled || 
         // @ts-ignore
         document.mozFullScreenEnabled || 
         // @ts-ignore
         document.msFullscreenEnabled;
});

// Office文档预览URL
const officePreviewUrl = computed(() => {
  // 检查是否为本地开发环境
  const isLocalDev = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
  
  // 如果是本地开发环境，直接使用预览URL
  if (isLocalDev) {
    // 对于本地环境，我们可以使用内嵌的Google Docs Viewer作为备选
    return `https://docs.google.com/viewer?url=${encodeURIComponent(props.previewUrl)}&embedded=true`;
  }
  
  // 生产环境使用Microsoft Office Online Viewer
  return `https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(props.previewUrl)}`;
});

// 初始化
onMounted(async () => {
  if (isText.value) {
    await fetchTextContent();
  }
  if (isCode.value && codeRef.value) {
    await initCodeHighlighting();
  }
  if (isMarkdown.value && markdownRef.value) {
    await initMarkdownRenderer();
  }
  if (is3DModel.value && modelViewerRef.value) {
    await init3DModelViewer();
  }

  // 监听全屏状态变化
  document.addEventListener('fullscreenchange', handleFullscreenChange);
  document.addEventListener('webkitfullscreenchange', handleFullscreenChange);
  document.addEventListener('mozfullscreenchange', handleFullscreenChange);
  document.addEventListener('MSFullscreenChange', handleFullscreenChange);
});

// 在组件卸载前清理资源
onBeforeUnmount(() => {
  // 移除所有全屏监听器
  document.removeEventListener('fullscreenchange', handleFullscreenChange);
  document.removeEventListener('webkitfullscreenchange', handleFullscreenChange);
  document.removeEventListener('mozfullscreenchange', handleFullscreenChange);
  document.removeEventListener('MSFullscreenChange', handleFullscreenChange);

  // 确保退出全屏
  if (isFullscreen.value) {
    // 对于视频，只需要重置状态
    if (isVideo.value) {
      isFullscreen.value = false;
    } else {
      try {
        if (document.exitFullscreen) {
          document.exitFullscreen();
        } else {
          // 使用any类型绕过TypeScript的类型检查，处理不同浏览器的前缀
          const doc = document as any;
          if (doc.webkitExitFullscreen) {
            doc.webkitExitFullscreen();
          } else if (doc.mozCancelFullScreen) {
            doc.mozCancelFullScreen();
          } else if (doc.msExitFullscreen) {
            doc.msExitFullscreen();
          }
        }
      } catch (error) {
        console.error('Error exiting fullscreen:', error);
      }
    }
  }

  // 清除所有引用
  previewContainer.value = null;
  imageRef.value = null;
  videoRef.value = null;
  audioRef.value = null;
  pdfRef.value = null;
  codeRef.value = null;
  markdownRef.value = null;
  officeRef.value = null;
  modelViewerRef.value = null;
});

// 监听文件变化
watch(() => props.file, async () => {
  // 重置预览状态
  zoomLevel.value = 1;
  rotation.value = 0;
  isFullscreen.value = false;
  
  if (isText.value) {
    await fetchTextContent();
  }
  if (isCode.value && codeRef.value) {
    await initCodeHighlighting();
  }
  if (isMarkdown.value && markdownRef.value) {
    await initMarkdownRenderer();
  }
}, { deep: true });

// 获取文本内容
const fetchTextContent = async () => {
  try {
    const response = await fetch(props.previewUrl);
    textContent.value = await response.text();
    
    await nextTick();
    
    if (isCode.value && codeRef.value) {
      initCodeHighlighting();
    } else if (isMarkdown.value && markdownRef.value) {
      initMarkdownRenderer();
    }
  } catch (error) {
    console.error('Failed to fetch text content', error);
    ElMessage.error('获取文本内容失败');
  }
};

// 初始化代码高亮
const initCodeHighlighting = async () => {
  if (!codeRef.value || !textContent.value) return;

  try {
    // 动态导入highlight.js
    const hljs = await import('highlight.js/lib/core');
    const { default: highlightjs } = hljs;
    
    // 设置代码内容
    codeRef.value.innerHTML = `<pre><code class="language-${fileExtension.value}">${textContent.value}</code></pre>`;
    
    // 应用高亮
    const codeBlocks = codeRef.value.querySelectorAll('pre code');
    codeBlocks.forEach((block) => {
      highlightjs.highlightElement(block as HTMLElement);
    });
  } catch (error) {
    console.error('Failed to initialize code highlighting', error);
  }
};

// 初始化Markdown渲染
const initMarkdownRenderer = async () => {
  if (!markdownRef.value || !textContent.value) return;

  try {
    // 动态导入marked
    const { marked } = await import('marked');
    
    // 设置安全选项
    marked.setOptions({
      breaks: true,
      gfm: true,
      // marked v1.0.0之后的版本选项有所不同
      // 使用类型断言来处理不同版本的兼容性
    } as any);
    
    // 渲染Markdown
    // 使用as string确保类型兼容
    const renderedContent = await marked(textContent.value) as string;
    markdownRef.value.innerHTML = renderedContent;
    
    // 高亮代码块
    const codeBlocks = markdownRef.value.querySelectorAll('pre code');
    if (codeBlocks.length > 0) {
      const hljs = await import('highlight.js/lib/core');
      const { default: highlightjs } = hljs;
      
      codeBlocks.forEach((block) => {
        highlightjs.highlightElement(block as HTMLElement);
      });
    }
  } catch (error) {
    console.error('Failed to initialize markdown renderer', error);
    // 如果渲染失败，使用简单的预格式化文本显示
    if (markdownRef.value) {
      markdownRef.value.innerHTML = `<pre>${textContent.value}</pre>`;
    }
  }
};

// 切换全屏
const toggleFullscreen = () => {
  if (!previewContainer.value) return;
  
  // 对于视频，我们使用CSS控制全屏
  if (isVideo.value) {
    isFullscreen.value = !isFullscreen.value;
    
    // 如果是进入全屏，尝试自动播放视频
    if (isFullscreen.value && videoRef.value) {
      try {
        videoRef.value.play().catch(() => {
          // 自动播放可能被浏览器阻止，这里不做特殊处理
        });
      } catch (e) {
        // 忽略错误
      }
    }
    return;
  }
  
  if (!isFullscreen.value) {
    // 进入全屏
    const element = getFullscreenElement();
    if (element) {
      if (element.requestFullscreen) {
        element.requestFullscreen();
      } else {
        // 使用any类型绕过TypeScript的类型检查，处理不同浏览器的前缀
        const el = element as any;
        if (el.webkitRequestFullscreen) {
          el.webkitRequestFullscreen();
        } else if (el.mozRequestFullScreen) {
          el.mozRequestFullScreen();
        } else if (el.msRequestFullscreen) {
          el.msRequestFullscreen();
        }
      }
    }
  } else {
    // 退出全屏
    if (document.exitFullscreen) {
      document.exitFullscreen();
    } else {
      // 使用any类型绕过TypeScript的类型检查，处理不同浏览器的前缀
      const doc = document as any;
      if (doc.webkitExitFullscreen) {
        doc.webkitExitFullscreen();
      } else if (doc.mozCancelFullScreen) {
        doc.mozCancelFullScreen();
      } else if (doc.msExitFullscreen) {
        doc.msExitFullscreen();
      }
    }
  }
};

// 处理全屏状态变化
const handleFullscreenChange = () => {
  // 如果是视频，我们已经在toggleFullscreen中处理了isFullscreen.value
  // 这里只处理非视频元素的全屏状态变化
  if (!isVideo.value) {
    isFullscreen.value = 
      document.fullscreenElement !== null || 
      // @ts-ignore
      document.webkitFullscreenElement !== null || 
      // @ts-ignore
      document.mozFullScreenElement !== null || 
      // @ts-ignore
      document.msFullscreenElement !== null;
  }
};

// 获取应该全屏的元素
const getFullscreenElement = () => {
  if (isImage.value) {
    return previewContainer.value;
  } else if (isVideo.value) {
    // 对于视频，我们使用CSS控制全屏，而不是真正的全屏API
    return null;
  } else if (isPdf.value) {
    return pdfRef.value;
  } else if (isOffice.value) {
    return officeRef.value;
  }
  return previewContainer.value;
};

// 下载文件
const downloadFile = async () => {
  try {
    const url = await getFileDownloadUrl(props.file.id);
    const a = document.createElement('a');
    a.href = url as unknown as string;
    a.download = props.file.fileName;
    a.click();
  } catch (error) {
    console.error('Failed to download file', error);
    ElMessage.error('下载文件失败');
  }
};

// 图片放大
const zoomIn = () => {
  zoomLevel.value = parseFloat((zoomLevel.value + 0.2).toFixed(1));
  isAnimating.value = true;
};

// 图片缩小
const zoomOut = () => {
  zoomLevel.value = parseFloat((zoomLevel.value - 0.2).toFixed(1));
  if (zoomLevel.value < 0.2) zoomLevel.value = 0.2;
  isAnimating.value = true;
};

// 重置图片缩放
const resetZoom = () => {
  zoomLevel.value = 1;
  rotation.value = 0;
  dragOffset.value = { x: 0, y: 0 };
  isAnimating.value = true;
};

// 旋转图片
const rotateImage = () => {
  rotation.value = (rotation.value + 90) % 360;
  isAnimating.value = true;
};

// 鼠标滚轮缩放图片
const handleMouseWheel = (e: WheelEvent) => {
  if (!isImage.value) return;
  
  // 阻止默认滚动行为
  e.preventDefault();
  
  if (e.deltaY < 0) {
    // 向上滚动，放大
    zoomLevel.value = Math.min(3, parseFloat((zoomLevel.value + 0.1).toFixed(1)));
  } else {
    // 向下滚动，缩小
    zoomLevel.value = Math.max(0.2, parseFloat((zoomLevel.value - 0.1).toFixed(1)));
  }
  
  isAnimating.value = false;
};

// 图片加载完成处理
const imageLoaded = () => {
  // 图片加载完成，停止加载状态
  loading.value = false;
};

// 开始拖拽
const startDrag = (e: MouseEvent) => {
  if (!isImage.value || zoomLevel.value <= 1) return;
  
  isDragging.value = true;
  dragStart.value = { x: e.clientX, y: e.clientY };
  
  // 禁用动画以实现平滑拖动
  isAnimating.value = false;
};

// 停止拖拽
const stopDrag = () => {
  isDragging.value = false;
};

// 拖拽处理
const onDrag = (e: MouseEvent) => {
  if (!isDragging.value) return;
  
  const dx = e.clientX - dragStart.value.x;
  const dy = e.clientY - dragStart.value.y;
  
  dragOffset.value = {
    x: dragOffset.value.x + dx,
    y: dragOffset.value.y + dy
  };
  
  dragStart.value = { x: e.clientX, y: e.clientY };
};

// 复制文本内容
const copyText = () => {
  if (!textContent.value) return;
  
  navigator.clipboard.writeText(textContent.value)
    .then(() => {
      ElMessage.success('文本内容已复制');
    })
    .catch(() => {
      ElMessage.error('复制失败');
    });
};

// 初始化3D模型查看器
const init3DModelViewer = async () => {
  if (!modelViewerRef.value || !props.previewUrl) return;
  
  try {
    // 这里可以动态导入Three.js库
    // 实际实现可能需要根据具体的3D库调整
    console.log('初始化3D模型查看器:', props.previewUrl);
    
    // 模拟加载过程
    setTimeout(() => {
      modelLoaded.value = true;
    }, 1000);
    
    // 注意：完整实现应该使用Three.js或其他3D库加载和渲染模型
    // 例如：
    // const THREE = await import('three');
    // const { GLTFLoader } = await import('three/examples/jsm/loaders/GLTFLoader');
    // 
    // const scene = new THREE.Scene();
    // const camera = new THREE.PerspectiveCamera(75, modelViewerRef.value.clientWidth / modelViewerRef.value.clientHeight, 0.1, 1000);
    // const renderer = new THREE.WebGLRenderer();
    // renderer.setSize(modelViewerRef.value.clientWidth, modelViewerRef.value.clientHeight);
    // modelViewerRef.value.appendChild(renderer.domElement);
    // 
    // const loader = new GLTFLoader();
    // loader.load(props.previewUrl, (gltf) => {
    //   scene.add(gltf.scene);
    //   modelLoaded.value = true;
    // });
  } catch (error) {
    console.error('Failed to initialize 3D model viewer', error);
  }
};

// 旋转3D模型
const rotateModel = (direction: 'left' | 'right') => {
  console.log(`旋转模型: ${direction}`);
  // 实际实现应该控制Three.js中的模型旋转
};

// 重置3D模型视图
const resetModelView = () => {
  console.log('重置模型视图');
  // 实际实现应该重置Three.js中的相机位置和模型旋转
};
</script>

<style scoped>
.advanced-preview-container {
  position: relative;
  width: 100%;
  min-height: 400px;
  height: auto;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background-color: #f6f8fa;
}

.preview-toolbar {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 100;
  display: flex;
  gap: 8px;
  background-color: rgba(255, 255, 255, 0.7);
  border-radius: 24px;
  padding: 6px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.preview-loading {
  width: 100%;
  height: 100%;
  min-height: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.image-preview-wrapper {
  width: 100%;
  height: auto;
  min-height: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  position: relative;
}

.image-preview-wrapper img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  cursor: move;
  position: relative;
  z-index: 1;
}

.video-preview-wrapper {
  width: 100%;
  height: auto;
  min-height: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.video-player {
  max-width: 100%;
  width: auto;
  height: auto;
  min-height: 350px;
  z-index: 1;
}

/* 全屏容器样式 */
.fullscreen-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw !important;
  height: 100vh !important;
  z-index: 9999;
  background-color: #000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.fullscreen-container .video-player {
  max-width: 100%;
  max-height: 100vh;
  width: auto;
  height: auto;
}

.audio-preview-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: auto;
  min-height: 300px;
  padding: 20px 0;
  position: relative;
}

.audio-placeholder {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #606266;
}

.pdf-preview-wrapper {
  width: 100%;
  height: auto;
  min-height: 500px;
  position: relative;
}

.pdf-viewer {
  width: 100%;
  height: 500px;
  min-height: 500px;
  border: none;
}

.office-preview-wrapper {
  width: 100%;
  height: auto;
  min-height: 500px;
  position: relative;
}

.office-viewer {
  width: 100%;
  height: 500px;
  min-height: 500px;
  border: none;
}

.text-preview-wrapper {
  width: 100%;
  height: auto;
  min-height: 400px;
  overflow: auto;
  padding: 16px;
  background-color: #fff;
  border-radius: 4px;
  position: relative;
}

.text-content {
  white-space: pre-wrap;
  font-family: monospace;
}

.text-content pre {
  margin: 0;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 4px;
  overflow-x: auto;
}

.code-preview {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 400px;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background-color: #f1f1f1;
  border-top-left-radius: 4px;
  border-top-right-radius: 4px;
  border-bottom: 1px solid #e0e0e0;
  position: sticky;
  top: 0;
  z-index: 2;
}

.file-name {
  font-weight: bold;
  color: #333;
}

.code-tools {
  display: flex;
  gap: 8px;
}

.code-container {
  flex: 1;
  overflow: auto;
  background-color: #ffffff;
  padding: 0;
  min-height: 350px;
}

.code-container pre {
  margin: 0;
  padding: 16px;
}

.markdown-preview-wrapper {
  width: 100%;
  height: auto;
  min-height: 400px;
  overflow: auto;
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
  position: relative;
}

.markdown-container {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  color: #24292e;
  line-height: 1.6;
}

.markdown-container h1,
.markdown-container h2 {
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}

.markdown-container pre {
  background-color: #f6f8fa;
  border-radius: 4px;
  padding: 16px;
  overflow: auto;
}

.markdown-container code {
  background-color: rgba(27, 31, 35, 0.05);
  border-radius: 3px;
  padding: 0.2em 0.4em;
  font-family: SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;
}

.markdown-container pre code {
  background-color: transparent;
  padding: 0;
}

.markdown-container blockquote {
  margin: 0;
  padding: 0 1em;
  color: #6a737d;
  border-left: 0.25em solid #dfe2e5;
}

.unsupported-preview {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw !important;
  height: 100vh !important;
  max-height: none !important;
  z-index: 9999;
  background-color: #000;
}

/* 3D模型预览样式 */
.model-preview-wrapper {
  width: 100%;
  height: auto;
  min-height: 500px;
  display: flex;
  flex-direction: column;
  position: relative;
}

.model-viewer-container {
  flex: 1;
  min-height: 450px;
  background-color: #1a1a1a;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

.model-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #ffffff;
}

.rotating {
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.model-controls {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background-color: rgba(255, 255, 255, 0.7);
  border-radius: 24px;
  padding: 6px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

/* CAD文件预览样式 */
.cad-preview-wrapper {
  width: 100%;
  height: 100%;
  min-height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  gap: 20px;
}

.cad-info {
  width: 100%;
  max-width: 600px;
}

.cad-thumbnail {
  width: 100%;
  max-width: 400px;
  height: auto;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.cad-thumbnail img {
  max-width: 100%;
  max-height: 300px;
  object-fit: contain;
}

.cad-actions {
  margin-top: 20px;
}
</style> 