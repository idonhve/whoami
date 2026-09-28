/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 后端 API 基址（分域部署时注入，如 https://xxx.onrender.com）；同源部署留空 */
  readonly VITE_API_BASE?: string
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'

  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}
