import type { AdminModule } from '../layout/types'

/** 经历管理模块（Spec 09）：挂载约定见 layout/types.ts */
export const experienceAdminModule: AdminModule = {
  routes: [
    {
      path: 'experience',
      name: 'admin-experience',
      component: () => import('./ExperienceManageView.vue'),
      meta: { title: '经历管理' },
    },
  ],
  nav: [
    {
      name: 'admin-experience',
      label: '经历管理',
      icon: 'M3 3h10v2H3zM3 8h8v2H3zM3 13h7v2H3zM13 5.5l2 2.5-2 2.5zM13 10.5l2 2.5-2 2.5zM13 15.5l2 2.5-2 2.5z',
    },
  ],
}
