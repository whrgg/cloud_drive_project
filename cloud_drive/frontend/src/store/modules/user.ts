import { defineStore } from 'pinia'
import { login, getUserInfo, logout } from '../../api/user'

interface UserState {
  token: string | null
  userInfo: {
    id: number | null
    username: string
    email: string
    role: number
    avatar: string
  } | null
  storageInfo: {
    usedSpace: number
    totalSpace: number
  } | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: localStorage.getItem('token'),
    userInfo: null,
    storageInfo: null
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token,
    storagePercentage: (state) => {
      if (!state.storageInfo) return 0
      return (state.storageInfo.usedSpace / state.storageInfo.totalSpace) * 100
    }
  },
  
  actions: {
    async loginAction(username: string, password: string) {
      try {
        const { data } = await login(username, password)
        this.token = data.token
        localStorage.setItem('token', data.token)
        return true
      } catch (error) {
        return false
      }
    },
    
    async getUserInfoAction() {
      try {
        if (!this.token) return false
        
        const { data } = await getUserInfo()
        this.userInfo = data.userInfo
        this.storageInfo = data.storageInfo
        return true
      } catch (error) {
        return false
      }
    },
    
    async logoutAction() {
      try {
        if (this.token) {
          await logout()
        }
        this.token = null
        this.userInfo = null
        this.storageInfo = null
        localStorage.removeItem('token')
        return true
      } catch (error) {
        return false
      }
    }
  }
}) 