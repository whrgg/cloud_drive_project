<template>
  <div class="image-viewer" ref="viewerContainer">
    <!-- 顶部工具栏 -->
    <div class="toolbar" v-show="showToolbar">
      <div class="left">
        <el-tooltip content="退出查看器" placement="bottom">
          <el-button circle @click="$emit('close')">
            <el-icon><Close /></el-icon>
          </el-button>
        </el-tooltip>
        <span class="title">{{ currentIndex + 1 }}/{{ images.length }} - {{ currentImage?.name }}</span>
      </div>
      <div class="right">
        <el-tooltip content="旋转" placement="bottom">
          <el-button circle @click="rotateImage">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏'" placement="bottom">
          <el-button circle @click="toggleFullscreen">
            <el-icon v-if="isFullscreen"><FullscreenExit /></el-icon>
            <el-icon v-else><FullScreen /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="下载" placement="bottom">
          <el-button circle @click="downloadImage">
            <el-icon><Download /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <!-- 缩放控制 -->
    <div class="zoom-controls" v-show="showToolbar">
      <el-tooltip content="缩小" placement="left">
        <el-button circle @click="zoomOut" :disabled="zoomLevel <= 0.2">
          <el-icon><ZoomOut /></el-icon>
        </el-button>
      </el-tooltip>
      <div class="zoom-level">{{ Math.round(zoomLevel * 100) }}%</div>
      <el-tooltip content="放大" placement="right">
        <el-button circle @click="zoomIn" :disabled="zoomLevel >= 3">
          <el-icon><ZoomIn /></el-icon>
        </el-button>
      </el-tooltip>
      <el-tooltip content="重置" placement="right">
        <el-button circle @click="resetZoom">
          <el-icon><RefreshRight /></el-icon>
        </el-button>
      </el-tooltip>
    </div>

    <!-- 导航按钮 -->
    <div class="navigation" v-if="images.length > 1 && showToolbar">
      <el-button 
        class="prev-btn" 
        circle 
        @click="prevImage" 
        :disabled="currentIndex === 0"
      >
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <el-button 
        class="next-btn" 
        circle 
        @click="nextImage" 
        :disabled="currentIndex === images.length - 1"
      >
        <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>

    <!-- 图片容器 -->
    <div 
      class="image-container"
      @mousedown="startDrag"
      @mouseup="stopDrag"
      @mouseleave="stopDrag"
      @mousemove="onDrag"
      @wheel.prevent="handleMouseWheel"
      @click="toggleToolbar"
    >
      <div 
        class="loading-indicator" 
        v-if="loading"
      >
        <el-icon class="is-loading" :size="48"><Loading /></el-icon>
      </div>
      <img 
        v-show="!loading && currentImage"
        :src="currentImage?.url" 
        :alt="currentImage?.name" 
        ref="imageRef"
        @load="onImageLoad"
        :style="{
          transform: `translate(${dragOffset.x}px, ${dragOffset.y}px) scale(${zoomLevel}) rotate(${rotation}deg)`,
          transition: isAnimating ? 'transform 0.2s' : 'none'
        }"
      />
    </div>

    <!-- 缩略图预览区 -->
    <div class="thumbnail-strip" v-if="images.length > 1 && showToolbar">
      <div 
        v-for="(img, index) in images" 
        :key="index"
        class="thumbnail"
        :class="{ active: index === currentIndex }"
        @click="goToImage(index)"
      >
        <img :src="img.url" :alt="img.name" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue';
import { ElMessage } from 'element-plus';

// 定义图片接口
interface ImageItem {
  url: string;
  name: string;
  id?: number;
}

// 定义组件属性
interface Props {
  images: ImageItem[];
  initialIndex?: number;
  showThumbnails?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  initialIndex: 0,
  showThumbnails: true
});

// 定义事件
const emit = defineEmits(['close', 'change']);

