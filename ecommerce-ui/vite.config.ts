import babel from '@rolldown/plugin-babel'
import tailwindcss from '@tailwindcss/vite'
import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react(), babel({ presets: [reactCompilerPreset()] }), tailwindcss()],
  resolve: {
    alias: {
      '@/': fileURLToPath(new URL('./src', import.meta.url)),
      '@/app': fileURLToPath(new URL('./src/app', import.meta.url)),
      '@/shared': fileURLToPath(new URL('./src/shared', import.meta.url)),
      '@/features': fileURLToPath(new URL('./src/features', import.meta.url))
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    environmentOptions: { jsdom: { url: 'http://localhost:8080' } },
    setupFiles: ['./testSetup.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: ['node_modules/', 'src/test/', 'src/mocks/', '**/*.d.ts', 'src/main.tsx', 'src/vite-env.d.ts'],
      thresholds: {
        lines: 80,
        functions: 80,
        branches: 80,
        statements: 80
      }
    }
  }
})
