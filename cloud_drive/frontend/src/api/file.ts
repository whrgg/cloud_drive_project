import request from './index';
import type { FileInfo, Breadcrumb } from '../types/file';

/**
 * 文件相关接口类型定义
 */
export interface FileItem {
  id: number;
  name: string;
  size: number;
  type: string;
  path: string;
  isDir: boolean;
  parentId: number;
  createTime: string;
  updateTime: string;
  thumbnail?: string;
  isStarred?: boolean;
  isShared?: boolean;
  isRecycled?: boolean;
}

export interface FileListParams {
  parentId?: number;
  type?: string;
  keyword?: string;
  page?: number;
  size?: number;
  orderBy?: string;
  orderDirection?: 'asc' | 'desc';
}

export interface UploadFileParams {
  file: File;
  parentId: number;
  md5?: string;
  chunkIndex?: number;
  chunks?: number;
}

/**
 * 获取文件列表
 */
export const getFileList = (params: {
  parentId: number;
  orderBy?: string;
  orderDirection?: 'asc' | 'desc';
}): Promise<{ list: FileInfo[], path: Breadcrumb[] }> => {
  return request({
    url: '/file/list',
    method: 'get',
    params
  });
};

/**
 * 获取文件详情
 */
export const getFileDetail = (id: number) => {
  return request({
    url: `/file/detail/${id}`,
    method: 'get'
  });
};

/**
 * 创建文件夹
 */
export const createFolder = (parentId: number, name: string): Promise<{ list: FileInfo[] }> => {
  // 确保参数正确
  if (parentId === undefined || parentId === null) {
    return Promise.reject(new Error('parentId不能为空'));
  }
  
  if (!name || name.trim() === '') {
    return Promise.reject(new Error('name不能为空'));
  }
  
  return request({
    url: '/file/folder',
    method: 'post',
    headers: {
      'Content-Type': 'application/json'
    },
    data: {
      parentId,
      name
    }
  });
};

/**
 * 重命名文件/文件夹
 */
export const renameFile = (id: number, name: string) => {
  return request({
    url: `/file/rename/${id}`,
    method: 'put',
    data: {
      name
    }
  });
};

/**
 * 移动文件/文件夹
 */
export const moveFile = (ids: number[], targetId: number) => {
  return request({
    url: '/file/move',
    method: 'put',
    data: {
      fileIds: ids,
      targetId
    }
  });
};

/**
 * 复制文件/文件夹
 */
export const copyFile = (ids: number[], targetId: number) => {
  return request({
    url: '/file/copy',
    method: 'post',
    data: {
      fileIds: ids,
      targetId
    }
  });
};

/**
 * 删除文件/文件夹(移至回收站)
 */
export const deleteFile = (ids: number[]) => {
  return request({
    url: '/file/delete',
    method: 'post',
    data: {
      fileIds: ids
    }
  });
};

/**
 * 彻底删除文件/文件夹
 */
export const removeFile = (ids: number[]) => {
  return request({
    url: '/file/remove',
    method: 'delete',
    data: {
      fileIds: ids
    }
  });
};

/**
 * 恢复回收站的文件/文件夹
 */
export const restoreFile = (ids: number[]) => {
  return request({
    url: '/file/restore',
    method: 'put',
    data: {
      fileIds: ids
    }
  });
};

/**
 * 清空回收站
 */
export const clearRecycleBin = () => {
  return request({
    url: '/file/clear-recycle',
    method: 'delete'
  });
};

/**
 * 获取回收站文件列表
 */
export const getRecycleBinList = (params: {
  orderBy?: string;
  orderDirection?: 'asc' | 'desc';
}) => {
  return request({
    url: '/file/recycle',
    method: 'get',
    params
  });
};

/**
 * 获取回收站中特定文件夹的内容
 */
export const getRecycleBinFolderContents = (folderId: number, params: {
  orderBy?: string;
  orderDirection?: 'asc' | 'desc';
}) => {
  return request({
    url: `/file/recycle/folder/${folderId}`,
    method: 'get',
    params
  });
};

/**
 * 获取文件下载链接
 */
