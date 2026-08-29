# Spec 06 — F6 管理后台框架（站点配置 + 操作日志 + 悬浮管理入口）

> 对应 PRD §F6 中"框架与横切能力"部分。术语遵循 `CONTEXT.md`（管理员、站点配置、操作日志）。
> 发布为 GitHub Issue #7（`idonhve/whoami`，标签 `ready-for-agent`）。依赖 Spec 00（地基：登录/JWT/后台空壳已交付）。
> **分册说明：F6 的登录鉴权条目由 Spec 00（M1 地基）交付；各内容管理页面与 CRUD API 归对应模块册（02/04/05/07/08/09）；本册只交付后台框架层与横切能力。**

## Problem Statement

全站十余类动态数据分散在各模块后台管理，若无统一的后台框架（布局、守卫、配置中心、操作留痕），各模块窗口会各自造管理页骨架与配置读取方式，口径漂移且无法审计。

## Solution

在 M1 登录空壳之上交付后台框架：统一布局与导航（各模块管理页挂载）、路由守卫与 401 拦截续签、站点配置管理页（键值对 CRUD，公开白名单下发）、操作日志（AOP 切面自动记录全部 `/admin/api` 写操作，可查询追溯）、前台仅管理员可见的悬浮"管理"按钮。

## User Stories

1. 作为管理员，我希望登录后进入统一布局的后台，侧边导航直达各模块管理页，以便高效维护内容。
2. 作为管理员，我希望会话过期时自动跳登录页而非报错，以便不被未处理 401 打断。
3. 作为站主，我希望在后台改站点配置（GitHub 链接、域名占位、姓名、降级开关），以便所有模块读同一配置源、改配置不改代码。
4. 作为站主，我希望敏感配置键永不下发前台，以便公开接口无泄露。
5. 作为管理员，我希望所有后台写操作自动留痕（谁、何时、改了什么），以便误操作可追溯。
6. 作为站主，我希望在前台任意页面看到悬浮"管理"按钮（仅我登录时可见），以便快速进后台。
7. 作为访客，我希望看不到任何后台入口与悬浮按钮，以便前台保持纯净终端风。
8. 作为开发窗口，我希望挂载管理页只需按约定注册路由与导航项，以便并行开发零冲突。

## Implementation Decisions

### 模块边界与依赖

- 前端：`src/views/admin/layout/`（后台布局、侧边导航、导航项注册约定）、`src/views/admin/config/`（站点配置管理页）、`src/views/admin/oplog/`（操作日志查询页）、`src/components/admin/FloatingAdminButton.vue`（前台悬浮管理按钮）；API 客户端 `src/api/admin/config.ts`、`src/api/admin/oplog.ts`。
- 公共文件改动点：`src/App.vue`（挂载悬浮管理按钮）、`src/router/index.ts`（后台子路由与守卫完善）、前端 HTTP 客户端（401 拦截 → 尝试续签 → 跳登录页，挂在全局 api 客户端）。
- 后端：`module/siteconfig/`（管理 CRUD + 公开白名单读取）、`module/oplog/`（AOP 切面 + 查询接口）。
- 依赖：Spec 00（登录/JWT/表骨架/后台空壳布局，本册在其上完善）。表结构如需调整用本册迁移区间 **V130–V139**。
- **导航项注册约定（避免并行冲突）：** 各模块管理页在自己模块目录内导出路由配置数组，后台布局侧边导航从聚合表读取；聚合表新增一行即算公共文件改动点，在 Issue 中声明。
- 悬浮管理按钮可见性：本地存在 token 且 `GET /admin/api/auth/me` 校验通过 → 显示；点击进入 `/admin`。
- 后台无重动效（不占用任何 3D 锚点配额，性能预算优先给前台）。

### API 契约

通用约定见 Spec 00。

