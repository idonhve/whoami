# 下一步开发计划与提示词

> 基于 `PRD.md`、`docs/parallel-plan.md`、`docs/spec/01~12`、`design-system/whoami/MASTER.md` 与当前代码现状制定。
> 已完成：M1 地基 + F1（欢迎页+首页）+ F6（管理后台框架）+ F2（技术栈展示）+ F4（作品展示）+ F9 后端（工作经历 CRUD）
> 当前里程碑：**M2 核心内容**（PRD §6 第 3-4 周）
> 当前进度：12 个模块已完成 4.5 个（约 37%）
> ⚠️ 注意：F2 / F4 / F9 后端三个分支代码已完成但**均未合并到 main**，下一步先合并再开新窗口。

---

## 总览：六个阶段，按序推进

| 阶段 | 模块 | 里程碑 | 状态 |
|------|------|--------|------|
| 0 | 合并已完成的 F2 / F4 / F9 后端到 main | - | ⚠️ **先做** |
| 1 | F4 GitHub 作品展示 | M2 | ✅ 已完成（feat/f4-works，未合并） |
| 2 | F2 技术栈展示 | M2 | ✅ 已完成（feat/f2-tech-stack，未合并） |
| 3 | **F9 工作经历战果墙（前端部分）** | M2 | ◀ **下一步**（后端已在 feat/f9-experience） |
| 4 | F7 简历下载 + F3 GitHub 图标 | M2 | 待做 |
| 5 | F8 照片墙 | M2 | 待做 |
| 6 | F5 统计 + F10 命令面板 + F11 彩蛋 | M3 | 待做 |

> M2 全部完成后进入 M4 本地验收（性能红线 + Lighthouse），M5 上线延后。

***

## 阶段 1：F4 — GitHub 作品展示模块（最优先）

### 为什么最先做

* 首页"精选作品"区是首屏重要组成部分，完成后首页完整度大幅提升

* 求职网站的核心展示内容——面试官看代码实力的第一入口

* 涉及 GitHub 同步任务，越早跑通越早发现 PAT/网络问题

### 涉及文件

* **后端**：`module/project/`（实体+CRUD）、`module/github/`（GitHubClient+SyncService+\@Scheduled）

* **前端**：`src/views/works/`、`src/components/works/`（RepoCard）、`src/api/works.ts`

* **后台**：`src/views/admin/works/`（列表+筛选+置顶/隐藏/编辑+同步按钮+日志）

* **公共文件**：`src/router/index.ts`（替换 /works 占位）、`src/views/Home.vue`（填充精选作品 slot）、`frontend/src/views/admin/layout/registry.ts`（追加 works 模块）

### 提示词（复制即用）

```
认领 Issue #5（F4 GitHub 作品展示模块），按 docs/spec/04-works.md 实现完整功能。

开工前准备：
1. git pull 同步 main，创建分支 feat/f4-works
2. 读 docs/spec/04-works.md（权威口径）、docs/spec/00-m1-foundation.md（通用约定）、design-system/whoami/MASTER.md（视觉铁律）
3. 读现有代码：backend/src/main/java/com/whoami/module/siteconfig/（后端模块范例）、frontend/src/api/http.ts（HTTP 客户端）、frontend/src/views/admin/layout/registry.ts（后台模块注册约定）、frontend/src/views/admin/config/（后台管理页范例）

后端实现（module/project/ + module/github/）：
- Project 实体 + Mapper + ProjectService + ProjectController（公开 GET /api/projects + 管理 GET/PUT /admin/api/projects）
- 同步：GitHubClient（调 GitHub REST API /users/{owner}/repos），SyncService（upsert 语义：元数据覆盖、运营字段保留、消失仓库自动隐藏），@Scheduled(cron = "0 0 3 * * ?") 每日 03:00
- POST /admin/api/projects/sync（手动同步）+ GET /admin/api/projects/sync/logs（同步日志）
- 置顶上限 3 个 → 409；PAT 缺失 → status=failed + 写日志
- 如需调整表结构用迁移区间 V110–V119

前端实现：
- /works 页面：作品卡片列表（语言色点、star/fork、最近更新、置顶标记），hover 3D 倾斜，滚动渐入
- 首页 Home.vue 填充"精选作品"占位 slot（scope=featured，仅置顶项）
- src/api/works.ts：封装公开与管理两组 API
- 后台管理页：列表+筛选（语言/置顶/隐藏）+ 编辑中文描述 + 置顶/隐藏切换 + 立即同步按钮 + 同步日志视图
- 后台模块注册：在 src/views/admin/works/ 导出 AdminModule，追加到 registry.ts 聚合表

验收自测（逐条对照 spec 验收标准）：
- 卡片展示中文名/语言色点/star-fork/更新时间/置顶标记
- 点击新标签打开 GitHub
- 后台置顶（≤3）/隐藏/编辑描述生效
- 同步按钮可用，失败有日志
- GitHub 不可用时前台展示缓存数据不报错
- 卡片滚动渐入 + hover 3D 倾斜

约束：
- 只引用 design-system token，不裸写 hex
- 动效带 reduced-motion 降级
- PR 描述关联 Issue：Closes #5
```

