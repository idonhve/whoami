import type { AdminModule } from '../layout/types'

/** 站点配置模块（Spec 06）：挂载约定见 layout/types.ts */
export const configAdminModule: AdminModule = {
  routes: [
    {
      path: 'config',
      name: 'admin-config',
      component: () => import('./SiteConfigView.vue'),
      meta: { title: '站点配置' },
    },
  ],
  nav: [
    {
      name: 'admin-config',
      label: '站点配置',
      icon: 'M1 3h9v2H1z M12 3h3v2h-3z M10 2h2v4h-2z M1 11h3v2H1z M6 11h9v2H6z M4 10h2v4H4z',
    },
  ],
}
