import type { AdminModule } from '../layout/types'

/** 简历管理模块（Spec 07）：挂载约定见 layout/types.ts */
export const resumeAdminModule: AdminModule = {
  routes: [
    {
      path: 'resume',
      name: 'admin-resume',
      component: () => import('./ResumeManageView.vue'),
      meta: { title: '简历管理' },
    },
  ],
  nav: [
    {
      name: 'admin-resume',
      label: '简历管理',
      icon: 'M7 1h2v7h2.5L8 11.5 4.5 8H7zM2 13h12v2H2z',
    },
  ],
}