***

## 阶段 2：F2 — 技术栈展示模块

### 为什么第二个做

* 面试官判断技术版图的核心模块

* ECharts 图表可视化，与 F4 卡片动效风格不同，避免连续做同类页面疲劳

* 数据结构简单（单表 CRUD），可快速出成果

### 涉及文件

* **后端**：`module/techstack/`（CRUD + 公开/管理两组接口）

* **前端**：`src/views/tech/`、`src/components/tech/`（饼图、条状图、技术项卡片）、`src/api/tech.ts`

* **后台**：`src/views/admin/tech/`

* **公共文件**：`src/router/index.ts`（替换 /tech 占位）、`registry.ts`（追加 tech 模块）

### 提示词

```
认领 Issue #3（F2 技术栈展示模块），按 docs/spec/02-tech-stack.md 实现完整功能。

开工前准备：
1. git pull 同步 main，创建分支 feat/f2-tech-stack
2. 读 docs/spec/02-tech-stack.md、design-system/whoami/MASTER.md
3. 读现有代码：frontend/src/api/http.ts、frontend/src/views/admin/config/（后台管理页范例）、frontend/src/views/admin/layout/registry.ts

后端实现（module/techstack/）：
- TechStack 实体 + Mapper + TechStackService + TechStackController
- 公开 GET /api/tech-stack（按 sortOrder 升序）
- 管理 GET/POST/PUT/DELETE /admin/api/tech-stack
- 校验：name 必填 ≤50；proficiency 枚举 master/proficient/familiar；weight 1~100；category 必填 ≤20
- 如需调整表结构用迁移区间 V100–V109

前端实现：
- /tech 页面：ECharts 饼图（按分类 weight 占比）+ 水平条状图（熟练度，渐变发光填充动画）
- 每项技术：名称 + devicon 图标 + 熟练度标签（精通/熟练/了解）
- 图表滚动触发渐入动画，hover tooltip 明细
- 移动端自适应
- ECharts 按路由分包，不进首包
- src/api/tech.ts：封装公开与管理两组 API
- 后台管理页：增删改技术项（名称/图标/分类/熟练度/权重/排序），分类下拉+自定义输入
- 后台模块注册到 registry.ts

验收自测：
- 饼图按分类占比 + 条状图熟练度渐变发光
- 每项有图标 + 熟练度标签
- 滚动渐入 + hover tooltip
- 后台改完前台刷新即见
- 移动端自适应无横向滚动

约束：
- ECharts 不进首包（路由分包）
- 只引用 design-system token
- 动效带 reduced-motion 降级
- PR 描述：Closes #3
```

***

## 阶段 3：F9 — 工作经历（战果视觉墙）

### 为什么第三个做

* 差异化亮点功能，视觉冲击力最强

* 重动效模块（时间轴+翻牌+雷达图），需要充足精力

* JSON 复合字段校验是后端重点

### 涉及文件

* **后端**：`module/experience/`（CRUD + JSON 字段校验）

