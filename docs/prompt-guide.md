# 赛博终端风格 AI 提示词指南

> 基于 whoami 项目的实际实现，提炼出的可复用提示词模式。
> 目标：让 AI 稳定生成「终端 / 像素 / 赛博霓虹」风格的前端效果。

---

## 一、风格总定义提示词（首次使用必加）

在第一次让 AI 生成相关内容时，先用这段话定义整体风格基调：

```
请按照以下视觉风格生成前端代码：

【视觉方向】终端 / 像素 / 赛博霓虹风格，整体像一台老式 CRT 终端显示器。
- 深色底 + 霓虹绿主色 + 青色/品红色/琥珀色点缀
- 像素字体仅用于 ASCII 标识（logo、代码、编号），中文使用等宽字体
- 动效走「硬件感」：闪烁光标、LED 脉冲、扫描线、CRT 断电/通电、故障闪切

【颜色 Token】所有颜色必须通过 CSS 变量引用，禁止裸写 hex：
- 背景：--bg (#04070b)、--bg-panel (#090f16)、--bg-raised (#0d151f)
- 边框：--border (#16222f)、--border-bright (#24384d)
- 文字：--text (#c8d6e5)、--text-dim (#64788f)
- 主色：--green (#00ff9c)、--green-soft (rgba(0,255,156,.12))、--green-glow (rgba(0,255,156,.35))
- 辅助色：--cyan (#2bd9ff)、--magenta (#ff2e88)、--amber (#ffb800)

【字体 Token】
- --font-pixel: 'Press Start 2P' — 仅用于 ASCII 标识
- --font-term: 'VT323' — 终端感大字、引导日志
- --font-mono: 'Cascadia Code' 等宽栈 — 正文

【动效铁律】
1. 所有动效必须提供 prefers-reduced-motion 降级方案
2. 只动画 transform / opacity / filter，不动 width/height/layout 属性
3. 像素感效果用 steps() 硬切，不用平滑过渡
4. 断电/熄灭用 cubic-bezier(0.55, 0, 0.85, 0.36) 加速曲线
5. 通电/展开用 cubic-bezier(0.23, 1, 0.32, 1) 过冲回弹曲线

【命名约定】
- crt-*：全局 CRT 质感层动画
- vt-*：路由/视图过场动画
- 通用：cursor-blink、led-pulse、rise-in
```

---

## 二、具体效果提示词模板

### 1. CRT 扫描线覆盖层

```
请实现一个全局 CRT 显示器质感覆盖层，要求：
- position: fixed 全屏覆盖，z-index: 9999，pointer-events: none
- 用 repeating-linear-gradient 实现水平扫描线（1px 深色 + 2px 透明重复）
- ::before 伪元素实现缓慢滚动的亮带（从上往下 9 秒循环）
- ::after 伪元素实现暗角（radial-gradient 边缘变暗）
- 整体有微妙的闪烁动画（每 7 秒几次亮度波动）
- prefers-reduced-motion: reduce 时关闭所有动画，隐藏滚动亮带
```

### 2. 故障闪切文字（Glitch Text）

```
请实现赛博风格的故障闪切文字效果，要求：
- 用 ::before 和 ::after 两个伪元素复制文字内容（通过 content: attr(data-text)）
- ::before 为青色 (--cyan)，::after 为品红色 (--magenta)
- 用 clip-path: inset() 裁剪出水平条带，配合 transform: translateX 左右错位
- 动画用 steps(1) 硬切，不要平滑过渡
- 每 7 秒触发一次，持续约 200ms，非常克制
- prefers-reduced-motion 时关闭
```

### 3. 打字机效果

```
请实现终端风格的打字机效果，要求：
- 多句文案循环打字 + 删除
- 打字速度约 95ms/字，删除速度约 24ms/字
- 打完一句后停留约 2.4 秒再删除
- 末尾有闪烁光标（▌字符，steps(1) 每秒闪烁）
- 用 setTimeout 递归实现，不要用 CSS 动画
- 组件卸载时清理定时器
- prefers-reduced-motion 时直接显示完整第一句，不打字
```

### 4. HUD 四角取景框

```
请实现 HUD 风格的四角括号取景框，要求：
- 纯 CSS 实现，用 8 个 linear-gradient 背景分别绘制四个角的横线和竖线
- 四角呈 L 形，不闭合
- 颜色用 --green，默认透明度 0.55
- 支持 CSS 变量自定义：--hud-c（颜色）、--hud-len（角线长度）、--hud-th（线粗）、--hud-inset（内边距）
- pointer-events: none
```

### 5. 霓虹像素按钮

```
请实现霓虹像素风格按钮，要求：
- 像素字体 (--font-pixel)，霓虹绿边框和文字
- 有内发光（inset box-shadow）和外发光（text-shadow + box-shadow）
- hover 时效果：
  1. 背景透明度增加
  2. 发光增强
  3. 文字产生色散效果（text-shadow 左右分别偏移品红和青色）
  4. 轻微上移 1px
- transition 只动 background / box-shadow / transform
- disabled 状态降低透明度，光标变 not-allowed
```

### 6. 像素进度条

```
请实现像素风进度条，要求：
- 外层边框 + 内层填充，整体 196x14px
- 内层填充用 repeating-linear-gradient 实现一格一格的像素感
- 用 transform: scaleX 控制进度，transform-origin: left
- 进度变化用 steps(n) 硬切过渡，不要平滑
- 带霓虹绿外发光
```

### 7. CRT 开关机动画（路由过场）

