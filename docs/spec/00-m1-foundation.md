# Spec 00 — M1 地基（工程脚手架 + 全栈一键起 + 管理员登录鉴权）

> 对应 PRD §6 里程碑 M1 与 `docs/parallel-plan.md` 阶段 2。术语遵循 `CONTEXT.md`。
> **串行优先：本册是唯一串行模块，其余 12 册（F1~F12）全部依赖本册交付物。**
> 发布为 GitHub Issue #1（`idonhve/whoami`，标签 `ready-for-agent`）。

## Problem Statement

12 个功能模块将按 `docs/parallel-plan.md` 由多个会话窗口并行开发。若没有统一交付的地基（工程结构、构建方式、数据库骨架、鉴权与 API 口径），并行窗口会各自发明目录结构、表结构风格和错误约定，导致大量合并冲突与口径不一致。

## Solution

一个窗口一次性交付地基：前端脚手架（Vue3 + Vite + TS + Pinia + Naive UI 暗色定制）、后端脚手架（Spring Boot 3 + MyBatis-Plus + Flyway）、Docker Compose 三容器一键起全栈（Nginx + Java + MySQL）、V1 迁移建齐 12 张表骨架、管理员登录（bcrypt + JWT ≤ 2h + 失败锁定）与全站统一 API 约定。交付后其余窗口"拉 main 即可并行"。

## User Stories

1. 作为站主，我希望 `docker compose up` 一条命令起全栈（前端、后端、MySQL），以便本地零配置开发与验收。
2. 作为站主，我希望初始管理员由 SQL 初始化脚本插入（bcrypt 密文、无明文），以便安全获得首个账号，并按部署文档改密。
3. 作为管理员，我希望用账号密码登录后台获得 JWT，以便安全地维护全站内容。
4. 作为管理员，我希望连续 5 次登录失败后账号锁定 10 分钟，以防暴力破解。
5. 作为管理员，我希望 JWT 2 小时内可续签，以便维护内容时不被频繁踢出。
6. 作为 AFK 开发窗口，我希望拉取 main 后即拥有可运行骨架与权威目录约定，以便只实现自己模块的前后端。
7. 作为 AFK 开发窗口，我希望所有 12 张表已有骨架且字段口径在 spec 中唯一，以便跨窗口表结构不冲突。
8. 作为 AFK 开发窗口，我希望迁移编号区间已被预分配，以便并行窗口的 migration 文件互不覆盖。
9. 作为访客，我希望前台任何页面都不需要登录即可浏览，以便零门槛了解站主。
10. 作为站主，我希望敏感配置（数据库口令、JWT 密钥、GitHub PAT）只存在于 `.env`，以便密钥不进 git、不下发前端。

## Implementation Decisions

### 模块边界与依赖

**本册交付（唯一，其余模块禁止重复实现）：**

- `docker-compose.yml`：nginx（前端静态资源 + 反代 `/api`、`/admin/api`）、java（Spring Boot）、mysql 8.0 三服务；卷：MySQL 数据卷、`uploads` 文件卷（供简历/证书图片存储，Spec 07/08 使用）；`.env` 注入。
- 前端脚手架：Vue 3 + Vite + TypeScript + Pinia + Vue Router + Naive UI（暗色主题定制）+ Three.js/GSAP/ECharts 依赖安装；目录骨架、ESLint/Prettier、按路由分包（PRD §4.1：首包 gzip ≤ 500KB，不含 3D 场景）。
- 后端脚手架：Java 17 + Spring Boot 3.x + MyBatis-Plus；全局异常处理器、统一响应包络、JWT 过滤器（拦截 `/admin/api/**`）、参数校验（`spring-boot-starter-validation`）。
- 数据库：Flyway；`V1__init_schema.sql` 按 Spec 02~09 各册"表结构"建齐 12 张表；`V2__init_seed.sql` 插入初始数据（见下）。
- 管理后台最小可用：`/admin` 登录页 + 空壳布局（后续各模块在布局下挂自己的管理页）。
- `.env.example`（全部键见 Further Notes）与部署文档（含 bcrypt 密文生成方式与首次改密说明）。

