import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 创建axios实例
const service = axios.create({
  baseURL: '', // 不使用前缀，因为API路径已包含/api前缀
  timeout: 30000 // 请求超时时间
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 从本地存储获取token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 如果返回的状态码不是200，说明接口请求有问题
    if (res.code !== 200) {
      ElMessage({
        message: res.message || '系统错误',
        type: 'error',
        duration: 5 * 1000
      })
      
      // 401: 未登录或token过期
      if (res.code === 401) {
        // 清除token
        localStorage.removeItem('token')
        // 跳转到登录页面
        router.replace({
          path: '/login',
          query: { redirect: router.currentRoute.value.fullPath }
        })
      }
      
      return Promise.reject(new Error(res.message || '系统错误'))
    } else {
      return res
    }
  },
  error => {
    console.error('响应错误:', error)
    
    // 处理网络错误
    let message = '网络错误，请稍后重试'
    if (error.response) {
      switch (error.response.status) {
        case 401:
          message = '未授权，请重新登录'
          // 清除token
          localStorage.removeItem('token')
          // 跳转到登录页面
          router.replace({
            path: '/login',
            query: { redirect: router.currentRoute.value.fullPath }
          })
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求的资源不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = `连接错误${error.response.status}`
      }
    }
    
    ElMessage({
      message: message,
      type: 'error',
      duration: 5 * 1000
    })
    
    return Promise.reject(error)
  }
)

export default service 