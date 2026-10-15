import react from '@vitejs/plugin-react'
import path from 'node:path'
import { defineConfig } from 'vite'

// Vite استاندارد (بدون افزونه Base44). alias «@» قبلاً توسط افزونه فراهم می‌شد.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(import.meta.dirname, './src') },
  },
});
