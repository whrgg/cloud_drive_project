/**
 * 用户相关类型定义
 */

// 用户信息
export interface UserInfo {
  id: number;
  username: string;
  email: string;
  avatar?: string;
  nickname?: string;
  phone?: string;
  role: string;
  status: number;
  totalSpace: number; // 总空间大小(字节)
  usedSpace: number; // 已用空间大小(字节)
  createdTime: string;
  lastLoginTime?: string;
}

// 登录参数
export interface LoginParams {
  username: string;
  password: string;
  rememberMe?: boolean;
  captcha?: string;
}

// 登录响应
export interface LoginResult {
  token: string;
  tokenType: string;
  refreshToken?: string;
  expiresIn?: number;
  userInfo: UserInfo;
}

// 注册参数
export interface RegisterParams {
  username: string;
  password: string;
  email: string;
  phone?: string;
  code?: string;
}

// 修改密码参数
export interface ChangePwdParams {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

// 用户设置
export interface UserSettings {
  theme?: 'light' | 'dark' | 'system';
  language?: 'zh-CN' | 'en-US';
  defaultView?: 'list' | 'grid';
  notificationEnabled?: boolean;
}

// 存储空间信息
export interface StorageInfo {
  totalSpace: number;
  usedSpace: number;
  availableSpace: number;
  usagePercent: number;
} 