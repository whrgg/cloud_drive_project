/**
 * 通用类型定义文件
 */

// API响应通用格式
export interface ApiResponse<T = any> {
  code: number;
  data: T;
  message: string;
  success: boolean;
}

// 分页请求参数
export interface PaginationParams {
  page: number;
  size: number;
}

// 分页响应数据
export interface PaginationResult<T> {
  total: number;
  list: T[];
  page: number;
  size: number;
  pages: number;
}

// 排序参数
export interface SortParams {
  orderBy: string;
  orderDirection: 'asc' | 'desc';
}

// 通用选项类型
export interface Option {
  label: string;
  value: any;
  disabled?: boolean;
}

// 文件类型枚举
export enum FileType {
  FOLDER = 'folder',
  IMAGE = 'image',
  VIDEO = 'video',
  AUDIO = 'audio',
  DOCUMENT = 'document',
  PDF = 'pdf',
  ARCHIVE = 'archive',
  EXECUTABLE = 'executable',
  CODE = 'code',
  OTHER = 'other'
}

// 文件图标映射类型
export interface FileIconMap {
  [key: string]: string;
}

// 文件大小单位
export type FileSizeUnit = 'B' | 'KB' | 'MB' | 'GB' | 'TB';

// 路由元信息
export interface RouteMeta {
  title: string;
  icon?: string;
  requiresAuth?: boolean;
  roles?: string[];
  keepAlive?: boolean;
  hidden?: boolean;
}

// 主题模式
export type ThemeMode = 'light' | 'dark' | 'system';

// 语言支持
export type Language = 'zh-CN' | 'en-US'; 