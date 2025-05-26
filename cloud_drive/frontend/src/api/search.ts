import request from './index';
import type { FileItem } from './file';

/**
 * 搜索相关接口类型定义
 */
export interface SearchParams {
  keyword: string;
  page?: number;
  size?: number;
  type?: string; // 文件类型筛选
  timeRange?: {
    start?: string;
    end?: string;
  };
  orderBy?: string;
  orderDirection?: 'asc' | 'desc';
}

export interface SearchResult {
  total: number;
  list: FileItem[];
}

/**
 * 基础搜索(文件名搜索)
 */
export function basicSearch(params: SearchParams) {
  return request({
    url: '/file/search',
    method: 'get',
    params
  });
}

/**
 * 全文检索(文档内容搜索)
 * 注意：此接口使用相同的后端接口，但增加了内容搜索标志
 */
export function fullTextSearch(params: SearchParams) {
  return request({
    url: '/file/search',
    method: 'get',
    params: {
      ...params,
      searchContent: true
    }
  });
}

/**
 * 获取搜索建议/自动补全
 */
export function getSearchSuggestions(keyword: string) {
  return request({
    url: '/file/search/suggestions',
    method: 'get',
    params: { keyword }
  });
}

/**
 * 图像内容搜索
 */
export function imageContentSearch(params: SearchParams) {
  return request({
    url: '/file/search',
    method: 'get',
    params: {
      ...params,
      searchType: 'image'
    }
  });
}

/**
 * 获取搜索历史记录
 * 注意：目前使用本地存储实现
 */
export function getSearchHistory(): Promise<string[]> {
  return new Promise((resolve) => {
    const history = localStorage.getItem('searchHistory');
    resolve(history ? JSON.parse(history) : []);
  });
}

/**
 * 清除搜索历史记录
 * 注意：目前使用本地存储实现
 */
export function clearSearchHistory(): Promise<boolean> {
  return new Promise((resolve) => {
    localStorage.removeItem('searchHistory');
    resolve(true);
  });
}

/**
 * 保存搜索历史记录
 * 注意：目前使用本地存储实现
 */
export function saveSearchHistory(keyword: string): Promise<boolean> {
  return new Promise((resolve) => {
    const history = localStorage.getItem('searchHistory');
    let historyArr = history ? JSON.parse(history) : [];
    
    // 如果已存在，先移除
    historyArr = historyArr.filter((item: string) => item !== keyword);
    
    // 添加到最前面
    historyArr.unshift(keyword);
    
    // 最多保存10条
    historyArr = historyArr.slice(0, 10);
    
    localStorage.setItem('searchHistory', JSON.stringify(historyArr));
    resolve(true);
  });
}

/**
 * 高级搜索(综合多条件)
 */
export function advancedSearch(params: SearchParams & {
  sizeRange?: {
    min?: number;
    max?: number;
  };
  tags?: string[];
  owner?: string;
}) {
  return request({
    url: '/file/search',
    method: 'get',
    params
  });
} 