// 状态
const viewerContainer = ref<HTMLElement | null>(null);
const imageRef = ref<HTMLImageElement | null>(null);
const currentIndex = ref(props.initialIndex);
const loading = ref(true);
const isFullscreen = ref(false);
const showToolbar = ref(true);
const zoomLevel = ref(1);
const rotation = ref(0);
const isAnimating = ref(true);
const dragOffset = ref({ x: 0, y: 0 });
const isDragging = ref(false);
const dragStart = ref({ x: 0, y: 0 });

// 计算属性
const currentImage = computed(() => {
  return props.images[currentIndex.value] || null;
});

// 初始化
onMounted(() => {
  // 添加键盘事件监听
  window.addEventListener('keydown', handleKeyDown);
  
  // 添加全屏变化事件监听
  document.addEventListener('fullscreenchange', handleFullscreenChange);
  document.addEventListener('webkitfullscreenchange', handleFullscreenChange);
  document.addEventListener('mozfullscreenchange', handleFullscreenChange);
  document.addEventListener('MSFullscreenChange', handleFullscreenChange);
  
  // 初始加载图片
  if (currentImage.value) {
    preloadImages();
  }
});

// 清理
onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeyDown);
  document.removeEventListener('fullscreenchange', handleFullscreenChange);
  document.removeEventListener('webkitfullscreenchange', handleFullscreenChange);
  document.removeEventListener('mozfullscreenchange', handleFullscreenChange);
  document.removeEventListener('MSFullscreenChange', handleFullscreenChange);
  
  // 如果在全屏模式下，退出全屏
  if (isFullscreen.value) {
    exitFullscreen();
  }
});

// 切换到上一张图片
const prevImage = () => {
  if (currentIndex.value > 0) {
    goToImage(currentIndex.value - 1);
  }
};

// 切换到下一张图片
const nextImage = () => {
  if (currentIndex.value < props.images.length - 1) {
    goToImage(currentIndex.value + 1);
  }
};

// 跳转到指定图片
const goToImage = (index: number) => {
  loading.value = true;
  currentIndex.value = index;
  // 重置缩放和旋转
  resetView();
  // 触发变更事件
  emit('change', index);
};

// 图片加载完成
const onImageLoad = () => {
  loading.value = false;
};

// 重置视图
const resetView = () => {
  zoomLevel.value = 1;
  rotation.value = 0;
  dragOffset.value = { x: 0, y: 0 };
  isAnimating.value = true;
};

// 放大图片
const zoomIn = () => {
  zoomLevel.value = Math.min(3, parseFloat((zoomLevel.value + 0.2).toFixed(1)));
  isAnimating.value = true;
};

// 缩小图片
const zoomOut = () => {
  zoomLevel.value = Math.max(0.2, parseFloat((zoomLevel.value - 0.2).toFixed(1)));
  isAnimating.value = true;
};

// 重置缩放
const resetZoom = () => {
  resetView();
};

// 旋转图片
const rotateImage = () => {
  rotation.value = (rotation.value + 90) % 360;
  isAnimating.value = true;
};

// 切换全屏
const toggleFullscreen = () => {
  if (!isFullscreen.value) {
    enterFullscreen();
  } else {
    exitFullscreen();
  }
};

// 进入全屏
const enterFullscreen = () => {
  if (!viewerContainer.value) return;
  
  if (viewerContainer.value.requestFullscreen) {
    viewerContainer.value.requestFullscreen();
  } else if ((viewerContainer.value as any).webkitRequestFullscreen) {
    (viewerContainer.value as any).webkitRequestFullscreen();
  } else if ((viewerContainer.value as any).mozRequestFullScreen) {
    (viewerContainer.value as any).mozRequestFullScreen();
  } else if ((viewerContainer.value as any).msRequestFullscreen) {
    (viewerContainer.value as any).msRequestFullscreen();
  }
};

// 退出全屏
const exitFullscreen = () => {
  if (document.exitFullscreen) {
    document.exitFullscreen();
  } else if ((document as any).webkitExitFullscreen) {
    (document as any).webkitExitFullscreen();
  } else if ((document as any).mozCancelFullScreen) {
    (document as any).mozCancelFullScreen();
  } else if ((document as any).msExitFullscreen) {
    (document as any).msExitFullscreen();
  }
};

