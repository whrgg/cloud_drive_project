<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h1>注册账号</h1>
        <p class="subtitle">加入云盘系统，享受安全便捷的云存储服务</p>
      </div>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="设置用户名"
            prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="输入邮箱"
            prefix-icon="Message"
          />
        </el-form-item>
        
        <el-form-item prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="输入手机号码（选填）"
            prefix-icon="Iphone"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="设置密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="确认密码"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleRegister"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            class="register-button"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
        
        <div class="login-link">
          已有账号？
          <router-link to="/login">立即登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { FormInstance } from 'element-plus';
import { register } from '../api/user';
import type { RegisterParams } from '../types/user';

const router = useRouter();
const registerFormRef = ref<FormInstance>();
const loading = ref(false);

interface RegisterForm extends RegisterParams {
  confirmPassword: string;
}

const registerForm = reactive<RegisterForm>({
  username: '',
  password: '',
  email: '',
  phone: '',
  confirmPassword: '',
});

const validatePass = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请输入密码'));
  } else {
    if (registerForm.confirmPassword !== '') {
      registerFormRef.value?.validateField('confirmPassword', () => null);
    }
    callback();
  }
};

const validateConfirmPass = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'));
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致!'));
  } else {
    callback();
  }
};

const validateEmail = (rule: any, value: string, callback: any) => {
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  if (!value) {
    callback(new Error('请输入邮箱'));
  } else if (!emailRegex.test(value)) {
    callback(new Error('请输入正确的邮箱格式'));
  } else {
    callback();
  }
};

const validatePhone = (rule: any, value: string, callback: any) => {
  const phoneRegex = /^1[3-9]\d{9}$/;
  if (value && !phoneRegex.test(value)) {
    callback(new Error('请输入正确的手机号格式'));
  } else {
    callback();
  }
};

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, validator: validateEmail, trigger: 'blur' }
  ],
  phone: [
    { validator: validatePhone, trigger: 'blur' }
  ],
  password: [
    { required: true, validator: validatePass, trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPass, trigger: 'blur' }
  ]
};

const handleRegister = () => {
  registerFormRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true;
        
        // 提取注册需要的数据
        const { username, password, email, phone } = registerForm;
        const registerData = { 
          username, 
          password, 
          email, 
          phone,
          confirmPassword: registerForm.confirmPassword,
        };
        
        await register(registerData);
        
        ElMessage.success('注册成功，请登录');
        router.push('/login');
      } catch (error: any) {
        // 显示详细的错误信息
        const errorMsg = error.message || '注册失败，请重试';
        ElMessage.error(errorMsg);
        console.error('注册失败:', error);
      } finally {
        loading.value = false;
      }
    }
  });
};
</script>

<style scoped>
.register-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
  background-image: url('../assets/login-bg.jpg');
  background-size: cover;
  background-position: center;
}

.register-box {
  width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h1 {
  font-size: 28px;
  color: #409EFF;
  margin-bottom: 10px;
}

.subtitle {
  font-size: 16px;
  color: #606266;
  margin: 0;
}

.register-form {
  margin-top: 30px;
}

.register-button {
  width: 100%;
  border-radius: 4px;
  height: 40px;
  font-size: 16px;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  color: #606266;
}
</style> 