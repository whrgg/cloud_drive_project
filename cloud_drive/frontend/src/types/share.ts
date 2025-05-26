/**
 * 分享相关类型定义
 */
import type { FileInfo } from './file';

/**
 * 分享信息接口
 */
export interface ShareInfo {
  id: string;           // 分享ID
  fileId: number;       // 文件ID
  userFileId: number;   // 用户文件ID
  fileName: string;     // 文件名
  fileType: string;     // 文件类型
  fileSize: number;     // 文件大小
  isDir: boolean;       // 是否为文件夹
  userId: number;       // 创建者ID
  username: string;     // 创建者用户名
  createTime: string;   // 创建时间
  updateTime: string;   // 更新时间
  expireTime: string | null; // 过期时间
  shareCode: string;    // 分享码
  code: string | null;  // 提取码
  views: number;        // 浏览次数
  downloads: number;    // 下载次数
  status: number;       // 状态：0-有效，1-已取消，2-已过期
  description: string | null; // 分享说明
  url: string;          // 分享链接
  needExtraction?: boolean; // 是否需要提取码
  extractionCode?: string | null; // 提取码
  viewCount?: number;   // 浏览次数
  downloadCount?: number; // 下载次数
}

/**
 * 创建分享链接参数
 */
export interface CreateShareParams {
  fileId: number;               // 文件ID
  expireTime: number | null;    // 有效期（天数，null表示永久）
  code: string | null;          // 提取码，为null则不设置
  description: string | null;   // 分享说明
}

/**
 * 分享验证响应
 */
export interface ShareVerifyResult {
  verified: boolean;            // 验证是否成功
  shareInfo?: ShareInfo;        // 分享信息（验证成功时返回）
}

/**
 * 获取分享文件列表响应
 */
export interface ShareFileListResult {
  list: ShareFileInfo[];        // 文件列表
  path?: ShareBreadcrumb[];     // 面包屑导航
}

/**
 * 分享文件信息
 */
export interface ShareFileInfo {
  id: number;           // 文件ID
  name: string;         // 文件名
  size: number;         // 文件大小
  type: string;         // 文件类型
  isDir: boolean;       // 是否为文件夹
  createTime: string;   // 创建时间
  updateTime: string;   // 更新时间
  parentId: number;     // 父文件夹ID
  fileType?: string;    // 文件类型分类（image, video, audio等）
  thumbnail?: string;   // 缩略图链接
}

/**
 * 分享文件面包屑
 */
export interface ShareBreadcrumb {
  id: number;           // 文件ID
  name: string;         // 文件名
}

/**
 * 用户分享列表查询参数
 */
export interface ShareListParams {
  page?: number;                // 页码
  size?: number;                // 每页数量
  orderBy?: string;             // 排序字段
  orderDirection?: 'asc' | 'desc'; // 排序方向
}

/**
 * 分享另存为参数
 */
export interface SaveShareFilesParams {
  shareId: string;              // 分享ID
  fileIds: number[];            // 要保存的文件ID数组
  targetId: number;             // 保存到的目标文件夹ID
}

// 分享详情(包含文件信息)
export interface ShareDetail extends ShareInfo {
  fileInfo: FileInfo;
  isExpired: boolean;
  remainingDays?: number;
}

// 分享类型
export enum ShareType {
  PUBLIC = 0, // 公开分享
  PASSWORD = 1, // 密码分享
  PRIVATE = 2, // 私人分享(仅指定用户可访问)
}

// 分享状态
export enum ShareStatus {
  NORMAL = 0, // 正常
  CANCELED = 1, // 已取消
  EXPIRED = 2, // 已过期
}

// 分享访问参数
export interface ShareAccessParams {
  shareId: string;
  password?: string;
}

// 分享文件列表
export interface ShareFileList {
  total: number;
  list: FileInfo[];
  path: {
    id: number;
    name: string;
  }[];
} 