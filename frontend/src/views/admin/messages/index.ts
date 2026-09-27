import type { AdminModule } from '../layout/types'

/** 留言管理模块（Spec 05）：挂载约定见 layout/types.ts */
export const messagesAdminModule: AdminModule = {
  routes: [
    {
      path: 'messages',
      name: 'admin-messages',
      component: () => import('./MessagesAdminView.vue'),
      meta: { title: '留言管理' },
    },
  ],
  nav: [
    {
      name: 'admin-messages',
      label: '留言管理',
      // 16×16 像素风气泡图标（fill=currentColor）
      icon: 'M1 2h14v9H8l-4 4v-4H1zM3 4v5h2v2l2-2h6V4z',
    },
  ],
}
