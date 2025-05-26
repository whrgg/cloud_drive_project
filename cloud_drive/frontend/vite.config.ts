import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    allowedHosts: [
      'localhost',
      '233c850b.r20.cpolar.top', // 明确指定具体的域名
      'another-subdomain.cpolar.top' // 如果有多个域名，需逐一列出
    ],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/file': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/share': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/user': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