* **前端**：`src/views/experience/`、`src/components/experience/`（Timeline、战果翻牌、RadarChart、TechTagCloud、可展开卡片）、`src/api/experience.ts`

* **后台**：`src/views/admin/experience/`

* **公共文件**：`src/router/index.ts`（替换 /experience 占位）、`registry.ts`

### 提示词

```
认领 Issue #10（F9 工作经历战果视觉墙），按 docs/spec/09-experience.md 实现完整功能。

开工前准备：
1. git pull 同步 main，创建分支 feat/f9-experience
2. 读 docs/spec/09-experience.md、design-system/whoami/MASTER.md
3. 读现有代码：frontend/src/api/http.ts、frontend/src/views/admin/layout/registry.ts、frontend/src/composables/routeTransition.ts（GSAP 用法范例）

后端实现（module/experience/）：
- Experience 实体 + Mapper + ExperienceService + ExperienceController
- 公开 GET /api/experiences（按 sortOrder 升序再按 startDate 倒序）
- 管理 GET/POST/PUT/DELETE /admin/api/experiences
- JSON 字段校验（重点）：radar 维度 3~8 且 score 0~100 整数、维度名 ≤20 不重复；achievements ≤6 条；techTags ≤12 个；highlights ≤10 条
- startDate 必填且 ≤ endDate
- 如需调整表结构用迁移区间 V160–V169

前端实现：
- /experience 页面：左侧发光主线路随滚动逐段点亮（GSAP ScrollTrigger），当前卡片高亮
- 战果数字翻牌动画（进入视口触发递增，数值含后缀如 %、w+ 时数字部分动画）
- ECharts radar 雷达图（3~8 维度，后台可自定义）
- 技术标签图标云
- 单卡片默认态文字 ≤30 字（公司+职位+时间），点击/hover 展开要点列表
- 移动端：单列卡片 + 滚动渐入，时间轴简化为左侧细线
- src/api/experience.ts
- 后台管理页：经历卡 CRUD（公司/职位/时间/战果数组/雷达维度数组/技术标签/展开要点/排序）
- 后台模块注册到 registry.ts

验收自测：
- 滚动驱动主线点亮 + 卡片高亮
- 战果翻牌 + 雷达图 + 技术标签云
- 默认态 ≤30 字，展开要点列表
- 移动端单列 + 简化动画
- 后台 CRUD 后前台即时反映
- 雷达图维度 3~8 可自定义

约束：
- 本页唯一 3D/重动效锚点 = 滚动点亮时间轴
- GSAP 只动 transform/opacity/filter
- 动效带 reduced-motion 降级
- ECharts 按路由分包
- PR 描述：Closes #10
```

***

## 阶段 4：F7 简历下载 + F3 GitHub 图标

### 为什么放一起

* F7 是核心转化按钮（求职终点），实现量中等但至关重要

* F3 是小模块（仅一个组件），适合与 F7 同窗口快速完成

* 两者都依赖 site\_config，配置已就绪

### F7 涉及文件

* **后端**：`module/resume/`（上传/版本/回滚/下载 + 服务端埋点）

* **前端**：`src/components/resume/DownloadButton.vue`、`src/api/resume.ts`

* **后台**：`src/views/admin/resume/`

* **公共文件**：`src/views/Home.vue`（填充 Hero 简历下载占位）、`registry.ts`

### F3 涉及文件

* **前端**：`src/components/shared/GithubIcon.vue`（页头页脚复用）

* **公共文件**：`src/components/layout/AppHeader.vue`、`AppFooter.vue`（追加图标）

### 提示词

