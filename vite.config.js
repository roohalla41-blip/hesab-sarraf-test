import react from '@vitejs/plugin-react'
import path from 'node:path'
import { defineConfig } from 'vite'

// Independent Vite build; no Base44 plugin or SDK.
export default defineConfig({
  base: '/hesab-sarraf-test/',
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(import.meta.dirname, './src') },
  },
})