```
请实现 CRT 显示器开关机动画，要求：

【关机动画（collapse）】约 460ms
- 画面从中心向上下压扁成一条亮线
- 同时亮度和饱和度飙升，模拟电子束聚焦
- 最后缩成极小的亮线后熄灭
- 缓动：cubic-bezier(0.55, 0, 0.85, 0.36) 加速曲线

【开机动画（reveal）】约 520ms
- 从一条水平亮线向上下展开
- 展开时有过冲回弹（scaleY 先超过 1 再回来）
- 亮度从高降到正常
- 缓动：cubic-bezier(0.23, 1, 0.32, 1) 过冲曲线
- 动画结束时完全恢复正常

两个动画都只操作 transform: scaleY/scaleX 和 filter: brightness/saturate
```

### 8. 终端引导日志过场

```
请实现终端风格的引导日志过场，要求：
- 深色背景，绿色终端字体 (--font-term)
- 5 行日志逐行出现（用 steps(1) 硬切，每行间隔约 120ms）
- 内容模拟系统启动日志格式，如 "$ mount /xxx"、"resolving module ......... [ OK ]"
- 其中一行包含像素进度条，逐格填满到 100%
- 最后一行末尾有闪烁光标
- 整体居中显示
```

### 9. 命令式导航栏

```
请实现命令行风格的导航栏，要求：
- 每个导航项格式为 "$ cd /path"，$ 符号用品红色 (--magenta)，命令用终端字体
- 当前页面对应的项高亮为霓虹绿，带发光效果，有边框和浅绿色背景
- 非当前项为灰色 (--text-dim)
- 导航栏固定顶部，半透明毛玻璃背景
- logo 用像素字体 "whoami"，霓虹绿发光
```

### 10. Three.js 3D 粒子场景

```
请用 Three.js 实现赛博风格的 3D 背景场景，要求：
- 线框二十面体（IcosahedronGeometry + WireframeGeometry），霓虹绿色，半透明
- 周围环绕粒子点云（约 400 个点），青色，加法混合 (AdditiveBlending)
- 粒子分布在球壳上（随机半径 + 球坐标转换）
- 二十面体和粒子反向缓慢旋转
- 背景透明，融入页面
- 支持 resize 自适应
- prefers-reduced-motion 时只渲染单帧静态画面，不启动动画循环
- 组件卸载时清理 renderer、raf、resizeObserver
```

---

## 三、组合效果提示词（高阶）

### 首页 Hero 区域

```
请实现一个赛博终端风格的首页 Hero 区域，包含以下元素：

1. 背景：Three.js 3D 线框二十面体 + 粒子点云（异步懒加载，降级模式用静态渐变替代）
2. HUD 四角取景框覆盖在内容区域
3. 顶部命令提示符："$ whoami --verbose"（终端字体，灰色）
4. 大标题：用户名（VT323 字体，霓虹绿，发光，带故障闪切效果）
5. 副标题：打字机效果的多句定位文案，带闪烁光标
6. 入口命令列表：
   - 每项格式为 "$ 命令 # 描述"
   - 左边框高亮，hover 时边框变绿、背景变绿、整体右移 4px
7. 底部滚动提示：像素字体 "SCROLL" + 向下箭头，LED 脉冲闪烁

所有元素按顺序依次上浮出现（rise-in 动画，错开延迟）
prefers-reduced-motion 时关闭所有动画，直接显示最终状态
```

---

## 四、通用修饰词清单

在描述任何效果时，可以按需添加以下修饰词来精准控制风格：

| 想要的感觉 | 修饰词 |
|-----------|--------|
| 像素感 | `steps() 硬切`、`像素字体`、`逐格推进`、`无平滑过渡` |
| 终端感 | `终端字体 VT323`、`命令行格式`、`等宽字体`、`[ OK ] 状态标记` |
| 赛博感 | `霓虹发光`、`故障闪切`、`色散效果`、`加法混合`、`HUD 取景框` |
| 硬件感 | `CRT 扫描线`、`断电/通电`、`电子束`、`LED 脉冲` |
| 克制感 | `非常克制`、`低频率触发`、`低透明度`、`微妙`、`不喧宾夺主` |

---

## 五、避坑提示词

当 AI 生成的效果不对时，用这些提示词修正：

```
请修正以下问题：
1. 所有颜色必须引用 CSS 变量（--green、--bg 等），禁止直接写 hex 色值
2. 像素字体只用于 ASCII 字符，中文必须用等宽字体 (--font-mono)
3. 动画必须加 prefers-reduced-motion 降级，@media 查询中关闭动画
4. 只动画 transform/opacity/filter，不要动画 width/height/top/left
5. 故障效果用 steps(1) 硬切，不要平滑过渡的 glitch
6. 发光效果用 text-shadow 或 box-shadow，不要用 filter: drop-shadow
7. 进度条用 transform: scaleX 驱动，不要用 width 动画
```

---

## 六、完整工作流建议

1. **先给风格定义**：每次开新对话先放「风格总定义」（第一节）
2. **再描述具体组件**：用第二节的模板描述你要的效果
3. **加技术栈约束**：明确告诉 AI 用什么框架（Vue / React / 纯 HTML+CSS）
4. **最后加避坑检查**：生成后对照第五节检查，有问题就修正

**示例完整提示词：**

```
我需要用 Vue 3 + TypeScript 实现一个组件。

【风格定义】
（粘贴第一节内容）

【需求】
请实现一个霓虹像素风格的按钮组件，要求：
（粘贴第二节对应模板，或自己描述）

【技术要求】
- 使用 <script setup lang="ts"> 语法
- scoped style
- 组件 props 类型定义清晰

【避坑】
（粘贴第五节内容）
```