```
认领 Issue #8（F7 简历下载）+ Issue #4（F3 GitHub 图标），按 docs/spec/07-resume.md 和 docs/spec/03-github-icon.md 实现。

开工前准备：
1. git pull 同步 main，创建分支 feat/f7-resume-f3-github-icon
2. 读 docs/spec/07-resume.md、docs/spec/03-github-icon.md、design-system/whoami/MASTER.md
3. 读现有代码：frontend/src/api/http.ts、frontend/src/components/layout/AppHeader.vue、AppFooter.vue、frontend/src/views/Home.vue（Hero 简历下载占位）、frontend/src/views/admin/layout/registry.ts

F7 后端实现（module/resume/）：
- ResumeFile 实体 + Mapper + ResumeService + ResumeController
- 公开 GET /api/resume/latest（{exists, displayName, updatedAt}）
- 公开 GET /api/resume/download（文件流，Content-Disposition: attachment; filename*=UTF-8''<显示名>，服务端写 resume_download 埋点后返回，无版本 404）
- 管理 POST /admin/api/resumes（multipart 上传，校验 pdf + ≤20MB）
- 管理 GET /admin/api/resumes（版本列表）
- 管理 PUT /admin/api/resumes/{id}/restore（回滚）
- 版本策略：每次上传递增版本号，is_current 唯一，历史仅保留最近 3 个
- 显示名：{owner_name}_简历_{年月}.pdf
- 如需调整表结构用迁移区间 V140–V149

F7 前端实现：
- DownloadButton.vue（唯一实现，首页 Hero 与 about 复用）
- exists=false → 按钮隐藏不报错；exists=true → 终端风命令样式（> download resume.pdf）+ hover 光效
- 点击即下载最新版
- src/api/resume.ts
- 后台管理页：上传 + 版本列表 + 回滚
- 填充 Home.vue 的 Hero"简历下载"占位
- 后台模块注册到 registry.ts

F3 前端实现：
- GithubIcon.vue（src/components/shared/）：githubUrl 为空 → 不渲染；非空 → 渲染图标 + target="_blank" + rel="noopener noreferrer"
- hover 呼吸光晕（CSS box-shadow 动画，不用 JS 库）
- aria-label="GitHub 主页"
- 追加到 AppHeader.vue 和 AppFooter.vue
- 点击外跳上报 github_outbound 埋点（注：埋点 SDK 属于 Spec 05，若 F5 尚未实现，先预留调用位置注释 TODO，不阻塞本模块）

验收自测：
F7：
- 下载按钮首屏可达，终端风样式 + hover 光效
- 点击即下载，文件名含姓名+年月
- 下载计入埋点
- 未上传时按钮隐藏
- 连传 4 版历史只剩 3 个
F3：
- 图标在页头页脚，hover 呼吸光晕
- 点击新标签打开 GitHub
- URL 来自 site_config，改配置不改代码
- 暗色背景下清晰可见

约束：
- 文件存储用 Docker uploads 卷 resume/ 目录
- 只引用 design-system token
- PR 描述：Closes #8, Closes #4
```

***

## 阶段 5：F8 — 照片墙 / 奖状栏

### 为什么放这里

* 图片处理（缩略图+压缩）有一定后端工作量

* 灯箱组件前端实现量中等

* 不阻塞其他模块，放这里做节奏舒服

### 涉及文件

* **后端**：`module/certificate/`（CRUD + 图片处理）

* **前端**：`src/views/awards/`、`src/components/awards/`（瀑布流、卡片翻转入场、Lightbox）、`src/api/certificate.ts`

* **后台**：`src/views/admin/awards/`

* **公共文件**：`src/router/index.ts`（替换 /awards 占位）、`registry.ts`

### 提示词

