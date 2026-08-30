import type { AdminModule } from '../layout/types'

/** 操作日志模块（Spec 06）：挂载约定见 layout/types.ts */
export const oplogAdminModule: AdminModule = {
  routes: [
    {
      path: 'op-logs',
      name: 'admin-op-logs',
      component: () => import('./OpLogView.vue'),
      meta: { title: '操作日志' },
    },
  ],
  nav: [
    {
      name: 'admin-op-logs',
      label: '操作日志',
      icon: 'M2 2h12v2H2z M2 7h12v2H2z M2 12h8v2H2z',
    },
  ],
}
