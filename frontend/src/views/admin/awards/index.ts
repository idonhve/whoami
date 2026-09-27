import type { AdminModule } from '../layout/types'

/** 证书模块（Spec 08）：挂载约定见 layout/types.ts */
export const awardsAdminModule: AdminModule = {
  routes: [
    {
      path: 'awards',
      name: 'admin-awards',
      component: () => import('./AwardsManageView.vue'),
      meta: { title: '证书管理' },
    },
  ],
  nav: [
    {
      name: 'admin-awards',
      label: '证书管理',
      // 像素取景框 + 四角星（照片墙意象，fill=currentColor）
      icon: 'M2 2h12v2H2z M2 12h12v2H2z M2 2h2v12H2z M12 2h2v12h-2z M8 4.5l.8 2.7 2.7 .8-2.7 .8-.8 2.7-.8-2.7-2.7-.8 2.7-.8z',
    },
  ],
}
