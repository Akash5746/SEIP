import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/auth': 'http://localhost:8880',
      '/expenses': 'http://localhost:8880',
      '/employees': 'http://localhost:8880',
      '/fraud': 'http://localhost:8880',
      '/audit': 'http://localhost:8880',
      '/analytics': 'http://localhost:8880',
    }
  }
})
