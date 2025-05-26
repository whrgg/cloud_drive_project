/**
 * 文件相关类型定义
 */
import type { FileType } from './common';

// 文件信息
export interface FileInfo {
  id: number;
  fileName: string;
  fileSize: number;
  type: string; // 文件MIME类型
  fileType: FileType; // 文件分类类型
  filePath: string;
  isDir: boolean;
  parentId: number;
  userId: number;
  createTime: string;
  updateTime: string;
  thumbnail?: string;
  md5?: string;
  status: number; // 0-正常 1-回收站 2-已删除
  isStarred: boolean;
  isShared: boolean;
  downloadCount: number;
  viewCount: number;
  lastViewTime?: string;
  extension?: string; // 文件扩展名
}

// 文件列表查询参数
export interface FileListParams {
  parentId?: number;
  type?: FileType;
  keyword?: string;
  page?: number;
  size?: number;
  orderBy?: string;
  orderDirection?: 'asc' | 'desc';
  isStarred?: boolean;
  isRecycled?: boolean;
}

// 文件列表响应
export interface FileListResult {
  total: number;
  list: FileInfo[];
  path: Breadcrumb[];
}

// 面包屑导航项
export interface Breadcrumb {
  id: number;
  name: string;
}

// 文件上传参数
export interface UploadFileParams {
  file: File;
  parentId: number;
  md5?: string;
  chunkIndex?: number;
  chunks?: number;
  onProgress?: (percentage: number) => void;
}

// 分片上传检查结果
export interface ChunkCheckResult {
  isExists: boolean; // 文件是否已存在(秒传)
  uploadedChunks: number[]; // 已上传的分片索引
  fileId?: number; // 文件已存在时返回文件ID
}

// 文件操作类型
export enum FileOperation {
  COPY = 'copy',
  MOVE = 'move',
  RENAME = 'rename',
  DELETE = 'delete',
  DOWNLOAD = 'download',
  SHARE = 'share',
  STAR = 'star',
  RESTORE = 'restore',
  PREVIEW = 'preview'
}

// 文件预览信息
export interface FilePreview {
  url: string;
  type: FileType;
  name: string;
  size: number;
} 