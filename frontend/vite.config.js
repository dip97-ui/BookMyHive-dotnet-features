import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In dev, /api calls are proxied to the Spring Boot server so no CORS setup is needed.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': process.env.JAVA_ORIGIN || 'http://localhost:8080' },
  },
});
