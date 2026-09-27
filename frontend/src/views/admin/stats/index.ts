import type { AdminModule } from '../layout/types'

/** 访客统计看板模块（Spec 05）：挂载约定见 layout/types.ts */
export const statsAdminModule: AdminModule = {
  routes: [
    {
      path: 'stats',
      name: 'admin-stats',
      component: () => import('./StatsView.vue'),
      meta: { title: '统计看板' },
    },
  ],
  nav: [
    {
      name: 'admin-stats',
      label: '统计看板',
      // 16×16 像素风折线图标（fill=currentColor）
      icon: 'M1 13h2V9H1zM5 13h2V6H5zM9 13h2V3H9zM13 13h2V7h-2zM0 15h16v1H0z',
    },
  ],
}
