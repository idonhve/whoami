# Spec 07 — F7 简历下载（前台按钮 + 版本管理）

> 对应 PRD §F7、§5 页面结构 `/`（Hero）与 `/about`。术语遵循 `CONTEXT.md`（简历版本）。
> 发布为 GitHub Issue #8（`idonhve/whoami`，标签 `ready-for-agent`）。依赖 Spec 00（地基、uploads 卷、鉴权）、Spec 01（Home Hero 占位）、Spec 05（about 页 slot、埋点事件）。

## Problem Statement

简历 PDF 是求职网站的核心转化物，但常见做法把文件名写死、无版本回滚、下载行为无统计；HR 拿到的可能是旧版本，站主也不知道简历被下载了几次（核心转化指标）。

## Solution

后台维护简历 PDF：上传（仅 pdf、≤ 20MB）自动替换当前版本并保留最近 3 个历史版本可回滚；前台在首页 Hero 与"关于我"区域提供终端风命令样式下载按钮，点击即下载最新版，文件名含姓名与更新日期；下载行为由服务端计入 `resume_download` 埋点；未上传简历时按钮隐藏且不报错。

## User Stories

1. 作为面试官，我希望在首页不滚动就能点到简历下载，以便 30 秒内拿到可转发的 PDF。
2. 作为面试官，我希望下载的文件名含姓名与更新日期（如 `张三_简历_2026-08.pdf`），以便归档不混淆版本。
3. 作为访客，我希望按钮是终端风命令样式并带 hover 光效，以便与全站风格一致。
4. 作为访客，我希望点击即开始下载最新版本，无需跳转或二次确认。
5. 作为站主，我希望上传新 PDF 后旧版自动成为历史版本，以便发错可回滚。
6. 作为站主，我希望保留最近 3 个历史版本，以便快速回退到任一近版。
7. 作为站主，我希望上传限制 pdf 类型与 20MB 大小，以便服务器不被滥用。
8. 作为站主，我希望后台看板可见简历下载次数，以便量化求职转化。
9. 作为站主，我希望尚未上传简历时前台按钮隐藏且不报错，以免死按钮损伤体验。
10. 作为站主，我希望下载事件由服务端记录，以便浏览器下载行为不被前端漏报。

## Implementation Decisions

### 模块边界与依赖

- 前端：`src/components/resume/DownloadButton.vue`（唯一实现，首页 Hero 与 about 复用）；API 客户端 `src/api/resume.ts`；后台管理页 `src/views/admin/resume/`（上传 + 版本列表 + 回滚）。
- 公共文件改动点：`src/views/Home.vue`（填充 Spec 01 预留的 Hero"简历下载"占位）、`src/views/about/`（填充 Spec 05 预留的下载按钮 slot）、`src/router/index.ts` 无新增路由（复用 `/` 与 `/about`）。
- 后端：`module/resume/`（上传/版本/回滚/下载 + 下载埋点）。
- 文件存储：Docker `uploads` 卷（Spec 00 已建）下 `resume/` 目录；服务端生成存储名（uuid + 版本号），物理文件不直接暴露，经下载接口输出。
- 依赖：Spec 00（表骨架、卷）；`owner_name` 取自 `site_config`（Spec 06）拼下载文件名；`resume_download` 事件写入 `track_event`（Spec 05 表结构，**服务端在下载接口内直接写库**，不经前端 SDK——浏览器导航下载无法保证前端上报）。表结构如需调整用本册迁移区间 **V140–V149**。
- 版本策略：每次上传生成新版本号（递增）；`is_current` 唯一指向最新；历史仅保留最近 3 个（第 4 旧的物理文件与记录删除）；回滚 = 将历史版本置 `is_current`（不删除其它版本）。
- 显示文件名规则：`{owner_name}_简历_{上传年月}.pdf`（如 `张三_简历_2026-08.pdf`）。

### API 契约

通用约定见 Spec 00。

