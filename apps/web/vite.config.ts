import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// In dev, we'll call /api/* and proxy to http://localhost:8080
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        rewrite: (p) => p.replace(/^\/api/, '')
      }
    }
  }
})