export const getFileDownloadUrl = (id: number) => {
  // 从localStorage获取token
  const token = localStorage.getItem('token');
  
  return request({
    url: `/file/download/${id}`,
    method: 'get',
    responseType: 'blob',
    headers: {
      // 确保请求头包含Authorization
      Authorization: token ? `Bearer ${token}` : ''
    }
  }).then(response => {
    // 从响应头中获取文件名
    const contentDisposition = response.headers['content-disposition'];
    let fileName = '';
    
    if (contentDisposition) {
      const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
      const matches = filenameRegex.exec(contentDisposition);
      if (matches != null && matches[1]) {
        fileName = matches[1].replace(/['"]/g, '');
      }
    }
    
    // 创建Blob URL
    const blob = new Blob([response.data], { type: response.headers['content-type'] });
    const url = window.URL.createObjectURL(blob);
    
    return url;
  });
};

/**
 * 批量获取文件下载链接(用于打包下载)
 */
export function getBatchDownloadUrl(fileIds: number[]) {
  // 从localStorage获取token
  const token = localStorage.getItem('token');
  
  return request({
    url: '/file/batch/download',
    method: 'post',
    data: { fileIds },
    responseType: 'blob',
    headers: {
      // 确保请求头包含Authorization
      Authorization: token ? `Bearer ${token}` : ''
    }
  }).then(response => {
    // 从响应头中获取文件名
    const contentDisposition = response.headers['content-disposition'];
    let fileName = 'download.zip'; // 默认文件名
    
    if (contentDisposition) {
      const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
      const matches = filenameRegex.exec(contentDisposition);
      if (matches != null && matches[1]) {
        fileName = matches[1].replace(/['"]/g, '');
      }
    }
    
    // 创建Blob URL
    const blob = new Blob([response.data], { type: response.headers['content-type'] });
    const url = window.URL.createObjectURL(blob);
    
    return url;
  });
}

/**
 * 获取文件预览URL
 */
export const getFilePreviewUrl = (id: number) => {
  return request({
    url: `/file/preview/${id}`,
    method: 'get'
  });
};

/**
 * 收藏/取消收藏文件
 */
export const toggleStarFile = (id: number, isStar: boolean) => {
  return request({
    url: `/file/star/${id}`,
    method: 'put',
    data: {
      star: isStar
    }
  });
};

/**
 * 获取收藏的文件列表
 */
export const getStarredFiles = (params: {
  orderBy?: string;
  orderDirection?: 'asc' | 'desc';
}): Promise<{ list: FileInfo[] }> => {
  return request({
    url: '/file/starred',
    method: 'get',
    params
  });
};

/**
 * 上传文件
 * @param params 上传参数
 * @returns Promise
 */
export const uploadFile = (params: {
  file: File;
  parentId: number;
  relativePath?: string;
  onProgress?: (percentage: number) => void;
}) => {
  const { file, parentId, relativePath, onProgress } = params;
  
  // 创建表单数据
  const formData = new FormData();
  formData.append('file', file);
  formData.append('parentId', parentId.toString());
  
  if (relativePath) {
    formData.append('relativePath', relativePath);
    console.log(`[API] 上传文件 - relativePath: ${relativePath}`);
    
    // 分析路径结构并打印
    const pathParts = relativePath.split('/');
    console.log(`[API] 上传文件 - 路径分析:`, {
      完整路径: relativePath,
      路径层级: pathParts.length,
      路径组成: pathParts,
      文件名: pathParts[pathParts.length - 1],
      父目录: pathParts.length > 1 ? pathParts.slice(0, -1).join('/') : '根目录'
    });
  } else {
    console.log(`[API] 上传文件 - 无相对路径，直接上传到父文件夹ID: ${parentId}`);
  }
  
  // 从localStorage获取token
  const token = localStorage.getItem('token');
  
  console.log(`[API] 准备上传文件:`, {
    文件名: file.name,
    文件大小: file.size,
    文件类型: file.type,
    父文件夹ID: parentId
  });
  
  return request({
    url: '/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
      // 确保请求头包含Authorization
      Authorization: token ? `Bearer ${token}` : ''
    },
    onUploadProgress: (progressEvent: any) => {
      if (onProgress && progressEvent.total > 0) {
        const percentage = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        onProgress(percentage);
      }
    }
  }).then(response => {
    console.log(`[API] 文件上传成功:`, {
      文件名: file.name,
      响应数据: response
    });
    return response;
  }).catch(error => {
    console.error(`[API] 文件上传失败:`, {
      文件名: file.name,
      错误信息: error
    });
    throw error;
  });
};

/**
 * 检查文件MD5
 * @param md5 文件MD5值
 * @param fileName 文件名
 * @param fileSize 文件大小
 * @returns Promise
 */
export const checkFileMd5 = (md5: string, fileName: string, fileSize: number) => {
  // 从localStorage获取token
  const token = localStorage.getItem('token');
  
  return request({
    url: '/file/check-md5',
    method: 'post',
    data: {
      md5,
      fileName,
      fileSize
    },
    headers: {
      // 确保请求头包含Authorization
      Authorization: token ? `Bearer ${token}` : ''
    }
  });
};

/**
 * 上传文件分片
 * @param params 分片参数
 * @returns Promise
 */
export const uploadChunk = (params: {
  file: File;
  parentId: number;
  md5: string;
  chunkIndex: number;
  chunks: number;
}) => {
  const { file, parentId, md5, chunkIndex, chunks } = params;
  
  // 创建表单数据
  const formData = new FormData();
  formData.append('file', file);
  formData.append('parentId', parentId.toString());
  formData.append('md5', md5);
  formData.append('chunkIndex', chunkIndex.toString());
  formData.append('chunks', chunks.toString());
  
  // 从localStorage获取token
  const token = localStorage.getItem('token');
  
  return request({
    url: '/file/upload-chunk',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
      // 确保请求头包含Authorization
      Authorization: token ? `Bearer ${token}` : ''
    }
  });
};

/**
 * 合并文件分片
 * @param md5 文件MD5值
 * @param fileName 文件名
 * @param parentId 父目录ID
 * @param chunks 分片数量
 * @returns Promise
 */
export const mergeChunks = (md5: string, fileName: string, parentId: number, chunks: number) => {
  // 从localStorage获取token
  const token = localStorage.getItem('token');
  
  return request({
    url: '/file/merge-chunks',
    method: 'post',
    data: {
      md5,
      fileName,
      parentId,
      chunks
    },
    headers: {
      // 确保请求头包含Authorization
      Authorization: token ? `Bearer ${token}` : ''
    }
  });
};

/**
 * 搜索文件
 */
export function searchFiles(keyword: string, params?: Omit<FileListParams, 'keyword'>) {
  return request({
    url: '/file/search',
    method: 'get',
    params: { keyword, ...params }
  });
}

// 批量删除文件
export const batchDeleteFiles = (fileIds: number[]) => {
  return request({
    url: '/file/delete',
    method: 'put',
    data: {
      fileIds
    }
  });
}; 