import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { adminChildRoutes } from '@/views/admin/layout/registry'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/Home.vue'),
    },
    // 前台核心区块（F1 建立路由骨架，内容由各 Spec 模块替换 PlaceholderView 填充）
    {
      path: '/works',
      name: 'works',
      component: () => import('@/views/PlaceholderView.vue'),
      meta: { title: 'works', spec: 'SPEC-04' },
    },
    {
      path: '/tech',
      name: 'tech',
      component: () => import('@/views/PlaceholderView.vue'),
      meta: { title: 'tech', spec: 'SPEC-02' },
    },
    {
      path: '/experience',
      name: 'experience',
      component: () => import('@/views/PlaceholderView.vue'),
      meta: { title: 'experience', spec: 'SPEC-09' },
    },
    {
      path: '/awards',
      name: 'awards',
      component: () => import('@/views/PlaceholderView.vue'),
      meta: { title: 'awards', spec: 'SPEC-08' },
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('@/views/PlaceholderView.vue'),
      meta: { title: 'about', spec: 'SPEC-05/07' },
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('@/views/admin/LoginView.vue'),
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/layout/AdminLayout.vue'),
      children: [
        {
          path: '',
          name: 'admin-home',
          component: () => import('@/views/admin/AdminIndex.vue'),
        },
        // 模块管理页路由来自聚合表（views/admin/layout/registry.ts），
        // 新模块不要在此处手写路由，按约定在模块目录导出 AdminModule
        ...adminChildRoutes,
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      redirect: '/',
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path.startsWith('/admin') && to.name !== 'admin-login' && !auth.token) {
    return { name: 'admin-login', query: { redirect: to.fullPath } }
  }
})

export default router
