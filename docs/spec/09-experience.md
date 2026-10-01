# Spec 09 — F9 工作经历模块（公司 / 项目介绍）

> 对应 PRD §F9、§5 页面结构 `/experience`。术语遵循 `CONTEXT.md`（经历、战果、雷达图维度）。
> 发布为 GitHub Issue #10（`idonhve/whoami`，标签 `ready-for-agent`）。依赖 Spec 00（地基、表骨架、鉴权）。

## Problem Statement

工作经历需要清楚呈现任职信息、公司背景与参与项目，让访客能够快速了解每段工作的上下文。

## Solution

`/experience` 页以滚动时间轴呈现经历卡片。每张卡片展示公司、职位、任职时间、公司介绍、项目介绍与技术标签；点击或悬停可展开补充要点。公司介绍、项目介绍和补充要点支持长文本。数据来自 `experience` 表，并由管理后台维护。

## User Stories

1. 作为面试官，我希望随滚动点亮的时间轴快速纵览职业轨迹，以便几秒建立履历全貌。
2. 作为面试官，我希望当前屏的经历卡片高亮，以便注意力聚焦。
3. 作为面试官，我希望了解公司的业务与团队背景，以便理解工作经历。
4. 作为面试官，我希望了解参与项目及承担的工作，以便理解经历中的实际内容。
5. 作为面试官，我希望卡片展示补充要点，以便快速浏览工作细节。
6. 作为技术同行，我希望技术标签以图标云呈现，以便识别技术口味。
7. 作为移动端访客，我希望时间轴变单列卡片 + 滚动渐入，以便手机上同样流畅。
8. 作为站主，我希望后台增删改经历卡（公司/职位/时间/公司介绍/项目介绍/技术标签/补充要点），以便内容运营不发版。

## Implementation Decisions

### 模块边界与依赖

- 前端：`src/views/experience/`（`/experience` 页）、`src/components/experience/`（Timeline 主线、战果数字翻牌、RadarChart（ECharts radar）、TechTagCloud、可展开卡片）、`src/api/experience.ts`；后台管理页 `src/views/admin/experience/`（经历卡 CRUD：公司/职位/时间/战果数组/雷达维度数组/技术标签/展开要点/排序）。
- 公共文件改动点：`src/router/index.ts`（`/experience` 路由）。
- 后端：`module/experience/`（CRUD + JSON 字段校验）。
- 依赖：Spec 00（表骨架、鉴权）；图表用 ECharts（radar），滚动动效用 GSAP ScrollTrigger（与 Spec 01 共用依赖，无新增库）。表结构如需调整用本册迁移区间 **V160–V169**。
- **本页 3D/重动效锚点**：滚动点亮时间轴主场景（`/experience` 页唯一锚点；翻牌与雷达图为常规滚动动效）。
- 数字翻牌：进入视口触发递增动画（GSAP），数值含文本后缀（`%`、`w+`）时对数字部分动画、后缀常显。
- 展开态内容为要点列表（不写段落），单条要点 ≤ 50 字。
- 移动端：单列卡片 + 滚动渐入，时间轴主线简化为左侧细线。

### API 契约

通用约定见 Spec 00。

| 方法 | 路径 | 鉴权 | 请求 | 响应 data |
| --- | --- | --- | --- | --- |
| GET | `/api/experiences` | 公开 | 无 | `ExperienceDTO[]`（按 `sortOrder` 升序，再按 `startDate` 倒序） |
| GET | `/admin/api/experiences` | JWT | 无 | 全量同构 |
| POST | `/admin/api/experiences` | JWT | `ExperienceCreate` | `{id}` |
| PUT | `/admin/api/experiences/{id}` | JWT | `ExperienceCreate` | 空 |
| DELETE | `/admin/api/experiences/{id}` | JWT | 无 | 空 |

**ExperienceDTO：** `id`、`company`、`title`、`startDate`、`endDate`（null = 至今）、`companyIntro`、`projectIntro`、`techTags: string[]`、`highlights: string[]`、`sortOrder`。

**校验（400）：** `company`/`title` 必填各 ≤ 50；`startDate` 必填且 ≤ `endDate`；`techTags` ≤ 12 个、单个 ≤ 30；`highlights` ≤ 10 条。公司介绍、项目介绍和单条要点均不限制字符数。

### 表结构

**`experience`**（M1 V1 建骨架，本册调整用 V160–V169；复合结构用 JSON 列）：

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK, 自增 | |
| company | varchar(50) | 非空 | 公司 |
| title | varchar(50) | 非空 | 职位 |
| start_date | date | 非空 | 入职 |
| end_date | date | 可空 | null = 至今 |
| company_intro | mediumtext | 可空 | 公司介绍正文 |
| project_intro | mediumtext | 可空 | 项目介绍正文 |
| achievements | json | 非空 | 旧版遗留字段，保留历史数据 |
| radar | json | 非空 | 旧版遗留字段，保留历史数据 |
| tech_tags | json | 非空 | 技术标签字符串数组 |
| highlights | json | 可空 | 展开要点列表 |
| sort_order | int | 非空, 默认 0 | |
| created_at / updated_at | datetime | 非空 | |

索引：`idx_sort (sort_order)`、`idx_start_date (start_date)`。

## 验收标准（逐条搬运自 PRD §F9）

* [ ] 滚动驱动：左侧发光主线路随滚动逐段点亮，当前经历卡片高亮

* [ ] 每段经历卡片展示公司介绍、项目介绍与技术标签；点击/悬停可展开补充要点

* [ ] 单卡片默认态文字 ≤ 30 字（公司+职位+时间）；点击/hover 展开才显示补充细节（展开内容也以要点列表为主，不写段落）

* [ ] 移动端时间轴切换为单列卡片 + 滚动渐入，动画保留但简化

* [ ] 数据来自 `experience` 表，后台可维护；雷达图维度可自定义（至少支持 3-8 个维度）

## Testing Decisions

- 测试缝：HTTP 集成测试（JSON 字段校验是重点）+ 组件测试 + Playwright。
- 后端集成测试：radar 2 维 / 9 维 / 分数越界 → 400；合法 3~8 维通过；日期倒置 400；公开列表排序正确；CRUD 后公开接口即时反映。
- 前端组件测试：默认态渲染文本字符数 ≤ 30（对样例数据断言）；进入视口触发数字递增（断言动画前后文本状态变化，不测 GSAP 内部）；点击/悬停展开要点；雷达图按 radar 数据渲染维度数。
- Playwright：桌面滚动驱动主线点亮与卡片高亮（滚动事件后断言激活态 class）；移动端视口断言单列布局。
- 移动端降级：断言动画简化路径（无 ScrollTrigger 重计算泄漏）。

## Out of Scope

- 经历的富文本编辑器（要点列表纯文本足够）。
- PDF 简历的同步生成（经历数据仅供前台渲染）。
- 多语言经历版本。

## Further Notes

- 战果与雷达维度素材来自 `docs/content/`（窗口 B 产出）；未到位前用占位经历卡开发。
- "单卡片默认态文字 ≤ 30 字"由组件测试对真实数据守门（超长直接测试失败，防口径漂移）。