**目录约定（权威，各模块窗口必须遵守）：**

- 前端：页面 `src/views/<module>/`、模块私有组件 `src/components/<module>/`、API 客户端 `src/api/<module>.ts`、Pinia store `src/stores/<module>.ts`（如需）。
- 后端：模块代码 `src/main/java/<basepkg>/module/<module>/{controller,service,mapper,entity}`；迁移 `src/main/resources/db/migration/V<n>__<name>.sql`。

**公共文件改动点（并行冲突区，各册另行声明，合并按 `docs/parallel-plan.md` 冲突规避规则）：**

`src/router/index.ts`（各模块追加路由）、`src/views/Home.vue`（由 Spec 01 建立，含"精选作品""简历下载"区块占位，Spec 04/07 填充）、`src/components/layout/AppHeader.vue`/`AppFooter.vue`（由 Spec 01 建立，Spec 03/10 追加图标）、`src/App.vue`（全局层：Spec 06 悬浮按钮、Spec 10 命令面板、Spec 11 彩蛋挂载）、`docker-compose.yml` 与 `CONTEXT.md`（原则上不动，必须改时在 Issue 声明）。

**迁移编号区间（M1 占用 V1~V2；各模块 ALTER 预留区间）：**

| 模块 | 区间 | 模块 | 区间 |
| --- | --- | --- | --- |
| Spec 02 技术栈 | V100–V109 | Spec 07 简历 | V140–V149 |
| Spec 04 作品 | V110–V119 | Spec 08 证书 | V150–V159 |
| Spec 05 统计 | V120–V129 | Spec 09 经历 | V160–V169 |
| Spec 06 后台框架 | V130–V139 | Spec 10/11/12 | 无表，不占用 |

### API 契约（全站通用约定 + 本册鉴权接口）

**通用约定（所有模块 spec 遵守，冲突以本条为准）：**

- 前台接口前缀 `/api`（公开或限流）；管理接口前缀 `/admin/api`（JWT 保护，未登录一律 401）。
- 统一响应包络：`{ "code": 0, "message": "ok", "data": ... }`；失败时 `code` 非 0 且携带 HTTP 状态码（400 参数错误 / 401 未登录 / 403 锁定或禁止 / 404 不存在 / 409 冲突 / 429 限流）。
- 鉴权头：`Authorization: Bearer <token>`；JWT 声明 `sub`（管理员 id）与 `exp`，有效期 ≤ 2 小时。
- 时间格式 ISO-8601 字符串（UTC+8）；数据库列命名 snake_case。
- 上传与文件下载接口使用 `multipart/form-data` 与文件流，其余均 JSON。

**本册接口：**

| 方法 | 路径 | 请求 | 响应 data | 说明 |
| --- | --- | --- | --- | --- |
| POST | `/admin/api/auth/login` | `{username, password}` | `{token, expiresIn}` | 成功清零失败计数；失败计数 +1 |
| POST | `/admin/api/auth/refresh` | 无（凭旧 token，过期前有效） | `{token, expiresIn}` | 续签 |
| GET | `/admin/api/auth/me` | 无 | `{id, username}` | 校验 token 有效性 |
| GET | `/admin/api/health` | 无 | `{status}` | 容器健康检查 |

登录失败锁定：`failed_attempts ≥ 5` 时置 `locked_until = now + 10min`；锁定期内登录返回 403（提示锁定）；成功登录重置计数。前端路由守卫：未持 token 访问 `/admin/*` 子页跳登录页；任何 `/admin/api/*` 401 响应触发跳转。

### 表结构

