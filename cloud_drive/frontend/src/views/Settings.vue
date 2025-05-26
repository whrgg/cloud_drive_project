<template>
  <div class="settings-container">
    <div class="settings-header">
      <h2>个人设置</h2>
    </div>

    <div class="settings-content">
      <el-card class="settings-card">
        <el-tabs v-model="activeTab">
          <!-- 基本信息设置 -->
          <el-tab-pane label="基本信息" name="profile">
            <div class="profile-settings">
              <div class="avatar-container">
                <el-avatar :size="100" :src="userForm.avatar" v-if="userForm.avatar">
                  {{ userInitials }}
                </el-avatar>
                <el-avatar :size="100" v-else>{{ userInitials }}</el-avatar>
                <div class="avatar-actions">
                  <el-upload
                    class="avatar-uploader"
                    action="#"
                    :http-request="uploadAvatarRequest"
                    :show-file-list="false"
                    accept="image/*"
                  >
                    <el-button size="small" type="primary">更换头像</el-button>
                  </el-upload>
                </div>
              </div>

              <el-form
                ref="userFormRef"
                :model="userForm"
                :rules="userRules"
                label-width="100px"
                class="user-form"
              >
                <el-form-item label="用户名" prop="username">
                  <el-input v-model="userForm.username" placeholder="请输入用户名" />
                </el-form-item>
                
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="userForm.email" placeholder="请输入邮箱" />
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" @click="updateProfile" :loading="updating">
                    保存修改
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>

          <!-- 修改密码 -->
          <el-tab-pane label="修改密码" name="password">
            <div class="password-settings">
              <el-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-width="100px"
                class="password-form"
              >
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input
                    v-model="passwordForm.oldPassword"
                    type="password"
                    placeholder="请输入当前密码"
                    show-password
                  />
                </el-form-item>

                <el-form-item label="新密码" prop="newPassword">
                  <el-input
                    v-model="passwordForm.newPassword"
                    type="password"
                    placeholder="请输入新密码"
                    show-password
                  />
                </el-form-item>

                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    placeholder="请再次输入新密码"
                    show-password
                  />
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" @click="updatePassword" :loading="updating">
                    修改密码
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>

          <!-- 存储空间 -->
          <el-tab-pane label="存储空间" name="storage">
            <div class="storage-settings">
              <div class="storage-info">
                <h3>存储空间使用情况</h3>
                <el-progress :percentage="storageUsage" :format="storageFormat" />
                
                <div class="storage-details">
                  <div class="storage-block">
                    <div class="storage-label">总容量</div>
                    <div class="storage-value">{{ totalSpace }}</div>
                  </div>
                  <div class="storage-block">
                    <div class="storage-label">已使用</div>
                    <div class="storage-value">{{ usedSpace }}</div>
                  </div>
                  <div class="storage-block">
                    <div class="storage-label">剩余空间</div>
                    <div class="storage-value">{{ freeSpace }}</div>
                  </div>
                </div>
              </div>

              <div class="storage-usage">
                <h3>存储空间分布</h3>
                <div class="usage-chart">
                  <el-empty v-if="!storageStats.length" description="暂无数据" />
                  <div v-else class="chart-container">
                    <!-- 此处可以使用图表库如ECharts来绘制饼图，这里用简单的列表代替 -->
                    <div v-for="(stat, index) in storageStats" :key="index" class="usage-item">
                      <div class="usage-type" :style="{ backgroundColor: stat.color }"></div>
                      <div class="usage-name">{{ stat.name }}</div>
                      <div class="usage-size">{{ stat.size }}</div>
                      <div class="usage-percent">{{ stat.percentage }}%</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 安全设置 -->
          <el-tab-pane label="安全设置" name="security">
            <div class="security-settings">
              <div class="security-item">
                <div class="security-info">
                  <h3>登录保护</h3>
                  <p>开启后，登录时将进行安全验证</p>
                </div>
                <div class="security-action">
                  <el-switch v-model="securitySettings.loginVerification" />
                </div>
              </div>
              
              <div class="security-item">
                <div class="security-info">
                  <h3>登录通知</h3>
                  <p>开启后，将在新设备登录时发送邮件通知</p>
                </div>
                <div class="security-action">
                  <el-switch v-model="securitySettings.loginNotification" />
                </div>
              </div>
              
              <div class="security-item">
                <div class="security-info">
                  <h3>设备管理</h3>
                  <p>管理已登录的设备</p>
                </div>
                <div class="security-action">
                  <el-button type="primary" @click="showDeviceManager">
                    管理设备
                  </el-button>
                </div>
              </div>
              
              <div class="security-footer">
                <el-button type="primary" @click="saveSecuritySettings" :loading="updating">
                  保存设置
                </el-button>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import {
  getUserInfo,
  updateUserInfo,
  changePassword,
  uploadAvatar,
  getUserStorageInfo,
} from '../api/user';
import type { UserInfo } from '../api/user';

// 标签页
const activeTab = ref('profile');
const updating = ref(false);

// 用户表单
const userFormRef = ref<FormInstance>();
const userForm = reactive({
  username: '',
  email: '',
  avatar: '',
});

// 密码表单
const passwordFormRef = ref<FormInstance>();
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

// 存储空间信息
const storageUsage = ref(0);
const totalSpace = ref('');
const usedSpace = ref('');
const freeSpace = ref('');

// 存储空间类型分布
const storageStats = ref<{
  name: string;
  size: string;
  percentage: number;
  color: string;
}[]>([]);

// 安全设置
const securitySettings = reactive({
  loginVerification: false,
  loginNotification: false,
});

// 用户名首字母
const userInitials = computed(() => {
  return userForm.username ? userForm.username.charAt(0).toUpperCase() : 'U';
});

