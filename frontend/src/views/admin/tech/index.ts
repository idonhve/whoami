import type { AdminModule } from '../layout/types'

/** 技术栈管理模块（Spec 02）：挂载约定见 layout/types.ts */
export const techAdminModule: AdminModule = {
  routes: [
    {
      path: 'tech',
      name: 'admin-tech',
      component: () => import('./TechManageView.vue'),
      meta: { title: '技术栈' },
    },
  ],
  nav: [
    {
      name: 'admin-tech',
      label: '技术栈',
      icon: 'M3 13h6v3H3z M15 14h6v2h-6z M9 13h6v4H9z M12 17v4 M12 9l2 3h-4z M12 4l2 4h-4z',
    },
  ],
}