**`admin_user`（本册实现并维护）：**

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK, 自增 | |
| username | varchar(50) | 唯一, 非空 | 登录名 |
| password_hash | varchar(100) | 非空 | bcrypt 密文 |
| failed_attempts | int | 非空, 默认 0 | 连续失败计数 |
| locked_until | datetime | 可空 | 锁定截止时间 |
| last_login_at | datetime | 可空 | |
| created_at / updated_at | datetime | 非空 | |

**V1 建齐全部 12 张表**（`tech_stack`/`project`/`experience`/`certificate`/`resume_file`/`guest_message`/`visit_log`/`track_event`/`admin_user`/`admin_op_log`/`site_config`/`sync_task_log`），字段以 Spec 02~09 各册"表结构"为唯一口径。**V2 种子数据：** `admin_user` 一条（bcrypt 密文）；`site_config` 初始键值（见下）。

**`site_config` 种子键（本册写入，Spec 06 维护）：**

| config_key | 初始值 | 消费方 |
| --- | --- | --- |
| `domain` | `localhost` | Spec 01（开机日志域名文案） |
| `owner_name` | `站主`（占位） | Spec 01（Hero）、Spec 07（简历文件名） |
| `github_url` | `""`（空，Spec 03 处理空值） | Spec 03（图标跳转） |
| `degrade_force_full` | `false` | Spec 01（强制满血预览开关） |

其余 11 张表的结构见对应模块册，本册只建骨架（空表、无业务代码）。

## 验收标准（逐条搬运自 PRD）

> 来源：PRD §6 M1 完成标志 + §F6 中与登录鉴权相关条目（F6 其余条目归 Spec 06）。

* [x] M1 完成标志：`docker compose up` 一键起全栈；管理员可登录后台（搬运自 PRD §6 M1 行）

* [ ] 后台入口为独立路由 `/admin`（前台不放显式入口）；登录用账号密码（bcrypt 存储）+ JWT，连续失败 5 次锁定 10 分钟（PRD F6）

* [ ] 初始管理员由 SQL 初始化脚本插入（bcrypt 密文，无明文），部署文档说明密文生成与首次改密方式（PRD F6）

* [ ] 未登录访问任何 `/admin/api/*` 接口返回 401，前端路由守卫跳转登录页（PRD F6）

* [ ] JWT 过期时间 ≤ 2h，支持续签（PRD §4.2）

## Testing Decisions

- **测试缝（最高缝）**：HTTP API 边界 + Docker Compose 冒烟。后端用 MockMvc 集成测试打真实 MySQL（Testcontainers），不测 service 内部实现。
- 鉴权集成测试覆盖：登录成功/密码错/用户名错/第 5 次失败触发锁定/锁定期内 403/成功登录清零计数/token 过期 401/续签成功/`/admin/api/**` 未登录 401。
- V1 迁移测试：Flyway 在空库上执行成功且 12 张表结构断言与 spec 一致。
- compose 冒烟：起栈 → `GET /admin/api/health` 200 → 登录成功 → 停栈，写成一键脚本。
- 前端：路由守卫（无 token 访问 `/admin` 跳登录页）组件级测试。

## Out of Scope

- 一切业务 CRUD、页面内容与动效（Spec 01~12）。
- 前台布局与页面切换动效（Spec 01）。
- 操作日志切面与站点配置管理页（Spec 06）。
- GitHub 同步、埋点、地图、留言（Spec 04/05）。
- 生产部署与 HTTPS（PRD M5，延后）。

## Further Notes

- `.env.example` 键：`DB_USER`、`DB_PASSWORD`、`JWT_SECRET`（≥ 32 字符）、`GITHUB_TOKEN`（留空，Spec 04 用）、`GITHUB_OWNER`（留空，Spec 04 用）、`TZ=Asia/Shanghai`。
- 本册完成是 `docs/parallel-plan.md` 阶段 3（模块并行）的启动门槛：只有本 Issue 关闭后，2~4 个模块窗口才开工。
- 各模块窗口开工前 `git pull` 同步 main；PR 描述关联 Issue（如 `Closes #<n>`）。