// 表单验证规则
const userRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
});

const passwordRules = reactive<FormRules>({
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
});

// 格式化存储空间使用百分比
const storageFormat = (percentage: number): string => {
  return `${percentage}%`;
};

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const userInfo = await getUserInfo();
    if (userInfo) {
      userForm.username = userInfo.username || '';
      userForm.email = userInfo.email || '';
      userForm.avatar = userInfo.avatar || '';
    }
  } catch (error) {
    console.error('获取用户信息失败', error);
    ElMessage.error('获取用户信息失败');
  }
};

// 加载存储空间信息
const loadStorageInfo = async () => {
  try {
    const storageInfo = await getUserStorageInfo();
    console.log('存储空间信息响应:', storageInfo);
    
    // 设置存储空间使用百分比
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
    freeSpace.value = formatBytes(storageInfo.availableSpace || 0);
    
    // 处理文件类型分布数据
    if (storageInfo.typeDistribution) {
      const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#8E44AD', '#16A085'];
      storageStats.value = Object.entries(storageInfo.typeDistribution).map(([key, value], index) => ({
        name: key,
        size: formatBytes(value as number),
        percentage: Math.round(((value as number) / (storageInfo.usedSpace || 1)) * 100),
        color: colors[index % colors.length],
      }));
    } else {
      storageStats.value = [];
    }
  } catch (error) {
    console.error('获取存储空间信息失败', error);
    ElMessage.error('获取存储空间信息失败');
  }
};

// 更新用户信息
const updateProfile = async () => {
  await userFormRef.value?.validate(async (valid) => {
    if (valid) {
      updating.value = true;
      try {
        await updateUserInfo({
          username: userForm.username,
          email: userForm.email,
        });
        ElMessage.success('个人信息更新成功');
      } catch (error) {
        console.error('更新个人信息失败', error);
        ElMessage.error('更新个人信息失败');
      } finally {
        updating.value = false;
      }
    }
  });
};

// 更新密码
const updatePassword = async () => {
  await passwordFormRef.value?.validate(async (valid) => {
    if (valid) {
      updating.value = true;
      try {
        await changePassword(passwordForm.oldPassword, passwordForm.newPassword);
        ElMessage.success('密码修改成功');
        
        // 清空密码表单
        passwordForm.oldPassword = '';
        passwordForm.newPassword = '';
        passwordForm.confirmPassword = '';
      } catch (error) {
        console.error('密码修改失败', error);
        ElMessage.error('密码修改失败，请检查当前密码是否正确');
      } finally {
        updating.value = false;
      }
    }
  });
};

// 上传头像
const uploadAvatarRequest = async (options: any) => {
  try {
    const avatarUrl = await uploadAvatar(options.file);
    console.log('头像上传结果:', avatarUrl); // 打印上传结果
    
    // 获取到头像URL后，更新用户表单中的头像
    if (avatarUrl && typeof avatarUrl === 'string') {
      userForm.avatar = avatarUrl;
      
      // 强制刷新头像显示（添加随机参数避免缓存）
      if (!userForm.avatar.includes('?')) {
        userForm.avatar = `${userForm.avatar}?t=${new Date().getTime()}`;
      }
      
      ElMessage.success('头像上传成功');
      
      // 重新加载用户信息，确保数据同步
      await loadUserInfo();
    } else {
      console.error('头像URL格式不正确:', avatarUrl);
      ElMessage.warning('头像上传成功，但URL格式不正确');
    }
  } catch (error) {
    console.error('头像上传失败', error);
    ElMessage.error(error instanceof Error ? error.message : '头像上传失败');
  }
};

// 保存安全设置
const saveSecuritySettings = async () => {
  updating.value = true;
  try {
    // 调用API保存安全设置
    // 假设API存在：updateSecuritySettings(securitySettings)
    await new Promise(resolve => setTimeout(resolve, 500));
    ElMessage.success('安全设置保存成功');
  } catch (error) {
    console.error('安全设置保存失败', error);
    ElMessage.error('安全设置保存失败');
  } finally {
    updating.value = false;
  }
};

// 显示设备管理对话框
const showDeviceManager = () => {
  ElMessage.info('设备管理功能即将上线');
};

// 生命周期钩子
onMounted(() => {
  loadUserInfo();
  loadStorageInfo();
});
</script>

<style scoped>
.settings-container {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.settings-header {
  margin-bottom: 20px;
}

.settings-header h2 {
  margin: 0;
}

.settings-content {
  flex: 1;
}

.settings-card {
  width: 100%;
  height: 100%;
}

.profile-settings {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-container {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.user-form,
.password-form {
  width: 100%;
  max-width: 500px;
}

.storage-settings {
  margin-top: 20px;
}

.storage-info {
  margin-bottom: 30px;
}

.storage-info h3 {
  margin-top: 0;
  margin-bottom: 16px;
}

.storage-details {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}

.storage-block {
  text-align: center;
  flex: 1;
}

.storage-label {
  color: #909399;
  margin-bottom: 8px;
}

.storage-value {
  font-size: 18px;
  font-weight: bold;
}

.usage-chart {
  margin-top: 20px;
}

.chart-container {
  margin-top: 16px;
}

.usage-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.usage-type {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  margin-right: 10px;
}

.usage-name {
  flex: 1;
}

.usage-size {
  width: 100px;
  text-align: right;
  margin-right: 10px;
}

.usage-percent {
  width: 60px;
  text-align: right;
}

.security-settings {
  margin-top: 20px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.security-item:first-child {
  padding-top: 0;
}

.security-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
}

.security-info p {
  color: #606266;
  margin: 0;
}

.security-footer {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style> 