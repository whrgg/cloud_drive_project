<template>
  <div class="app-layout">
    <header class="app-header">
      <!-- 页面头部区域 -->
      <div class="logo">
        <h1>云盘系统</h1>
      </div>
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文件..."
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="handleSearch">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>
      <div class="user-menu">
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user-avatar">
            <el-avatar :size="32" :src="userAvatar">{{ userInitials }}</el-avatar>
            <span>{{ userName }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="settings">个人设置</el-dropdown-item>
              <el-dropdown-item command="about">关于系统</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    
    <div class="app-container">
      <aside class="app-sidebar">
        <!-- 侧边栏导航 -->
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          router
        >
          <el-menu-item index="/">
            <el-icon><icon-menu-home /></el-icon>
            <span>我的云盘</span>
          </el-menu-item>
          <el-menu-item index="/starred">
            <el-icon><icon-menu-star /></el-icon>
            <span>收藏夹</span>
          </el-menu-item>
          <el-menu-item index="/shares">
            <el-icon><icon-menu-share /></el-icon>
            <span>我的分享</span>
          </el-menu-item>
          <el-menu-item index="/recycle">
            <el-icon><icon-menu-delete /></el-icon>
            <span>回收站</span>
          </el-menu-item>
          <el-menu-item index="/settings">
            <el-icon><icon-menu-setting /></el-icon>
            <span>个人设置</span>
          </el-menu-item>
        </el-menu>
        
        <!-- 存储空间使用情况 -->
        <div class="storage-info">
          <el-progress :percentage="storageUsage" :format="storageFormat" />
          <div class="storage-text">
            已用 {{ usedSpace }} / 总共 {{ totalSpace }}
          </div>
        </div>
      </aside>
      
      <main class="app-content">
        <!-- 主内容区域 -->
        <router-view v-slot="{ Component }">
          <keep-alive :include="cachedViews">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getUserInfo, getUserStorageInfo, logout } from '../../api/user';

// 内部状态
const route = useRoute();
const router = useRouter();
const searchKeyword = ref('');
const userName = ref('');
const userAvatar = ref('');
const totalSpace = ref('0 GB');
const usedSpace = ref('0 GB');
const storageUsage = ref(0);

// 计算缓存视图
const cachedViews = computed(() => {
  return ['Home', 'Starred', 'RecycleBin', 'MyShares', 'Settings'];
});

// 计算用户名首字母(无头像时使用)
const userInitials = computed(() => {
  return userName.value ? userName.value.charAt(0).toUpperCase() : 'U';
});

// 计算当前活动菜单
const activeMenu = computed(() => {
  return route.path;
});

// 格式化存储空间使用百分比
const storageFormat = (percentage: number): string => {
  return `${percentage}%`;
};

// 处理下拉菜单命令
const handleCommand = (command: string) => {
  if (command === 'logout') {
    ElMessageBox.confirm(
      '确定要退出登录吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    ).then(() => {
      logout().then(() => {
        localStorage.removeItem('token');
        router.push('/login');
        ElMessage.success('已成功退出登录');
      });
    }).catch(() => {
      // 取消退出
    });
  } else if (command === 'settings') {
    router.push('/settings');
  }
};

// 处理搜索
const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({
      path: '/search',
      query: { keyword: searchKeyword.value.trim() }
    });
  }
};

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const userInfo = await getUserInfo();
    userName.value = userInfo.username;
    userAvatar.value = userInfo.avatar || '';
  } catch (error) {
    console.error('获取用户信息失败', error);
  }
};

// 加载存储空间信息
const loadStorageInfo = async () => {
  try {
    const response = await getUserStorageInfo();
    console.log('边栏存储空间信息响应:', response);
    
    // 确保响应数据存在
    const storageInfo = response || {};
    
    storageUsage.value = Math.round(storageInfo.usagePercent || 0);
    
    // 格式化存储空间
    const formatBytes = (bytes: number, decimals = 2) => {
      if (!bytes || bytes === 0) return '0 B';
      const k = 1024;
      const dm = decimals < 0 ? 0 : decimals;
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    };
    
    totalSpace.value = formatBytes(storageInfo.totalSpace || 0);
    usedSpace.value = formatBytes(storageInfo.usedSpace || 0);
  } catch (error) {
    console.error('获取存储空间信息失败', error);
  }
};

// 生命周期钩子
onMounted(() => {
  loadUserInfo();
  loadStorageInfo();
});
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 60px;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 10;
}

.logo {
  flex: 0 0 200px;
}

.logo h1 {
  margin: 0;
  font-size: 20px;
  color: #409EFF;
}

.search-bar {
  flex: 1;
  max-width: 500px;
  margin: 0 20px;
}

.user-menu {
  margin-left: auto;
}

.user-avatar {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.user-avatar span {
  margin-left: 8px;
}

.app-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.app-sidebar {
  width: 220px;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
  border-right: 1px solid #e0e0e0;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
}

.storage-info {
  padding: 16px;
  border-top: 1px solid #e0e0e0;
}

.storage-text {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
  text-align: center;
}

.app-content {
  flex: 1;
  overflow: auto;
  padding: 20px;
  background-color: #f6f8fa;
}
</style> 