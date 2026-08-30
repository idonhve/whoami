import type { RouteRecordRaw } from 'vue-router'

import { configAdminModule } from '../config'
import { oplogAdminModule } from '../oplog'
import type { AdminModule, AdminNavItem } from './types'

/**
 * 后台导航聚合表（公共文件）。
 *
 * 新增模块 = import 该模块的 AdminModule + 在数组追加一行。
 * 本文件是公共改动点：任何修改必须在 Issue / PR 中声明（见 docs/spec/06-admin-cms.md）。
 * 侧边导航与 /admin 子路由都从本表读取，不要在别处另起口径。
 */
export const adminModules: AdminModule[] = [configAdminModule, oplogAdminModule]

export const adminChildRoutes: RouteRecordRaw[] = adminModules.flatMap((m) => m.routes)

export const adminNavItems: AdminNavItem[] = adminModules.flatMap((m) => m.nav)
