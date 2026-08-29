import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
        // AI analyses run synchronously and can take up to ~2 min with a real LLM.
        proxyTimeout: 180_000,
        timeout: 180_000,
      },
    },
  },
})
