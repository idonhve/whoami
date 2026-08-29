# Spec 10 — F10 命令面板（Ctrl+K）

> 对应 PRD §F10。术语遵循 `CONTEXT.md`（命令面板）。_Avoid_：搜索框。
> 发布为 GitHub Issue #11（`idonhve/whoami`，标签 `ready-for-agent`）。依赖 Spec 00（地基）、Spec 01（布局与路由）、Spec 05（埋点 SDK）、Spec 03/07（github/resume 命令的落地行为）。

## Problem Statement

多页面站点中，鼠标多级导航慢且与终端风人设割裂；技术型访客期待键盘优先的全站导航，且导航行为需要被统计以评估该特性价值。

## Solution

全站可唤出的终端风命令面板：`Ctrl+K`（Mac `Cmd+K`）或点击页头 `>_` 图标唤起；支持 `home / works / tech / experience / awards / resume / message / github / theme` 等命令，模糊匹配、上下键选择、回车执行；`help` 列出全部命令、未知命令返回 `command not found`；执行带回显动画（`> navigating to works ...`）；历史记录存 localStorage 最近 5 条；面板使用计入 `cmd_palette_use` 埋点。

## User Stories

1. 作为技术同行，我希望 `Ctrl+K` 一步唤起命令面板，以便键盘全站导航。
2. 作为技术同行，我希望模糊匹配 + 上下键 + 回车的操作模型，以便像用真终端一样高效。
3. 作为访客，我希望点击页头 `>_` 图标也能唤起，以便不知道快捷键的用户不迷路。
4. 作为访客，我希望输入 `help` 看到全部命令，以便自学可用命令。
5. 作为访客，我希望输错命令得到 `command not found` 的终端风提示，以便体验有梗不冷场。
6. 作为访客，我希望执行命令有 `> navigating to works ...` 回显动画，以便操作有反馈。
7. 作为访客，我希望 `resume` 命令直接触发简历下载，以便两步拿到转化物。
8. 作为访客，我希望 `github` 命令新标签打开站主 GitHub，以便一键跳转。
9. 作为访客，我希望 `theme` 命令切换主题强调色并被记住，以便个性化。
10. 作为访客，我希望历史命令（最近 5 条）可快速重选，以便重复导航更省事。
11. 作为站主，我希望面板使用次数进入埋点统计，以便评估该特性的价值。
12. 作为移动端访客，我希望 `>_` 图标路径可用且面板样式自适应，以便触屏也能用。

## Implementation Decisions

### 模块边界与依赖

- 前端：`src/components/command-palette/`（面板组件、命令注册表、模糊匹配工具、回显动画、历史管理）；公共文件改动点：`src/App.vue`（全局挂载面板层）、`src/components/layout/AppHeader.vue`（追加 `>_` 图标按钮）。
- 命令注册表为模块内常量：命令名、描述、执行函数（导航/下载/外跳/切主题）。**命令表集中本册维护**，不开放各模块注册（避免并行冲突；新增命令在本册追加）。
- 依赖：路由（Spec 01）；`resume` 命令调用 Spec 07 的下载行为（复用其 API 客户端触发下载）；`github` 命令读 `site_config.githubUrl`（Spec 06）并复用 Spec 03 的外跳与埋点；`theme` 切换全局强调色 CSS 变量（Spec 01 定义的 token），localStorage 键 `whoami:theme`；埋点：打开并执行命令时经 Spec 05 tracker SDK 上报 `cmd_palette_use`（`detail: {command}`）。
- 历史：localStorage 键 `whoami:cmd-history`，最多 5 条，去重、最新在前。
- 无后端代码、无表、无新增 API。
- 键盘细节：面板打开时 `Esc` 关闭；`↑`/`↓` 移动选中项；`Enter` 执行；输入焦点自动落输入框；全局快捷键在面板关闭时监听（capture 阶段避免被页面内输入框抢占——输入框聚焦时仍响应 `Ctrl+K`）。

### API 契约

无新增。消费：路由导航（内部）、`GET /api/site-config`（`githubUrl`）、`GET /api/resume/latest` + 下载（Spec 07）、`POST /api/track/event`（`cmd_palette_use`，Spec 05）。

### 表结构

无。`cmd_palette_use` 事件写入 Spec 05 的 `track_event`（`detail` 存命令名）。

## 验收标准（逐条搬运自 PRD §F10）

* [ ] `Ctrl+K`（Mac `Cmd+K`）或点击页头 `>_` 图标唤起命令面板

* [ ] 支持命令：`home / works / tech / experience / awards / resume / message / github / theme` 等，含模糊匹配与上下键选择、回车执行

* [ ] 输入 `help` 显示全部命令列表；未知命令返回终端风提示 `command not found`

* [ ] 命令执行带终端风回显动画（`> navigating to works ...`）

* [ ] 面板有历史记录（localStorage，最近 5 条）

## Testing Decisions

- 测试缝：组件行为（键盘事件模型）+ Playwright；不测匹配算法内部评分细节，测契约（输入 `wrk` 命中 `works` 等）。
- 组件测试：快捷键唤起/`Esc` 关闭/页面输入框聚焦时 `Ctrl+K` 仍唤起；`↑/↓/Enter` 选中执行；`help` 渲染全部命令；未知命令渲染 `command not found`；模糊匹配样例（`expr` → `experience`）；历史存取与 5 条上限、去重；执行导航命令时 mock 路由跳转断言目标。
- Playwright：真实 `Ctrl+K` → 输入 `works` → 回车 → 路由变化；执行命令产生 `cmd_palette_use` 埋点请求（拦截断言）；`resume` 命令触发下载导航；`theme` 切换后刷新仍保持（localStorage）。
- 移动端：`>_` 图标点击唤起，面板宽度自适应。

## Out of Scope

- 自然语言命令/模糊语义搜索（只做前缀与模糊子串匹配）。
- 各模块动态注册命令（命令表集中维护）。
- 面板内嵌内容预览。

## Further Notes

- `message` 命令导航至 `/about` 并自动聚焦留言板输入框（与 Spec 05 的锚点联动）。
- 回显动画文案统一格式：`> <动作> <目标> ...`，动作词表（navigating / downloading / opening / switching）集中常量维护。
