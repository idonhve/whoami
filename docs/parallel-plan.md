# 多会话窗口并行执行计划

## 原则

- **串行的只有两件事**：① 需求/规格文档（保证 12 册 spec 口径一致）② M1 地基脚手架（所有模块依赖它）
- 其余全部并行：每个会话窗口认领 1 个 GitHub Issue（`idonhve/whoami`），独立分支开发，PR 合并
- 协调靠 GitHub Issues + 标签（`ready-for-agent`），不靠人肉记忆

## 阶段表

| 阶段 | 窗口 | 任务 | 依赖 | 并行度 |
| --- | --- | --- | --- | --- |
| 0 需求定稿 | 本窗口 | PRD v1.1 + CONTEXT.md + ADR + 素材清单 | - | 已完成 |
| 1 to-spec | 窗口 A | 按模块生成 `docs/spec/01~12`（每册含 API 契约、表结构、验收标准），并为每册创建 Issue 打 `ready-for-agent` | 阶段 0 | 单窗口（保证一致性） |
| 1' 素材收集 | 窗口 B | 按 `docs/content-checklist.md` 向站主收集素材 → `docs/content/` | 阶段 0 | **可与 A 并行** |
| 2 地基 | 窗口 C | 认领 M1 Issue：前后端脚手架 + docker compose + admin 登录 JWT | 阶段 1 | 已完成（PR #14，含终端/像素/赛博视觉重做） |
| 3 模块并行 | 窗口 D/E/F… | 各认领 1 个模块 Issue，按对应 spec 实现（前后端 + 验收自测） | 阶段 2 | 2~4 窗口并行（首批：F1 #2 + F6 #7，git worktree 隔离） |
| 4 集成验收 | 窗口 G | 联调 + 性能红线 + Lighthouse 本地验收（= PRD 的 M4） | 阶段 3 | 单窗口 |
| 5 上线 | 任意 | 按代码量购境内服务器/域名 + ICP 备案（= PRD 的 M5，站主决定时机） | 阶段 4 | - |

## 模块分配建议（阶段 3）

| 窗口 | 模块 |
| --- | --- |
| 前端 1 | F1 欢迎页 + 首页 Hero |
| 前端 2 | F2 技术栈图表 + F3 GitHub 图标 |
| 前端 3 | F4 作品列表 + F8 照片墙 |
| 前端 4 | F9 经历视觉墙 + F10 命令面板 + F11 彩蛋 |
| 后端 1 | 内容管理 API（技术栈/作品/经历/证书/简历/站点配置） |
| 后端 2 | 统计埋点 API（访问日志/行为事件/地图聚合/留言）+ GitHub 同步任务 |

> spec 中会标注每个模块的目录边界与公共文件改动点，避免并行冲突。

## 冲突规避规则

1. 每窗口独立分支（如 `feat/f4-works`），开工前先同步 main
2. 只改自己模块目录；公共文件（路由表、`CONTEXT.md`、表清单、docker 配置）的改动需在 Issue 中声明，最后合并
3. 表结构：M1 只建骨架，各模块窗口用 migration 维护自己的表，按依赖顺序合并

## 行动指南（复制即用的开场白）

**窗口 A（to-spec，现在就可以开）：**

> 根据 `PRD.md` 与 `docs/parallel-plan.md` 执行 to-spec：按 F1~F12 生成 `docs/spec/01~12` 共 12 册（每册含：模块边界与依赖、API 契约、表结构、验收标准逐条搬运），并为每册在 `idonhve/whoami` 创建 Issue 打 `ready-for-agent` 标签，M1 地基 Issue 单独建并标注"串行优先"。

**窗口 B（素材收集，你在场配合，可与 A 同时开）：**

> 按 `docs/content-checklist.md` 逐区向我收集素材，整理归档到 `docs/content/`（图片与 PDF 存文件，文本存 Markdown）。敏感密钥只写入本地 `.env`，不进 git。

**窗口 C（M1 地基，等 A 完成后开，只开这一个）：**

> 认领 `idonhve/whoami` 中的 M1 地基 Issue 并实现：Vue3+Vite+TS 前端脚手架、Spring Boot 3 + MyBatis-Plus + MySQL 8 后端、docker compose 一键起全栈、admin 登录 + JWT + 失败锁定。

**窗口 D~F（模块并行，等 C 完成后开 2~4 个）：**

> 列出 `idonhve/whoami` 中 `ready-for-agent` 的 Issue，我指定一个，你认领并按对应 spec 实现完整功能（前后端 + 验收项自测），独立分支提 PR。

## 注意

- 每窗口开工前 `git pull` 同步 main
- PR 描述关联 Issue（如 `Closes #3`）
- 阶段 5 上线前不要购买服务器与域名（站主已决策：延后，防浪费）
