import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import type { RouteMeta } from '../types/common'

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/components/layout/AppLayout.vue'),
    meta: {
      requiresAuth: true
    } as RouteMeta,
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: {
          title: '我的云盘',
          icon: 'home',
          keepAlive: true
        } as RouteMeta,
      },
      {
        path: 'folder/:folderId',
        name: 'Folder',
        component: () => import('@/views/Home.vue'),
        meta: {
          title: '文件夹',
          keepAlive: true
        } as RouteMeta,
      },
      {
        path: 'starred',
        name: 'Starred',
        component: () => import('@/views/Starred.vue'),
        meta: {
          title: '收藏夹',
          icon: 'star',
          keepAlive: true
        } as RouteMeta,
      },
      {
        path: 'recycle',
        name: 'RecycleBin',
        component: () => import('@/views/RecycleBin.vue'),
        meta: {
          title: '回收站',
          icon: 'delete',
          keepAlive: true
        } as RouteMeta,
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/Search.vue'),
        meta: {
          title: '搜索结果',
          icon: 'search',
          keepAlive: false
        } as RouteMeta,
      },
      {
        path: 'shares',
        name: 'ShareManage',
        component: () => import('@/views/share/ShareManage.vue'),
        meta: {
          title: '我的分享',
          icon: 'share',
          keepAlive: true
        } as RouteMeta,
      },
      {
        path: 'detail/:fileId',
        name: 'FileDetail',
        component: () => import('@/views/FileDetail.vue'),
        meta: {
          title: '文件详情',
          keepAlive: false
        } as RouteMeta,
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: {
          title: '个人设置',
          icon: 'setting',
          keepAlive: true
        } as RouteMeta,
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: {
      title: '注册',
      requiresAuth: false
    } as RouteMeta,
  },
  {
    path: '/s/:shareId',
    name: 'Share',
    component: () => import('@/views/share/ShareAccess.vue'),
    meta: {
      title: '分享文件',
      requiresAuth: false
    } as RouteMeta,
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: {
      title: '页面不存在',
      requiresAuth: false
    } as RouteMeta,
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  // 设置标题
  const title = to.meta.title ? `${to.meta.title} - 云盘系统` : '云盘系统'
  document.title = title

  // 身份验证检查
  const token = localStorage.getItem('token')
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  
  if (requiresAuth && !token) {
    // 需要认证但无token，重定向到登录页
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  } else if (token && (to.path === '/login' || to.path === '/register')) {
    // 已登录状态下访问登录/注册页，重定向到首页
    next({ path: '/' })
  } else {
    next()
  }
})

export default router 