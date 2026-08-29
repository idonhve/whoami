## Agent 技能

### Issue 跟踪

本仓库的 Issue 记录在 GitHub Issues（`idonhve/whoami`）中，通过 `gh` CLI 操作。详见 `docs/agents/issue-tracker.md`。

### 分诊标签

默认使用五个标准分诊标签，直接原样使用：`needs-triage`、`needs-info`、`ready-for-agent`、`ready-for-human`、`wontfix`。详见 `docs/agents/triage-labels.md`。

### 领域文档

单一上下文结构：根目录 `CONTEXT.md` + `docs/adr/`。详见 `docs/agents/domain.md`。

### 前端设计规范

写或改任何前端（页面、组件、动效、样式）之前，先读 `design-system/whoami/MASTER.md`。它是全站视觉真相源（终端 / 像素 / 赛博霓虹），权威高于 ui-ux-pro-max 等工具的内置建议；页面级覆盖放 `design-system/whoami/pages/`。硬性铁律：只引用 token、动效必带 reduced-motion 降级、像素字体仅用于 ASCII 标识。
