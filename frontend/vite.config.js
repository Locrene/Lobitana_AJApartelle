import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server proxies /api to the Spring Boot backend, so the browser sees one origin
// and the httpOnly SameSite=Strict auth cookie works without extra CORS setup.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
});
