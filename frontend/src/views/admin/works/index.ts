import type { AdminModule } from '../layout/types'

/** 作品模块（Spec 04）：挂载约定见 layout/types.ts */
export const worksAdminModule: AdminModule = {
  routes: [
    {
      path: 'works',
      name: 'admin-works',
      component: () => import('./WorksAdminView.vue'),
      meta: { title: '作品管理' },
    },
  ],
  nav: [
    {
      name: 'admin-works',
      label: '作品管理',
      icon: 'M2 2h3v2H2z M7 2h3v2H7z M12 2h2v2h-2z M2 7h12v2H2z M2 12h3v2H2z M7 12h3v2H7z M12 12h2v2h-2z',
    },
  ],
}
