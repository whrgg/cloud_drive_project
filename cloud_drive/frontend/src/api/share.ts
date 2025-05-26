import request from './index';
import type { ShareInfo } from '../types/share';

/**
 * 创建分享链接
 * @param params 分享参数
 * @returns Promise
 */
export const createShareLink = (params: {
  userFileId: number;
  expireType: number | null; // 过期时间类型：0-永久有效，1-1天，7-7天，30-30天
  hasExtraction: boolean;    // 是否设置提取码
  extractionCode: string | null; // 提取码，为null则自动生成
  description?: string | null; // 分享说明
}) => {
  return request({
    url: '/share/create',
    method: 'post',
    data: params
  });
};

/**
 * 获取分享链接详情
 * @param shareId 分享ID
 * @param code 提取码（可选）
 * @returns Promise
 */
export const getShareInfo = (shareId: string, code?: string) => {
  return request({
    url: `/share/info/${shareId}`,
    method: 'get',
    params: code ? { code } : {}
  });
};

/**
 * 验证分享提取码
 * @param shareId 分享ID
 * @param code 提取码
 * @returns Promise
 */
export const verifyShareCode = (shareId: string, code: string) => {
  return request({
    url: `/share/verify/${shareId}`,
    method: 'post',
    data: { code }
  });
};

/**
 * 获取分享文件列表
 * @param shareId 分享ID
 * @param parentId 父文件夹ID
 * @returns Promise
 */
export const getShareFileList = (shareId: string, parentId: number = 0) => {
  return request({
    url: `/share/list/${shareId}`,
    method: 'get',
    params: { parentId }
  });
};

/**
 * 获取用户创建的分享列表
 * @param params 查询参数
 * @returns Promise
 */
export const getUserShares = (params?: {
  page?: number;
  size?: number;
  orderBy?: string;
  orderDirection?: 'asc' | 'desc';
}) => {
  return request({
    url: '/share/my',
    method: 'get',
    params
  });
};

/**
 * 取消分享
 * @param shareId 分享ID
 * @returns Promise
 */
export const cancelShare = (shareId: string) => {
  return request({
    url: `/share/cancel/${shareId}`,
    method: 'delete'
  });
};

/**
 * 批量取消分享
 * @param shareIds 分享ID数组
 * @returns Promise
 */
export const batchCancelShare = (shareIds: string[]) => {
  return request({
    url: '/share/batch/cancel',
    method: 'delete',
    data: { shareIds }
  });
};

/**
 * 保存分享文件到自己的网盘
 * @param shareId 分享ID
 * @param fileIds 要保存的文件ID数组
 * @param targetId 保存到的目标文件夹ID
 * @returns Promise
 */
export const saveShareFiles = (shareId: string, fileIds: number[], targetId: number = 0) => {
  return request({
    url: `/share/save/${shareId}`,
    method: 'post',
    data: {
      fileIds,
      targetId
    }
  });
};

/**
 * 增加分享浏览次数
 * @param shareId 分享ID
 * @returns Promise
 */
export const increaseShareViews = (shareId: string) => {
  return request({
    url: `/share/views/${shareId}`,
    method: 'put'
  });
};

/**
 * 增加分享下载次数
 * @param shareId 分享ID
 * @returns Promise
 */
export const increaseShareDownloads = (shareId: string) => {
  return request({
    url: `/share/downloads/${shareId}`,
    method: 'put'
  });
};

/**
 * 获取分享文件下载链接
 * @param shareId 分享ID
 * @param userFileId 用户文件ID
 * @returns Promise
 */
export const getShareFileDownloadUrl = (shareId: string, userFileId: number) => {
  // 从localStorage获取token
  const token = localStorage.getItem('token');
  
  console.log(`获取分享下载链接: /share/download/${shareId}/${userFileId}`);
  
  return request({
    url: `/share/download/${shareId}/${userFileId}`,
    method: 'get',
    responseType: 'blob',
    headers: {
      // 确保请求头包含Authorization
      Authorization: token ? `Bearer ${token}` : ''
    }
  }).then(response => {
    console.log('下载响应头:', response.headers);
    
    // 从响应头中获取文件名
    const contentDisposition = response.headers['content-disposition'];
    let fileName = '';
    
    if (contentDisposition) {
      const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
      const matches = filenameRegex.exec(contentDisposition);
      if (matches != null && matches[1]) {
        fileName = matches[1].replace(/['"]/g, '');
        // 解码文件名
        try {
          fileName = decodeURIComponent(fileName);
        } catch (e) {
          console.warn('Failed to decode filename', e);
        }
      }
    }
    
    // 获取内容类型
    const contentType = response.headers['content-type'] || 'application/octet-stream';
    
    // 创建Blob URL
    const blob = new Blob([response.data], { type: contentType });
    const url = window.URL.createObjectURL(blob);
    
    console.log('创建下载URL成功:', url, '文件名:', fileName, '内容类型:', contentType);
    
    return url;
  });
}; 