```
认领 Issue #9（F8 照片墙/奖状栏），按 docs/spec/08-awards.md 实现完整功能。

开工前准备：
1. git pull 同步 main，创建分支 feat/f8-awards
2. 读 docs/spec/08-awards.md、design-system/whoami/MASTER.md
3. 读现有代码：frontend/src/api/http.ts、frontend/src/views/admin/layout/registry.ts

后端实现（module/certificate/）：
- Certificate 实体 + Mapper + CertificateService + CertificateController
- 公开 GET /api/certificates（按 sortOrder 升序再按 obtainedAt 倒序）
- 管理 POST /admin/api/certificates（multipart file + name + obtainedAt）
- 管理 PUT /admin/api/certificates/{id}、DELETE /admin/api/certificates/{id}（物理文件一并删除）
- 图片处理：服务端用 Thumbnailator 生成缩略图（~400px 宽 webp）+ 压缩原图（长边 ≤2000px jpeg/webp），存储于 uploads 卷 certificate/ 目录
- 校验：name 必填 ≤100；obtainedAt 必填；file 类型 ∈ {jpg/jpeg/png/webp}（魔数校验不信扩展名）、≤5MB
- 静态文件访问：/uploads/** 只读暴露
- 如需调整表结构用迁移区间 V150–V159

前端实现：
- /awards 页面：瀑布流/错落网格，图片懒加载（视口前 500px 触发）
- 入场逐个翻转/渐入动画
- 点击打开 Lightbox 灯箱（左右切换、ESC/遮罩关闭）
- hover 显示证书名称与获取时间
- 首屏该模块缩略图总体积 ≤1MB
- src/api/certificate.ts
- 后台管理页：上传 + 名称/时间编辑 + 排序 + 删除
- 后台模块注册到 registry.ts

验收自测：
- 瀑布流 + 懒加载 + 翻转入场动画
- 灯箱左右切换 + ESC/遮罩关闭
- hover 显示名称与时间
- 首屏缩略图总体积 ≤1MB
- 后台增删改即时生效

约束：
- 本页唯一锚点 = 网格逐个翻转入场 + hover 视差
- 压缩产物 webp 优先，不支持时回退 jpeg
- 只引用 design-system token
- 动效带 reduced-motion 降级
- PR 描述：Closes #9
```

***

## 阶段 6：F5 统计 + F10 命令面板 + F11 彩蛋（M3 阶段，可并行）

### 为什么放最后

* 属于 M3 里程碑（统计与亮点），PRD 定义 M2 核心内容完成后再做

* F5 是其他模块的埋点依赖（F3/F4/F7/F10/F11 都消费 tracker SDK），但可以先用占位 SDK 开发，F5 完成后接入

* F10/F11 是纯前端模块，可与其他窗口并行

### F5 提示词

```
认领 Issue #6（F5 访客统计），按 docs/spec/05-visitor-stats.md 实现完整功能。

开工前准备：
1. git pull 同步 main，创建分支 feat/f5-visitor-stats
2. 读 docs/spec/05-visitor-stats.md、docs/adr/0002-ip2region-offline.md、design-system/whoami/MASTER.md
3. 读现有代码：frontend/src/api/http.ts、frontend/src/views/admin/layout/registry.ts

后端实现（module/track/ + module/guestmessage/ + module/stats/）：
- TrackController：POST /api/track/session（写 visit_log，后端补 IP/归属地/UA）
- POST /api/track/session/{sessionId}/end（幂等，算停留时长）
- POST /api/track/event（统一事件入口）
- ip2region 离线库解析 IP 归属地（xdb 约 11MB 放 resources）
- GuestMessageController：POST /api/messages（限流同 IP 每分钟 ≤3 条 → 429）、GET /api/messages（仅 approved）
- StatsController：GET /admin/api/stats/daily、top-pages、referrers、GET /api/visit-stats/geo（省级聚合+城市 TOP）
- IP 脱敏：展示层输出 1.2.*.* 格式
- 如需调整表结构用迁移区间 V120–V129

前端实现：
- src/tracker/（埋点 SDK：会话 id 生成、路由变化自动 page_view、sendBeacon 封装，main.ts 全局安装一行）
- /about 页面：访客地图区（ChinaMap ECharts + DataV GeoJSON 标准版图入库）+ 留言板区
- ChinaMap：呼吸打点动效，hover 显示"该省 N 次"+城市 TOP
- 留言板：昵称必填+内容必填+邮箱选填，提交成功终端风 toast（> message sent ✓）
- 后台看板：PV/UV 曲线、地图、TOP 页面、来源分布
- 后台留言管理：列表/回复/删除
- 后台模块注册到 registry.ts（stats + messages 两个模块）

验收自测：
- 零登录门槛，埋点异步不阻塞
- 地图省级聚合 + 呼吸打点 + hover 城市列表
- 留言提交 + toast + 限流
- 后台看板 PV/UV/TOP/来源
- IP 脱敏
- PR 描述：Closes #6
```

### F10 提示词

