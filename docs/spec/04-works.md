# Spec 04 — F4 GitHub 作品展示模块（同步 + 置顶/隐藏 + 卡片动效）

> 对应 PRD §F4、§2.3 GitHub 数据链路、§5 页面结构 `/works` 与首页"精选作品"区。术语遵循 `CONTEXT.md`（作品、精选作品、同步、同步日志）。
> **受 ADR-0001 约束：后端定时同步 GitHub REST API 写入 `project` 表，前台永不直连 GitHub。**
> 发布为 GitHub Issue #5（`idonhve/whoami`，标签 `ready-for-agent`）。依赖 Spec 00（地基、表骨架、鉴权）、Spec 05（埋点 SDK）。

## Problem Statement

站主的代码实力主要体现在 GitHub 仓库，但直接贴链接列表既不直观也无法运营（无法置顶重点作品、无法补充中文描述），且前台直连 GitHub API 会受 60 次/小时限流并受制于其可用性。

## Solution

后端定时任务（每日 03:00）+ 后台手动触发，用站主 PAT（只读公开仓库）拉取 GitHub REST API 同步仓库数据入库；管理后台对同步数据叠加运营字段（中文描述、置顶最多 3、隐藏、排序）；前台 `/works` 展示全部未隐藏作品，首页"精选作品"区仅展示置顶项；GitHub 服务不可用时前台展示最近一次同步的缓存数据。

## User Stories

1. 作为面试官，我希望在作品页看到站主仓库的中文名描述、语言色点、star/fork 数与最近更新时间，以便不用跳转 GitHub 也能评估实力。
2. 作为面试官，我希望首页"精选作品"直接呈现最值得看的 ≤ 3 个项目，以便首屏即建立印象。
3. 作为访客，我希望点击作品卡片新标签打开 GitHub 仓库页，以便深入查看代码。
4. 作为技术同行，我希望卡片有滚动渐入与 hover 3D 倾斜效果，以便体验与终端风一致。
5. 作为站主，我希望后台可置顶（最多 3 个）、隐藏、编辑中文描述，以便按求职重点运营作品集。
6. 作为站主，我希望后台有"立即同步"按钮，以便新建仓库后马上上线展示。
7. 作为站主，我希望同步失败在后台可见日志与原因，以便排查 PAT 过期或网络问题。
8. 作为站主，我希望每日 03:00 自动同步一次，以便数据最多滞后一天且无需人工。
9. 作为访客，我希望 GitHub 服务不可用时前台照常展示最近一次同步的数据，以便页面不报错不空白。
10. 作为站主，我希望我的运营字段（中文描述/置顶/隐藏/排序）不被同步覆盖，以便重复同步不丢运营工作。
11. 作为站主，我希望 PAT 通过环境变量注入且只读公开仓库，以便令牌不入库、不下发前端。
12. 作为站主，我希望外跳 GitHub 的点击计入埋点，以便评估作品模块的引流效果。

## Implementation Decisions

### 模块边界与依赖

- 前端：`src/views/works/`（`/works` 页）、`src/components/works/`（RepoCard 卡片：hover 3D 倾斜）、`src/api/works.ts`；后台管理页 `src/views/admin/works/`（列表 + 筛选 + 置顶/隐藏/编辑描述 + 立即同步按钮 + 同步日志视图）。
- 公共文件改动点：`src/router/index.ts`（`/works` 路由）、`src/views/Home.vue`（填充 Spec 01 预留的"精选作品"占位 slot）。
- 后端：`module/project/`（作品实体与运营 CRUD）、`module/github/`（GitHubClient + SyncService + `@Scheduled` 每日 03:00）。
- 环境变量：`GITHUB_TOKEN`（PAT，只读公开仓库权限）、`GITHUB_OWNER`（站主 GitHub 用户名）；均由 Spec 00 `.env.example` 预留。
- 依赖：Spec 00（表骨架、鉴权）；Spec 05 埋点 SDK（卡片点击外跳上报 `github_outbound`）。表结构如需调整用本册迁移区间 **V110–V119**。
- **同步语义（决策）：** 以 GitHub 仓库 `repo_id` 为唯一键 upsert；仓库元数据（名称/描述/star/fork/语言/更新时间/URL）覆盖更新；运营字段（中文描述/置顶/隐藏/排序）保留不覆盖；新入库仓库默认 `is_pinned=false`、`is_hidden=false`；GitHub 上已不存在的仓库不删除，自动置 `is_hidden=true`（防死链），并在同步日志记数。
- **本页 3D/重动效锚点**：`/works` 作品卡片的 hover 3D 倾斜群（本页唯一锚点）。
- 同步为同步执行（HTTP 超时上限 60s），单人后台无需异步队列；每次同步（定时/手动）都写 `sync_task_log`。

### API 契约

通用约定见 Spec 00。

| 方法 | 路径 | 鉴权 | 请求/参数 | 响应 data |
| --- | --- | --- | --- | --- |
| GET | `/api/projects` | 公开 | query `scope`=`all`\|`featured`（默认 `all`） | `ProjectCardDTO[]` |
| GET | `/admin/api/projects` | JWT | query 筛选 `language`/`pinned`/`hidden` | 全量 `ProjectAdminDTO[]` |
| PUT | `/admin/api/projects/{id}` | JWT | `{cnTitle?, isPinned?, isHidden?, sortOrder?}` | 空 |
| POST | `/admin/api/projects/sync` | JWT | 无 | `{status, repoCount, hiddenGone, message}` |
| GET | `/admin/api/projects/sync/logs` | JWT | query `limit`（默认 20） | `SyncLogDTO[]` |

