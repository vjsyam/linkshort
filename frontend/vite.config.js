import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Proxy API requests to Spring Boot backend during development
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Proxy short code redirects to backend in dev
      '^/(?!@|assets|src|node_modules|vite.svg)[a-zA-Z0-9_-]+$': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
