import type { RouteRecordRaw } from 'vue-router'

/**
 * 后台模块挂载约定（Spec 06 定型，后续模块窗口照做即可零冲突并行）：
 *
 * 各模块在自己的目录 `src/views/admin/<module>/` 下新建 `index.ts`，
 * 导出一个 `AdminModule` 常量：
 *   - routes：该模块管理页的路由配置数组（/admin 下的相对子路径，懒加载组件）
 *   - nav：侧边导航项，name 必须与 routes 中某条路由的 name 一致（高亮/跳转依据）
 *
 * 然后在聚合表 `layout/registry.ts` 里 import 并追加一行即完成挂载。
 * 不要在 router/index.ts 里直接写模块路由，也不要在布局里写死导航项。
 */
export interface AdminModule {
  routes: RouteRecordRaw[]
  nav: AdminNavItem[]
}

export interface AdminNavItem {
  /** 与路由 name 一致 */
  name: string
  /** 导航显示文案（中文，走等宽栈） */
  label: string
  /** 16×16 像素风 SVG path（fill=currentColor），不用 emoji */
  icon: string
}
