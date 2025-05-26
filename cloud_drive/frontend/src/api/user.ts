import request from './index'

/**
 * 用户相关的API接口
 */
export interface LoginParams {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterParams {
  username: string;
  password: string;
  email: string;
  code?: string; // 验证码
}

export interface UserInfo {
  id: number;
  username: string;
  email: string;
  avatar?: string;
  totalSpace: number;
  usedSpace: number;
  createdTime: string;
  lastLoginTime?: string;
}

/**
 * 登录接口
 * @param data 登录参数
 */
export function login(data: LoginParams) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 注册接口
 * @param data 注册参数
 */
export function register(data: RegisterParams) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

/**
 * 退出登录
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/**
 * 获取当前用户信息
 */
export function getUserInfo(): Promise<UserInfo> {
  return request({
    url: '/user/info',
    method: 'get'
  }).then(response => {
    // 确保返回用户信息
    if (typeof response === 'object' && response !== null) {
      // 安全地处理响应
      return response as UserInfo;
    }
    // 如果没有获取到有效的用户信息，抛出错误
    throw new Error('获取用户信息失败');
  });
}

/**
 * 修改用户信息
 * @param data 用户信息
 */
export function updateUserInfo(data: Partial<UserInfo>) {
  return request({
    url: '/user/info',
    method: 'put',
    data
  })
}

/**
 * 修改密码
 * @param oldPassword 旧密码
 * @param newPassword 新密码
 */
export function changePassword(oldPassword: string, newPassword: string) {
  return request({
    url: '/user/password',
    method: 'put',
    data: {
      oldPassword,
      newPassword
    }
  })
}

/**
 * 上传头像
 * @param file 头像文件
 * @returns 头像URL
 */
export function uploadAvatar(file: File): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);
  
  return request({
    url: '/user/avatar',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }).then(response => {
    console.log('上传头像接口返回:', response);
    
    // 确保返回头像URL
    if (typeof response === 'string') {
      return response;
    }
    
    // 如果返回对象包含data字段，则使用data字段
    if (response && typeof response === 'object') {
      if ('data' in response) {
        return response.data as string;
      }
      // 尝试其他可能的字段
      if ('url' in response) {
        return response.url as string;
      }
    }
    
    // 如果无法从响应中提取URL，抛出错误
    throw new Error('上传头像失败：无法获取有效的头像URL');
  });
}

/**
 * 获取用户存储空间信息
 * @returns 存储空间信息，包含totalSpace、usedSpace、availableSpace、usagePercent和typeDistribution
 */
export function getUserStorageInfo(): Promise<StorageInfo> {
  return request({
    url: '/user/storage',
    method: 'get'
  }).then(response => {
    // 确保返回的数据符合预期格式
    if (typeof response === 'object' && response !== null) {
      // 尝试安全地访问响应对象的属性
      const safeResponse = response as any;
      return {
        totalSpace: safeResponse.totalSpace || 0,
        usedSpace: safeResponse.usedSpace || 0,
        availableSpace: safeResponse.availableSpace || 0,
        usagePercent: safeResponse.usagePercent || 0,
        typeDistribution: safeResponse.typeDistribution || {}
      };
    }
    // 如果数据格式不符合预期，返回一个默认对象
    return {
      totalSpace: 0,
      usedSpace: 0,
      availableSpace: 0,
      usagePercent: 0,
      typeDistribution: {}
    };
  });
}

/**
 * 发送验证码
 * @param type 类型：'email'或'phone'
 * @param target 目标邮箱或手机号
 */
export function sendVerificationCode(type: 'email' | 'phone', target: string) {
  return request({
    url: '/auth/send-code',
    method: 'post',
    data: {
      type,
      target
    }
  })
} 