# Spec 02 — F2 技术栈展示模块（饼图 + 条状图 + 后台管理）

> 对应 PRD §F2 与 §5 页面结构 `/tech`。术语遵循 `CONTEXT.md`（技术栈、熟练度）。
> 发布为 GitHub Issue #3（`idonhve/whoami`，标签 `ready-for-agent`）。依赖 Spec 00（地基、表骨架、鉴权）。

## Problem Statement

简历上的技术栈通常是文字罗列，面试官无法在几秒内感知技术版图与深度；且文字版内容改动需要改代码发版。

## Solution

`/tech` 页以两张图表可视化技术栈：饼图按分类（前端/后端/数据库/工具等）展示占比，水平条状图展示单项熟练度（条形渐变发光填充动画）。每项技术含名称、图标与三档熟练度标签。全部数据由后台维护，增删改后前台刷新即见，无需发版。

## User Stories

1. 作为面试官，我希望一眼从饼图看懂站主的技术版图分布，以便快速判断岗位匹配度。
2. 作为面试官，我希望从条状图看出每项技术的熟练深度与精通项，以便锁定追问点。
3. 作为面试官，我希望每项技术有图标与"精通/熟练/了解"标签，以便不读文字也能建立认知。
4. 作为技术同行，我希望 hover 图表出现明细 tooltip，以便看到权重与分类细节。
5. 作为访客，我希望图表入场有滚动触发的渐入动画，以便页面有"被设计过"的质感。
6. 作为移动端访客，我希望图表自适应且可交互，以便手机上不横向滚动。
7. 作为站主，我希望后台增删改技术项（名称/图标/分类/熟练度/权重/排序），以便求职重点变化时立即调整。
8. 作为站主，我希望权重可自定义，以便饼图占比反映我的真实侧重而非数量。
9. 作为站主，我希望改完后台前台刷新即生效，以便不发版维护内容。

## Implementation Decisions

### 模块边界与依赖

- 前端：`src/views/tech/`（`/tech` 页）、`src/components/tech/`（饼图、条状图、技术项卡片）、`src/api/tech.ts`；公共文件改动点：`src/router/index.ts` 追加 `/tech` 路由。
- 后端：`module/techstack/`（controller/service/mapper/entity，含公开与管理两组接口）。
- 后台管理页：`src/views/admin/tech/`（挂在 Spec 00 的后台空壳布局下）。
- 依赖：Spec 00（表骨架 `tech_stack` 已在 V1 建；鉴权与包络）。如需调整表结构用本册预留迁移区间 **V100–V109**。
- 图表库：ECharts（饼图 + 条状图，条状图渐变发光填充用 ECharts linearGradient 实现）；图标用 devicon 图标名渲染。
- **本页 3D/重动效锚点**：条状图渐变发光填充动画（`/tech` 页唯一锚点，滚动触发渐入）。

### API 契约

通用约定（包络/鉴权/401 兜底）见 Spec 00。

| 方法 | 路径 | 鉴权 | 请求 | 响应 data |
| --- | --- | --- | --- | --- |
| GET | `/api/tech-stack` | 公开 | 无 | `TechItem[]`（按 `sortOrder` 升序） |
| GET | `/admin/api/tech-stack` | JWT | 无 | `TechItem[]` 全量 |
| POST | `/admin/api/tech-stack` | JWT | `TechItemCreate` | `{id}` |
| PUT | `/admin/api/tech-stack/{id}` | JWT | `TechItemCreate` | 空 |
| DELETE | `/admin/api/tech-stack/{id}` | JWT | 无 | 空 |

**DTO 字段：** `id`、`name`、`icon`（devicon 图标名，可空=无图标）、`category`（分类字符串：前端/后端/数据库/工具/其他等，自由值）、`proficiency`（`master` | `proficient` | `familiar`，展示为 精通/熟练/了解）、`weight`（饼图权重，≥1 整数）、`sortOrder`。

**校验（400）：** `name` 必填 ≤ 50 字符；`proficiency` 必须为三档枚举；`weight` 1~100；`category` 必填 ≤ 20 字符。

### 表结构

**`tech_stack`**（M1 V1 建骨架，本册如需调整用 V100–V109）：

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK, 自增 | |
| name | varchar(50) | 非空 | 技术名称（如 Vue 3） |
| icon | varchar(50) | 可空 | devicon 图标名 |
| category | varchar(20) | 非空 | 分类（前端/后端/数据库/工具/其他…） |
| proficiency | varchar(16) | 非空 | master / proficient / familiar |
| weight | int | 非空, 默认 1 | 饼图权重 |
| sort_order | int | 非空, 默认 0 | 展示排序 |
| created_at / updated_at | datetime | 非空 | |

索引：`idx_category (category)`（饼图聚合用）。

## 验收标准（逐条搬运自 PRD §F2）

* [ ] 提供饼图（按分类：前端/后端/数据库/工具等的数量或自定权重占比）与水平条状图（单项熟练度，条形有渐变发光填充动画）

* [ ] 每项技术含名称 + 图标（devicon 或同类图标库）+ 熟练度标签（如 精通/熟练/了解）

* [ ] 图表入场有滚动触发的渐入动画，hover 有 tooltip 明细

* [ ] 技术栈数据全部来自数据库，后台增删改后前台刷新即可见，无需发版

* [ ] 移动端图表自适应，可正常交互

## Testing Decisions

- 测试缝：HTTP API（集成测试打真实库）+ 前端组件测试 + Playwright 冒烟，只测外部行为。
- 后端集成测试：公开列表返回排序正确；未登录访问管理接口 401；CRUD 全流程后公开接口反映变更；枚举/长度校验返回 400。
- 前端组件测试：饼图数据按 category 聚合 weight；条形长度映射熟练度档位；tooltip 内容。
- Playwright：`/tech` 页渲染图表、滚动触发渐入（断言容器 class/opacity 变化）；移动端视口无横向滚动。
- 后台管理页表单测试：新增/编辑/删除走真实 API（mock fetch 层校验请求形状）。

## Out of Scope

- 首页摘要版技术栈区块（PRD 允许，暂不验收；如后续需要由首页窗口在 Home.vue 追加，复用 `src/api/tech.ts`）。
- 3D 技术栈场景（本页锚点已定为图表发光动效）。
- 熟练度自动评估或测评功能。

## Further Notes

- 分类值为自由字符串，后台管理页提供常用分类下拉 + 自定义输入，避免硬编码枚举限制站主表达。
- 与 PRD §4.1 相关：图表库按路由分包，ECharts 不进首包。