**ProjectCardDTO（公开）：** `id`、`cnTitle`（中文名描述，空则回退仓库 description）、`language`、`stargazersCount`、`forksCount`、`htmlUrl`、`pushedAt`（最近更新）、`isPinned`、`sortOrder`。排序：置顶优先 → `sortOrder` → `pushedAt` 倒序；`scope=featured` 仅置顶且未隐藏；`scope=all` 为全部未隐藏。

**ProjectAdminDTO** 额外含 `repoId`、`repoName`、`isHidden`、`lastSyncedAt`。

**校验/错误：** 置顶第 4 个 → 409（`置顶数量已达上限 3`）；同步失败 → 200 包络内 `status=failed` + `message`（原因），同时写日志。

**GitHub 同步（内部调用，非对外 API）：** `GET /users/{GITHUB_OWNER}/repos?per_page=100&sort=pushed`，逐页拉全；PAT 缺失时同步直接失败并写日志（提示配置 `GITHUB_TOKEN`）。

### 表结构

**`project`**（M1 V1 建骨架，本册如需调整用 V110–V119）：

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK, 自增 | |
| repo_id | bigint | 唯一, 非空 | GitHub 仓库数字 id（同步幂等键） |
| repo_name | varchar(100) | 非空 | 仓库名 |
| full_name | varchar(200) | 非空 | owner/repo |
| cn_title | varchar(200) | 可空 | 中文描述（运营字段，同步不覆盖） |
| description_en | varchar(500) | 可空 | GitHub 原始描述 |
| language | varchar(50) | 可空 | 主语言 |
| stargazers_count | int | 非空, 默认 0 | |
| forks_count | int | 非空, 默认 0 | |
| html_url | varchar(300) | 非空 | 仓库页 URL |
| pushed_at | datetime | 可空 | 最近推送时间 |
| is_pinned | tinyint(1) | 非空, 默认 0 | 置顶（业务上限 3，服务端校验） |
| is_hidden | tinyint(1) | 非空, 默认 0 | 隐藏（同步发现仓库消失时自动置 1） |
| sort_order | int | 非空, 默认 0 | 运营排序 |
| last_synced_at | datetime | 可空 | |
| created_at / updated_at | datetime | 非空 | |

索引：`uk_repo_id (repo_id)`、`idx_is_pinned (is_pinned)`、`idx_is_hidden (is_hidden)`。

**`sync_task_log`：**

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK, 自增 | |
| trigger_type | varchar(16) | 非空 | `scheduled` / `manual` |
| status | varchar(16) | 非空 | `success` / `failed` |
| repo_count | int | 非空, 默认 0 | 本次同步仓库数 |
| hidden_gone | int | 非空, 默认 0 | 因仓库消失被自动隐藏的数量 |
| message | varchar(500) | 可空 | 失败原因（如 PAT 无效、网络超时） |
| started_at / finished_at | datetime | 可空 | |

## 验收标准（逐条搬运自 PRD §F4）

* [ ] 作品卡片展示：仓库中文名描述、语言色点、star/fork 数、最近更新时间、置顶标记

* [ ] 卡片点击跳转 GitHub 仓库页（新标签）

* [ ] 后台可置顶（最多 3 个）、隐藏、编辑中文描述；首页"精选作品"区只显示置顶项，作品页显示全部未隐藏项

* [ ] 定时任务每日自动同步一次；后台有"立即同步"按钮；同步失败在后台可见日志与原因

* [ ] GitHub 服务不可用时，前台展示最近一次同步的缓存数据，不报错、不空白

* [ ] 卡片入场有滚动渐入 + hover 3D 倾斜效果

## Testing Decisions

- 测试缝：同步服务边界（mock GitHub REST API 响应）+ HTTP API 集成测试 + Playwright。不测 GitHub 客户端内部解析细节以外的实现。
- 同步集成测试（WireMock/mock 服务器）：首次同步入库；二次同步元数据更新而运营字段保留；仓库消失自动隐藏并计数；PAT 缺失/401 → `status=failed` 且日志含原因；置顶第 4 个 409。
- 定时任务：不真等 03:00，用可调度入口（把 cron 触发的同一个 service 方法直接调用）测试幂等；cron 表达式本身在配置中断言为 `0 0 3 * * ?`。
- 缓存兜底测试：模拟 GitHub 不可达（同步失败），断言公开列表接口仍返回库内已有数据、无 500。
- Playwright：`/works` 渲染卡片与置顶标记；首页"精选作品"仅置顶项；卡片点击新标签（`target="_blank"`）且触发 `github_outbound` 埋点（拦截网络请求断言）。
- 后台管理页：编辑中文描述 → 前台刷新可见（验收"无需发版"精神在此模块同样适用）。

## Out of Scope

- GitHub README 内容抓取与渲染（只同步仓库元数据）。
- 仓库语言分布图、commit 活跃度图表。
- 同步结果的前台展示（同步日志仅后台可见）。
- 仓库删除的物理清理（仅自动隐藏）。

## Further Notes

- 同步时刻为每日 03:00 + 手动触发（PRD §7.2 代决项，站主可推翻；改动只需调整 cron）。
- `GITHUB_TOKEN` 只读公开仓库权限即可，创建步骤见 `docs/content-checklist.md`。
- 首页"精选作品"区块渲染于 Spec 01 预留 slot，数据接口与卡片组件复用本册实现，避免两处实现漂移。
