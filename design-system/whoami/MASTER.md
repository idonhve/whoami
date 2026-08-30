# whoami 前端设计系统（Master）

> **全局真相源（Global Source of Truth）**。写或改任何前端页面、组件、动效、样式之前，先读本文件。
> 本文件是项目实际调校后的规范，**权威高于 ui-ux-pro-max 内置库的通用建议**，冲突时以本文件为准。
> 页面级覆盖放 `pages/<name>.md`，覆盖规则优先于本文件（具体页面独有规则）。

## 1. 视觉方向

**终端 / 像素 / 赛博霓虹**。整站像一台老式 CRT 终端：

- 深色底 + 霓虹绿主色 + 青/品红/琥珀点缀
- 像素字体**仅用于 ASCII 标识**（logo、代码、编号），中文一律走等宽栈
- 动效走「硬件感」：闪烁光标、LED 脉冲、扫描线、CRT 断电/通电、故障闪切

## 2. 设计 Token

唯一权威来源：`frontend/src/styles/global.css` 的 `:root`。

### 颜色

| Token | 值 | 用途 |
|-------|-----|------|
| `--bg` | `#04070b` | 页面底色 |
| `--bg-panel` | `#090f16` | 面板 |
| `--bg-raised` | `#0d151f` | 悬浮层 |
| `--border` | `#16222f` | 边框 |
| `--border-bright` | `#24384d` | 高亮边框 |
| `--text` | `#c8d6e5` | 正文 |
| `--text-dim` | `#64788f` | 次要文字 |
| `--green` | `#00ff9c` | **主色（霓虹绿）** |
| `--green-soft` | `rgba(0,255,156,.12)` | 绿底 |
| `--green-glow` | `rgba(0,255,156,.35)` | 绿光晕 |
| `--cyan` | `#2bd9ff` | 辅助青 |
| `--cyan-soft` | `rgba(43,217,255,.12)` | 青底 |
| `--magenta` | `#ff2e88` | 辅助品红 |
| `--amber` | `#ffb800` | 辅助琥珀 |
| `--error` | `#ff3860` | 错误 |

**铁律**：组件内禁止裸写 hex；新增语义色必须先加进 `:root` 再引用。

### 字体

| Token | 字体 | 用途 |
|-------|------|------|
| `--font-pixel` | `'Press Start 2P'` | 仅 ASCII 标识：logo / 代码 / 编号 |
| `--font-term` | `'VT323'` | 终端感的大字、引导日志 |
| `--font-mono` | `'Cascadia Code'` 等 | 正文等宽栈 |

### 尺寸

正文 14px、行高 1.6；新组件间距用 4px 倍数；动效与控件最小可点击尺寸 44×44px。

## 3. 组件 / 模式库

| 模式 | 位置 | 说明 |
|------|------|------|
| `.neon-btn` | `styles/global.css` | 霓虹像素按钮（hover 有色散 + 上浮） |
| `.hud-frame` | `styles/global.css` | 四角括号取景框 |
| `.crt-overlay` | `App.vue` | 全局 CRT 质感层（扫描线 + 暗角 + 滚动亮带 + 闪烁），`z-index:9999`、`pointer-events:none` |
| `RouteTransitionOverlay` | `components/RouteTransitionOverlay.vue` | 路由过场遮罩（引导日志 / 白闪 / 光束 / 故障条） |
| `routeTransition` | `composables/routeTransition.ts` | 路由过场编排（`collapse → boot → reveal`） |
| `.pixel-bar` | `styles/global.css` | 像素进度条（steps 逐格推进，`scaleX` 驱动，开机动画 / 加载场景复用） |
| `FrontLayout` | `components/layout/FrontLayout.vue` | 前台页面骨架（AppHeader 命令式导航 + 内容 + AppFooter），前台页面统一包裹 |

新增的复用模式必须登记进本表，避免同一效果重复造。

## 4. 动效规范

### 命名前缀（按作用域）

- `crt-*`：全局 CRT 质感层（`crt-flicker`、`crt-roll`）
- `vt-*`：路由过场（`vt-crt-off`、`vt-crt-on`、`vt-line-in`、`vt-bar-fill`、`vt-flash-out`、`vt-beam-sweep`、`vt-slice-jitter`）
- 通用：`cursor-blink`、`led-pulse`、`rise-in`

### 缓动约定

- 断电/熄灭：`cubic-bezier(0.55, 0, 0.85, 0.36)`
- 通电/展开：`cubic-bezier(0.23, 1, 0.32, 1)`（过冲回弹）
- 逐格 / 硬切：`steps(1)` 或 `steps(n)`（像素硬切感）

### reduced-motion 铁律（不可省略）

- 所有动效必须提供 `prefers-reduced-motion` 降级。
- CSS：参照 `App.vue` 里 `.crt-overlay` 的 reduce 处理（`animation:none`）。
- JS：动效前先 `prefersReducedMotion()` 检查（参照 `routeTransition.ts`，reduce 时直接放行跳过过场）。

### 性能

- 只动 `transform` / `opacity` / `filter`，**不**动 `width/height/layout`。
- 闪烁、扫描、逐格进度用 `steps()` 保持硬核像素感。

## 5. 目录 / 命名约定

- 复用组件 → `frontend/src/components/`（`PascalCase.vue`）
- 可复用逻辑 → `frontend/src/composables/`（`camelCase.ts`）
- 页面 → `frontend/src/views/`
- 全局样式与 token → `frontend/src/styles/global.css`

## 6. 技术栈（不可随意替换）

Vue 3.5 + TypeScript + Vite 7；Naive UI 2.45（`darkTheme`）；Pinia；vue-router；GSAP 3.15；Three.js；ECharts。不要引入替代这些的库。

## 7. 新增页面前 Checklist

- [ ] 只引用 token，不写裸色 / 裸字体
- [ ] 像素字体仅用于 ASCII 标识，中文走等宽栈
- [ ] 动效带 reduced-motion 降级，只动 transform / opacity / filter
- [ ] 复用 `.neon-btn` / `.hud-frame` / 已登记模式，不重复造
- [ ] 图标用 SVG，不用 emoji
- [ ] 可访问性：对比度 4.5:1、键盘导航、可见 focus
