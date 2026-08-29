# Spec 08 — F8 照片墙 / 奖状栏模块（瀑布流 + 灯箱）

> 对应 PRD §F8、§5 页面结构 `/awards`。术语遵循 `CONTEXT.md`（证书）。
> 发布为 GitHub Issue #9（`idonhve/whoami`，标签 `ready-for-agent`）。依赖 Spec 00（地基、uploads 卷、鉴权）。

## Problem Statement

证书与奖状是能力背书，但直接平铺大图既慢（原图体积大）又无浏览体验；且上传素材与调整排序需要后台维护、改内容不发版。

## Solution

`/awards` 页以瀑布流/错落网格展示证书缩略图（懒加载 + 逐个翻转/渐入入场）；点击打开灯箱查看原图（左右切换、ESC/遮罩关闭）；hover 显示证书名称与获取时间；后台上传图片自动生成缩略图与压缩原图，首屏该模块图片总体积 ≤ 1MB；数据来自 `certificate` 表，后台增删改与排序即时生效。

## User Stories

1. 作为面试官，我希望扫一眼照片墙就能看到站主的证书/奖状背书，以便快速验证能力。
2. 作为面试官，我希望点击任意图片打开大图灯箱，以便看清证书细节。
3. 作为访客，我希望灯箱支持左右切换与 ESC/遮罩关闭，以便高效浏览不卡死。
4. 作为访客，我希望图片懒加载且首屏不超 1MB，以便移动流量友好。
5. 作为访客，我希望入场有逐个翻转/渐入动画，以便页面有质感。
6. 作为访客，我希望 hover 看到证书名称与获取时间，以便不点开也有上下文。
7. 作为站主，我希望后台上传图片（jpg/png/webp、单张 ≤ 5MB）并填名称与获取时间，以便维护奖状墙。
8. 作为站主，我希望后台可拖拽/数值排序，以便按说服力排布。
9. 作为站主，我希望后台自动生成缩略图与压缩图，以便不用手动处理素材。

## Implementation Decisions

### 模块边界与依赖

- 前端：`src/views/awards/`（`/awards` 页）、`src/components/awards/`（瀑布流网格、卡片翻转入场、Lightbox 灯箱）、`src/api/certificate.ts`；后台管理页 `src/views/admin/awards/`（上传 + 名称/时间编辑 + 排序 + 删除）。
- 公共文件改动点：`src/router/index.ts`（`/awards` 路由）。
- 后端：`module/certificate/`（CRUD + 图片处理）。
- 图片处理（决策）：服务端用 Thumbnailator 生成两种产物——缩略图（约 400px 宽 webp，前台网格用）与压缩原图（限制长边 ≤ 2000px jpeg/webp，灯箱用）；存储于 `uploads` 卷 `certificate/` 目录。
- 依赖：Spec 00（表骨架、卷、鉴权）。表结构如需调整用本册迁移区间 **V150–V159**。
- **本页 3D/重动效锚点**：网格逐个翻转入场 + hover 视差（`/awards` 页唯一锚点，Lightbox 切换为常规过渡不计锚点）。
- 图片懒加载：进入视口前 500px 触发加载（与 PRD §4.1 通用策略一致）；灯箱原图按需加载。

### API 契约

通用约定见 Spec 00。

| 方法 | 路径 | 鉴权 | 请求 | 响应 data |
| --- | --- | --- | --- | --- |
| GET | `/api/certificates` | 公开 | 无 | `CertificateDTO[]`（按 `sortOrder` 升序，再按 `obtainedAt` 倒序） |
| POST | `/admin/api/certificates` | JWT | multipart `file` + `name` + `obtainedAt` | `{id}` |
| PUT | `/admin/api/certificates/{id}` | JWT | `{name?, obtainedAt?, sortOrder?}` | 空 |
| DELETE | `/admin/api/certificates/{id}` | JWT | 无 | 空（物理文件一并删除） |

**CertificateDTO：** `id`、`name`、`obtainedAt`、`thumbUrl`、`imageUrl`、`sortOrder`。

**校验（400）：** `name` 必填 ≤ 100；`obtainedAt` 必填（日期）；`file` 类型 ∈ {jpg/jpeg/png/webp}（魔数校验，不信扩展名）、大小 ≤ 5MB。

静态文件访问：缩略图与压缩原图经后端静态资源映射（`/uploads/**` 只读暴露该卷目录）或 Nginx 直发（部署形态由 Spec 00 compose 决定，接口路径不变）。

### 表结构

**`certificate`**（M1 V1 建骨架，本册调整用 V150–V159）：

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK, 自增 | |
| name | varchar(100) | 非空 | 证书/奖状名称 |
| obtained_at | date | 非空 | 获取时间 |
| original_file | varchar(300) | 非空 | 压缩原图相对路径 |
| thumbnail_file | varchar(300) | 非空 | 缩略图相对路径 |
| sort_order | int | 非空, 默认 0 | |
| created_at / updated_at | datetime | 非空 | |

索引：`idx_sort (sort_order)`、`idx_obtained (obtained_at)`。

## 验收标准（逐条搬运自 PRD §F8）

* [ ] 瀑布流或错落网格布局，图片懒加载；入场有逐个翻转/渐入动画

* [ ] 点击任意图片打开灯箱（Lightbox），支持左右切换、ESC/点击遮罩关闭

* [ ] hover 显示证书名称与获取时间；后台可排序

* [ ] 图片经压缩处理（后台生成缩略图，前台先加载缩略图再按需加载原图），首屏该模块图片总体积 ≤ 1MB

* [ ] 数据来自 `certificate` 表，后台增删改后前台即时生效

## Testing Decisions

- 测试缝：HTTP 集成测试（真实图片字节）+ 组件/Playwright。
- 后端集成测试：上传合法 jpg → 产出缩略图与压缩图（断言文件存在且缩略图体积显著小于原图）；魔数伪装（改扩展名的非图片）→ 400；超 5MB → 400；排序生效；删除后文件与记录均消失。
- 体积红线测试：模拟上传 N 张（> 10）后断言 `/awards` 首屏缩略图总体积 ≤ 1MB（累加各缩略图字节数）。
- 前端组件测试：懒加载初始只请求视口内图片（拦截网络请求断言数量）；灯箱 ESC/遮罩/左右键行为；hover 信息浮层。
- Playwright：上传两三张后前台网格渲染、点击开灯箱、左右切换。

## Out of Scope

- 灯箱的图片编辑/标注。
- 证书分类/标签体系。
- 外链图床（图片一律本仓库卷存储）。

## Further Notes

- 素材由窗口 B 收集至 `docs/content/`，尺寸差异大属正常——服务端压缩统一兜底。
- 压缩产物格式 webp 优先（体积最优），不支持的原图格式回退 jpeg 输出。