| 方法 | 路径 | 鉴权 | 请求 | 响应 data | 说明 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/site-config` | 公开 | 无 | `{domain, ownerName, githubUrl, degradeForceFull, ...}` | **仅公开白名单键**；敏感键永不下发 |
| GET | `/admin/api/site-config` | JWT | 无 | `[{key, value, description, updatedAt}]` | 全量 |
| PUT | `/admin/api/site-config/{key}` | JWT | `{value}` | 空 | 键存在则更新，不存在 404 |
| GET | `/admin/api/op-logs` | JWT | query `page`/`size`（默认 1/20） | `{list:[OpLogDTO], total}` | 倒序分页 |

**公开白名单键（初始）：** `domain`、`owner_name`、`github_url`、`degrade_force_full`。新增公开键需在本册登记；其余键一律只限后台。

**操作日志切面（AOP）：** 拦截全部 `/admin/api/**` 的 POST/PUT/DELETE（含各模块管理接口，自动生效，各模块无需自己埋日志），记录：管理员 id、HTTP 方法与路径、资源 id（可从路径/参数提取）、请求参数摘要（脱敏后）、IP、时间。登录成功/失败也记录。

### 表结构

**`site_config`**（M1 V1 建骨架 + V2 种子，本册维护）：

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK, 自增 | |
| config_key | varchar(64) | 唯一, 非空 | 键（snake_case） |
| config_value | text | 可空 | 值（统一字符串存储，消费方自行转型） |
| description | varchar(200) | 可空 | 用途说明（后台管理页展示） |
| updated_by | bigint | 可空 | 最后修改的管理员 id |
| updated_at | datetime | 非空 | |

**`admin_op_log`：**

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK, 自增 | |
| admin_user_id | bigint | 非空 | 操作者 |
| action | varchar(16) | 非空 | HTTP 方法（LOGIN 也记） |
| resource | varchar(200) | 非空 | 请求路径 |
| resource_id | varchar(64) | 可空 | 目标资源 id |
| detail | json | 可空 | 参数摘要（密码等敏感字段脱敏为 ***） |
| ip | varchar(45) | 非空 | |
| created_at | datetime | 非空 | |

索引：`idx_admin_created (admin_user_id, created_at)`、`idx_resource (resource)`。

## 验收标准（逐条搬运自 PRD §F6，登录相关 4 条由 Spec 00 交付）

* [x] 后台入口为独立路由 `/admin`（前台不放显式入口）；登录用账号密码（bcrypt 存储）+ JWT，连续失败 5 次锁定 10 分钟（Spec 00 交付）

* [x] 初始管理员由 SQL 初始化脚本插入（bcrypt 密文，无明文），部署文档说明密文生成与首次改密方式（Spec 00 交付）

* [ ] 管理员登录后，前台页面出现悬浮的"管理"按钮（仅管理员可见）

* [x] 未登录访问任何 `/admin/api/*` 接口返回 401，前端路由守卫跳转登录页（Spec 00 交付）

* [ ] 所有后台操作有操作日志（`admin_op_log`），可追溯

> F6 其余验收条目（简历/作品/技术栈/证书/经历管理、统计看板、留言管理）分别搬运于对应模块册并在彼处验收。

## Testing Decisions

- 测试缝：HTTP API 集成测试（AOP 生效性通过真实调用断言落库）+ 前端守卫组件测试。
- site_config：公开接口只含白名单键（契约断言：新增敏感键后公开响应不含该键）；PUT 更新后公开值变化；未知键 404。
- op_log 集成测试：调用任一模块写接口（如技术栈新增）后 `admin_op_log` 出现对应记录且敏感字段脱敏；登录成功/失败各留一条；分页正确。
- 前端：未持 token 访问 `/admin/xxx` → 跳登录页；持有效 token 访问 → 进布局；token 失效（401）→ 自动尝试续签一次，失败跳登录。
- 悬浮按钮：mock `me` 成功 → 前台显示；失败/未登录 → 不渲染。

## Out of Scope

- 各内容模块的管理页面与 CRUD API（Spec 02/04/05/07/08/09）。
- 登录/JWT/锁定（Spec 00 已交付，本册只做 401 拦截续签的前端完善）。
- 后台主题切换、多管理员、角色权限（全站仅一人）。

## Further Notes

- 本册是"框架"册：模块窗口管理页挂载约定（路由数组导出 + 导航聚合表）一旦在本册定型，其余窗口照做即可零冲突并行。
- `site_config` 值统一字符串存储（含 boolean 也存 "true"/"false"），避免类型迁移成本；消费方转型。
