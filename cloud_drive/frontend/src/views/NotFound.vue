<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <div class="not-found-icon">
        <el-icon><Warning /></el-icon>
      </div>
      <h1>404</h1>
      <p>页面不存在</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const goHome = () => {
  router.push('/');
};
</script>

<style scoped>
.not-found-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}

.not-found-content {
  text-align: center;
  padding: 40px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.not-found-icon {
  font-size: 64px;
  color: #f56c6c;
  margin-bottom: 16px;
}

h1 {
  font-size: 48px;
  color: #303133;
  margin: 0;
  margin-bottom: 16px;
}

p {
  font-size: 18px;
  color: #606266;
  margin-bottom: 24px;
}
</style> 