```
认领 Issue #11（F10 命令面板），按 docs/spec/10-command-palette.md 实现完整功能。

开工前准备：
1. git pull 同步 main，创建分支 feat/f10-command-palette
2. 读 docs/spec/10-command-palette.md、design-system/whoami/MASTER.md
3. 读现有代码：frontend/src/components/layout/AppHeader.vue（追加 >_ 图标）、frontend/src/App.vue（全局挂载）

前端实现（纯前端，无后端）：
- src/components/command-palette/（面板组件、命令注册表、模糊匹配、回显动画、历史管理）
- 命令：home/works/tech/experience/awards/resume/message/github/theme
- Ctrl+K（Mac Cmd+K）或点击页头 >_ 图标唤起
- 模糊匹配 + 上下键选择 + 回车执行
- help 显示全部命令；未知命令返回 command not found
- 执行回显动画（> navigating to works ...）
- 历史 localStorage 最近 5 条（whoami:cmd-history）
- 公共文件：App.vue 挂载面板层、AppHeader.vue 追加 >_ 图标按钮
- resume 命令复用 Spec 07 下载 API；github 命令读 site_config.githubUrl
- theme 命令切换全局强调色 CSS 变量（localStorage whoami:theme）
- 埋点：打开并执行命令时上报 cmd_palette_use（依赖 Spec 05 tracker SDK）

验收自测：
- Ctrl+K 唤起 + >_ 图标唤起
- 模糊匹配 + 上下键 + 回车
- help + command not found
- 回显动画
- 历史记录 5 条

约束：
- 命令表集中本册维护，不开放各模块注册
- 面板样式自适应移动端
- PR 描述：Closes #11
```

### F11 提示词

```
认领 Issue #12（F11 控制台彩蛋），按 docs/spec/11-easter-egg.md 实现完整功能。

开工前准备：
1. git pull 同步 main，创建分支 feat/f11-easter-egg
2. 读 docs/spec/11-easter-egg.md、design-system/whoami/MASTER.md
3. 读现有代码：frontend/src/App.vue（挂载初始化）

前端实现（纯前端，无后端）：
- src/easter-egg/（console 输出、秘籍序列监听、特效触发器）
- console 输出 ASCII art 签名 + 招聘联系方式 + "你打开了控制台，说明你是对的人"文案
- 键盘秘籍 ↑↑↓↓←→←→BA 触发全站粒子雨（轻量 canvas）+ 主题切换
- 惰性绑定（零运行时开销）：console 彩蛋挂载后一次性输出；秘籍监听首次按键进入候选序列才开始比对
- 触发计入 easter_egg 埋点（仅秘籍触发，console 输出不打点）
- 公共文件：App.vue 挂载初始化一行

验收自测：
- console 输出 ASCII art + 联系方式
- 秘籍触发粒子雨 + 视觉反馈
- 零运行时开销（非候选按键 O(1) 退出）
- 不影响正常功能

约束：
- 不占用页面 3D 锚点配额
- 粒子雨用轻量 canvas，非 3D
- PR 描述：Closes #12
```

***

## 全局注意事项

1. **每个窗口开工前**：`git pull` 同步 main，独立分支开发
2. **PR 描述**关联 Issue（如 `Closes #5`）
3. **前端铁律**（MASTER.md）：只引用 token 不裸写 hex；像素字体仅 ASCII；动效带 reduced-motion 降级；图标用 SVG 不用 emoji
4. **后台模块注册**：在模块目录导出 AdminModule → 追加到 `registry.ts` 聚合表（公共文件改动需在 PR 声明）
5. **路由占位替换**：`/works`、`/tech`、`/experience`、`/awards`、`/about` 当前都是 PlaceholderView，各模块替换为自己的页面组件
6. **埋点依赖**：F3/F4/F7/F10/F11 都依赖 F5 的 tracker SDK；若 F5 未完成，先预留调用位置（TODO 注释），不阻塞开发
7. **GitHub PAT**：F4 需要 `GITHUB_TOKEN` 和 `GITHUB_OWNER` 环境变量，提前在 `.env` 配置（创建步骤见 `docs/content-checklist.md`）

