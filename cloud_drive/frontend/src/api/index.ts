import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '../router';

// 创建axios实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 60000, // 请求超时时间：60秒
  withCredentials: false // 跨域请求不携带cookie
});

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 从localStorage获取token
    const token = localStorage.getItem('token');
    // 如果存在token则在请求头中添加Authorization
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // 确保POST请求设置正确的Content-Type
    if (config.method === 'post' && !config.headers['Content-Type'] && !config.headers['content-type']) {
      // 如果不是FormData类型的请求，设置Content-Type为application/json
      if (!(config.data instanceof FormData)) {
        config.headers['Content-Type'] = 'application/json';
      }
    }
    
    return config;
  },
  (error) => {
    // 请求错误处理
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 如果是二进制文件，直接返回完整响应（包含headers）
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response;
    }
    
    // 获取响应数据
    const res = response.data;
    
    // 检查返回的数据状态
    if (res.code !== undefined && res.code !== 200) {
      // 401: 未登录或token过期
      if (res.code === 401) {
        ElMessage({
          message: '登录已过期，请重新登录',
          type: 'error',
          duration: 3000
        });
        
        // 清除token
        localStorage.removeItem('token');
        
        // 跳转到登录页面
        router.replace({
          path: '/login',
          query: { redirect: router.currentRoute.value.fullPath }
        });
      } else {
        // 处理其他业务错误
        ElMessage({
          message: res.message || '请求失败',
          type: 'error',
          duration: 3000
        });
      }
      
      const error = new Error(res.message || '请求失败');
      return Promise.reject(error);
    }
    
    // 返回实际的数据部分
    return res.data !== undefined ? res.data : res;
  },
  (error) => {
    // 处理响应错误
    if (error.response) {
      const { status, data } = error.response;
      
      // 如果后端返回了错误信息，优先使用后端返回的错误信息
      let errorMessage = '请求失败';
      if (data && data.message) {
        errorMessage = data.message;
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      // 处理401未授权错误
      if (status === 401) {
        ElMessage({
          message: '登录已过期，请重新登录',
          type: 'error',
          duration: 3000
        });
        
        // 清除token
        localStorage.removeItem('token');
        
        // 重定向到登录页
        setTimeout(() => {
          router.replace({
            path: '/login',
            query: { redirect: window.location.pathname }
          });
        }, 1000);
      } else if (status === 403) {
        // 处理403无权限错误
        ElMessage({
          message: errorMessage || '无权限访问该资源',
          type: 'error',
          duration: 3000
        });
      } else if (status === 404) {
        // 处理404资源未找到错误
        ElMessage({
          message: errorMessage || '请求的资源不存在',
          type: 'error',
          duration: 3000
        });
      } else if (status === 500) {
        // 处理500服务器错误
        ElMessage({
          message: errorMessage || '服务器内部错误',
          type: 'error',
          duration: 3000
        });
      } else {
        // 其他错误
        ElMessage({
          message: errorMessage,
          type: 'error',
          duration: 3000
        });
      }
      
      // 创建一个包含响应数据的错误对象
      if (data) {
        const enhancedError = new Error(errorMessage);
        enhancedError.message = errorMessage;
        return Promise.reject(enhancedError);
      }
    } else {
      // 网络错误
      ElMessage({
        message: '网络连接异常，请检查您的网络',
        type: 'error',
        duration: 3000
      });
    }
    
    return Promise.reject(error);
  }
);

export default request; 