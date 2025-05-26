<script setup lang="ts">
// 云盘应用主组件
</script>

<template>
  <div class="app-container">
    <!-- 路由视图 -->
    <router-view />
  </div>
</template>

<style>
/* 全局样式 */
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}

#app {
  height: 100vh;
}

.app-container {
  height: 100%;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-thumb {
  background-color: #c0c4cc;
  border-radius: 4px;
}

::-webkit-scrollbar-track {
  background-color: #f0f2f5;
}
</style>