// 处理全屏状态变化
const handleFullscreenChange = () => {
  isFullscreen.value = 
    document.fullscreenElement !== null || 
    (document as any).webkitFullscreenElement !== null || 
    (document as any).mozFullScreenElement !== null || 
    (document as any).msFullscreenElement !== null;
};

// 下载当前图片
const downloadImage = () => {
  if (!currentImage.value) return;
  
  const link = document.createElement('a');
  link.href = currentImage.value.url;
  link.download = currentImage.value.name;
  link.target = '_blank';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

// 预加载图片
const preloadImages = () => {
  // 预加载当前图片的前一张和后一张
  const preloadIndexes = [
    currentIndex.value - 1,
    currentIndex.value + 1
  ].filter(i => i >= 0 && i < props.images.length);
  
  preloadIndexes.forEach(index => {
    const img = new Image();
    img.src = props.images[index].url;
  });
};

// 处理键盘事件
const handleKeyDown = (e: KeyboardEvent) => {
  switch (e.key) {
    case 'ArrowLeft':
      prevImage();
      break;
    case 'ArrowRight':
      nextImage();
      break;
    case 'ArrowUp':
      zoomIn();
      break;
    case 'ArrowDown':
      zoomOut();
      break;
    case 'Escape':
      if (isFullscreen.value) {
        exitFullscreen();
      } else {
        emit('close');
      }
      break;
    case ' ':
      resetView();
      break;
    case 'r':
    case 'R':
      rotateImage();
      break;
    case 'f':
    case 'F':
      toggleFullscreen();
      break;
  }
};

// 切换工具栏显示
const toggleToolbar = () => {
  showToolbar.value = !showToolbar.value;
};

// 鼠标滚轮缩放图片
const handleMouseWheel = (e: WheelEvent) => {
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

// 开始拖拽
const startDrag = (e: MouseEvent) => {
  if (zoomLevel.value <= 1) return;
  
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
</script>

<style scoped>
.image-viewer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.9);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  user-select: none;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  padding: 16px;
  color: #fff;
  background-color: rgba(0, 0, 0, 0.5);
  transition: opacity 0.3s;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
}

.left, .right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title {
  font-size: 16px;
  margin-left: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 300px;
}

.zoom-controls {
  position: absolute;
  bottom: 100px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: rgba(0, 0, 0, 0.5);
  padding: 8px 16px;
  border-radius: 24px;
  z-index: 10;
  transition: opacity 0.3s;
}

.zoom-level {
  color: #fff;
  min-width: 50px;
  text-align: center;
  font-size: 14px;
}

.navigation {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  padding: 0 20px;
  transform: translateY(-50%);
  z-index: 10;
  pointer-events: none;
}

.prev-btn, .next-btn {
  pointer-events: auto;
  background-color: rgba(0, 0, 0, 0.5);
  color: #fff;
}

.image-container {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  cursor: move;
  position: relative;
}

.loading-indicator {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #fff;
}

.image-container img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transform-origin: center;
}

.thumbnail-strip {
  height: 80px;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  gap: 8px;
  padding: 10px;
  overflow-x: auto;
  overflow-y: hidden;
  z-index: 10;
}

.thumbnail {
  height: 60px;
  min-width: 60px;
  border: 2px solid transparent;
  opacity: 0.6;
  transition: opacity 0.3s, border-color 0.3s;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
}

.thumbnail:hover {
  opacity: 0.9;
}

.thumbnail.active {
  border-color: #409EFF;
  opacity: 1;
}

.thumbnail img {
  max-height: 100%;
  max-width: 100%;
  object-fit: contain;
}

/* 适配移动端 */
@media (max-width: 768px) {
  .toolbar, .zoom-controls {
    padding: 8px;
  }
  
  .title {
    max-width: 150px;
    font-size: 14px;
  }
  
  .navigation {
    padding: 0 10px;
  }
  
  .thumbnail-strip {
    height: 60px;
  }
  
  .thumbnail {
    height: 40px;
    min-width: 40px;
  }
}
</style> 