| 方法 | 路径 | 鉴权 | 请求 | 响应 | 说明 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/resume/latest` | 公开 | 无 | `{exists, displayName, updatedAt}` | 按钮显隐与文案；`exists=false` 当无任何版本 |
| GET | `/api/resume/download` | 公开 | 无 | `application/pdf` 文件流 | `Content-Disposition: attachment; filename*=UTF-8''<显示名>`；服务端写 `resume_download` 埋点后返回文件；无版本时 404 |
| POST | `/admin/api/resumes` | JWT | multipart `file` | `{id, versionNo}` | 校验类型 pdf、大小 ≤ 20MB，否则 400 |
| GET | `/admin/api/resumes` | JWT | 无 | `ResumeVersionDTO[]` | 倒序版本列表 |
| PUT | `/admin/api/resumes/{id}/restore` | JWT | 无 | 空 | 回滚为当前版 |

**ResumeVersionDTO：** `id`、`versionNo`、`displayName`、`sizeBytes`、`isCurrent`、`uploadedAt`。

**校验/错误：** 非白名单类型/超 20MB → 400（消息含原因）；下载无版本 → 404（前台靠 `latest.exists` 提前隐藏，不触达此分支）。

### 表结构

**`resume_file`**（M1 V1 建骨架，本册调整用 V140–V149）：

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK, 自增 | |
| version_no | int | 非空 | 递增版本号 |
| file_path | varchar(300) | 非空 | 存储相对路径（uploads 卷内） |
| display_name | varchar(100) | 非空 | 下载文件名（`{姓名}_简历_{年月}.pdf`） |
| size_bytes | bigint | 非空 | |
| is_current | tinyint(1) | 非空, 默认 0 | 全表至多一个 1（服务端事务保证） |
| uploaded_at | datetime | 非空 | |
| created_at | datetime | 非空 | |

索引：`idx_version (version_no)`。

## 验收标准（逐条搬运自 PRD §F7）

* [ ] 下载按钮位于首页显眼位置（首屏可达，不用滚动超过一屏）与"关于我"区域，按钮为终端风命令样式（如 `> download resume.pdf`），带 hover 光效

* [ ] 点击即下载最新版本 PDF，文件名含姓名与更新日期（如 `张三_简历_2026-08.pdf`）

* [ ] 下载行为本身计入埋点事件（后台可见简历下载次数——求职网站的核心转化指标）

* [ ] 后台尚未上传简历时，按钮隐藏且不报错

## Testing Decisions

- 测试缝：HTTP 接口集成测试（真实 multipart 上传与流下载）+ 前端组件测试。
- 后端集成测试：上传合法 pdf → `latest.exists=true`；上传 txt/超大文件 → 400；连传 4 版后历史只剩 3 个（旧文件删除）；回滚后 `download` 返回回滚版内容与文件名；每次 `download` 后 `track_event` 多一条 `resume_download`（直查库断言）；无版本时 `latest.exists=false`、`download` 404。
- 文件名断言：`owner_name` 修改后新上传的显示名随之变化。
- 前端组件测试：`exists=false` → 按钮不渲染；`exists=true` → 渲染且点击触发导航下载（`location` 或 `<a download>` 行为 mock）。
- 后台管理页：上传 → 版本列表刷新；点回滚 → `isCurrent` 标记变化。

## Out of Scope

- 简历在线预览/翻页渲染（下载即终点）。
- PDF 内容生成（由站主在站外制作上传，素材见 `docs/content/`）。
- Word/其他格式转换。
- 下载次数的对外展示（仅后台可见）。

## Further Notes

- 下载埋点走服务端直写（不经 tracker SDK），Spec 05 的事件枚举已含 `resume_download`，本册复用同一张 `track_event`，看板曲线由 Spec 05 聚合接口统一提供。
- 保留 3 个历史版本为 PRD 明确数量；超出即删旧，无需站主确认（决策：自动执行，上传成功提示中说明被淘汰的版本号）。
