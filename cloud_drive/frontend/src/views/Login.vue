<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>云盘系统</h1>
        <p class="subtitle">安全存储，便捷分享</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名/邮箱"
            prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <div class="login-options">
          <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
          <el-link type="primary" :underline="false">忘记密码？</el-link>
        </div>
        
        <el-form-item>
          <el-button
            type="primary"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
        
        <div class="register-link">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { FormInstance } from 'element-plus';
import { login } from '../api/user';
import type { LoginParams } from '../types/user';

const router = useRouter();
const route = useRoute();
const loginFormRef = ref<FormInstance>();
const loading = ref(false);

const loginForm = reactive<LoginParams>({
  username: '',
  password: '',
  rememberMe: false
});

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ]
};

const handleLogin = () => {
  loginFormRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true;
        const result = await login(loginForm);
        
        console.log('登录结果:', result);
        
        // 确保返回了token
        if (!result || !result.token) {
          ElMessage.error('登录失败：未获取到令牌');
          console.error('登录响应中没有token:', result);
          return;
        }
        
        // 存储token
        localStorage.setItem('token', result.token);
        
        // 如果有tokenType也存储它
        if (result.tokenType) {
          localStorage.setItem('tokenType', result.tokenType);
        }
        
        // 存储用户信息
        if (result.userInfo) {
          localStorage.setItem('userInfo', JSON.stringify(result.userInfo));
        }
        
        ElMessage.success('登录成功');
        
        // 重定向到之前尝试访问的页面或首页
        const redirectPath = route.query.redirect ? String(route.query.redirect) : '/';
        router.replace(redirectPath);
      } catch (error: any) {
        console.error('登录失败:', error);
        ElMessage.error(error.message || '登录失败，请重试');
      } finally {
        loading.value = false;
      }
    }
  });
};
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
  background-image: url('../assets/login-bg.jpg');
  background-size: cover;
  background-position: center;
}

.login-box {
  width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h1 {
  font-size: 28px;
  color: #409EFF;
  margin-bottom: 10px;
}

.subtitle {
  font-size: 16px;
  color: #606266;
  margin: 0;
}

.login-form {
  margin-top: 30px;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.login-button {
  width: 100%;
  border-radius: 4px;
  height: 40px;
  font-size: 16px;
}

.register-link {
  text-align: center;
  margin-top: 20px;
  color: #606266;
}
</style>