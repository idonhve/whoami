# Spec 03 — F3 GitHub 图标跳转（页头/页脚）

> 对应 PRD §F3。术语遵循 `CONTEXT.md`（站点配置）。受 ADR-0001 约束（前台不直连 GitHub，跳转是整页新标签打开，不涉及 API 调用）。
> 发布为 GitHub Issue #4（`idonhve/whoami`，标签 `ready-for-agent`）。依赖 Spec 00 与 Spec 01（布局）、Spec 06（`site_config`）、Spec 05（埋点 SDK）。

## Problem Statement

GitHub 主页是技术访客与面试官评估站主代码实力的关键入口，但需要显眼且可配置的入口；域名或 GitHub 账号变动时不应改代码。

## Solution

页头导航栏与页脚各展示一个 GitHub 图标，带 hover 呼吸光晕动效；点击以 `target="_blank"` 打开站主 GitHub 主页，URL 读取自 `site_config`（改配置不改代码）。图标在暗色背景下通过可访问性对比度检查。

## User Stories

1. 作为面试官，我希望从任意页面一键新标签打开站主 GitHub，以便看真实代码与活跃度。
2. 作为技术同行，我希望图标带 hover 呼吸光晕，以便一眼识别这是可交互入口。
3. 作为访客，我希望图标在暗色终端风背景下清晰可见，以便不用眯眼找。
4. 作为站主，我希望 GitHub 链接来自站点配置，以便账号/链接变更时后台改配置即可。
5. 作为站主，我希望链接尚未配置时图标不出现在页面上（而非 404 跳转），以便配置前不留死链。
6. 作为站主，我希望点击外跳计入 GitHub 外跳埋点，以便知道这个入口带来了多少跳转（求职转化参考）。

## Implementation Decisions

### 模块边界与依赖

- 前端：`src/components/shared/GithubIcon.vue`（唯一实现，页头页脚复用同一组件）。
- 公共文件改动点：`AppHeader.vue` 与 `AppFooter.vue`（Spec 01 已建，本册仅追加图标挂载，合并时声明）。
- 依赖：Spec 06 `GET /api/site-config` 的 `githubUrl`（空字符串时组件整体不渲染）；Spec 05 埋点 SDK（点击时上报 `github_outbound` 事件，依赖其 `POST /api/track/event`）。
- 无后端代码、无表、无新增 API。
- hover 动效：呼吸光晕（CSS box-shadow 动画），不使用 JS 动画库，不占用本页锚点配额。

### API 契约

无新增。消费：

- `GET /api/site-config` → `data.githubUrl`（Spec 06）。
- `POST /api/track/event`，`eventType: "github_outbound"`，`detail: { source: "header" | "footer" }`（Spec 05）。

### 表结构

无。消费 `site_config` 的 `github_url` 键（结构与维护见 Spec 06）。

## 验收标准（逐条搬运自 PRD §F3）

* [ ] GitHub 图标位于页头导航栏与页脚，带 hover 动效（如呼吸光晕）

* [ ] 点击以 `target="_blank"` 打开 GitHub 主页，URL 来自后台配置（`site_config`），改配置不改代码

* [ ] 图标在暗色背景下清晰可见，通过可访问性对比度检查

## Testing Decisions

- 测试缝：组件渲染 + 配置驱动的行为，不测 CSS 动画帧细节。
- 组件测试：`githubUrl` 为空 → 不渲染；非空 → 渲染且 `href`/`target="_blank"`/`rel="noopener noreferrer"` 正确；点击触发埋点调用（mock SDK）。
- 契约测试：后台修改 `github_url` 后前台（刷新）图标链接随之变化。
- 可访问性：对比度用工具断言（图标前景与背景对比度 ≥ 3:1，图形类组件标准），并带 `aria-label`（如"GitHub 主页"）。

## Out of Scope

- 作品卡片点击跳转仓库页（Spec 04，各自埋点）。
- GitHub 数据拉取与展示（Spec 04）。
- 页头/页脚布局本身（Spec 01）。

## Further Notes

- `rel="noopener noreferrer"` 是安全要求，验收第三条的对比度检查使用 axe 或等价工具跑全页报告即可顺带覆盖。
