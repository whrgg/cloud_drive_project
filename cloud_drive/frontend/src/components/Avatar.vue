<template>
  <div class="avatar-wrapper" :style="{ width: size + 'px', height: size + 'px' }">
    <img
      :src="processedSrc"
      :alt="alt"
      class="avatar-image"
      @error="handleImageError"
    />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import defaultAvatarImg from '@/assets/default-avatar.png';

const props = defineProps({
  src: {
    type: String,
    default: ''
  },
  size: {
    type: Number,
    default: 40
  },
  alt: {
    type: String,
    default: '用户头像'
  }
});

// 默认头像
const defaultAvatar = defaultAvatarImg;

// 处理头像URL
const processedSrc = computed(() => {
  if (!props.src) {
    return defaultAvatar; // 没有头像URL时显示默认头像
  }
  
  // 已经是有效URL，直接返回
  if (props.src.startsWith('http://') || props.src.startsWith('https://')) {
    // 强制刷新头像避免缓存问题
    if (props.src.includes('?')) {
      return props.src; // 已有查询参数，无需添加
    } else {
      // 添加时间戳参数避免缓存
      return `${props.src}?t=${new Date().getTime()}`;
    }
  }
  
  // 如果是相对路径，转换为绝对路径
  return defaultAvatar; // 如果无法解析，返回默认头像
});

// 处理图片加载错误
const handleImageError = () => {
  console.warn('头像加载失败，使用默认头像');
  // 这里不修改原始src，而是让computed属性处理回退逻辑
};
</script>

<style scoped>
.avatar-wrapper {
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style> 