# Spec 05 — F5 访客统计（埋点 + 访客地图 + 留言板 + 统计看板）

> 对应 PRD §F5、§5 页面结构 `/about`。术语遵循 `CONTEXT.md`（访客、访问日志、行为事件、IP 归属地、留言）。
> **受 ADR-0002 约束：IP 归属地用 ip2region 离线库解析，地图打点按省聚合。**
> 发布为 GitHub Issue #6（`idonhve/whoami`，标签 `ready-for-agent`）。依赖 Spec 00（地基、表骨架、鉴权）、Spec 01（about 页骨架）。

## Problem Statement

站主不知道谁在看网站、从哪来、看了什么、停留多久，无法评估求职转化；访客没有轻量的留言通道，HR 想联系也无处留话；而常见方案依赖在线 IP API（限流、境外精度差、服务随时变更）或强制登录登记（劝退访客）。

## Solution

访客免登录零门槛浏览；前端埋点 SDK 异步上报（sendBeacon，不阻塞渲染）页面 PV、会话进入/离开与来源；后端用离线库 ip2region 解析 IP 归属地，按省级聚合成访客地图（前台"关于本站"与后台看板同源渲染）；提供自愿留名的留言板（默认自动通过、可后台回复删除）；后台看板呈现按日 PV/UV 曲线、TOP 页面、来源分布。IP 存储做脱敏说明，统计数据保留 ≥ 12 个月。

## User Stories

1. 作为访客，我希望不弹窗、不登记即可看完全部内容，以便零门槛了解站主。
2. 作为访客，我希望浏览行为不打断页面渲染，以便低端网络也流畅。
3. 作为访客，我希望在"关于本站"看到中国地图上的访问打点，以便感知这个站被谁在看（社交证明）。
4. 作为访客，我希望 hover 省份点看到"该省访问 N 次"与城市 TOP，以便了解访客分布细节。
5. 作为访客，我希望自愿留名留言（昵称必填、邮箱选填），以便 HR 或潜在客户留下联系意向。
6. 作为访客，我希望留言提交成功有终端风 toast（`> message sent ✓`），以便确认提交生效。
7. 作为站主，我希望看到按日 PV/UV 曲线、TOP 页面与来源分布，以便评估简历投递后的流量变化。
8. 作为站主，我希望在后台看板看到访客地图（省级聚合），以便知道面试官的地域分布。
9. 作为站主，我希望回复与删除留言，以便维护留言区质量。
10. 作为站主，我希望留言有频率限制与内容过滤，以便防灌水与 XSS。
11. 作为站主，我希望统计保留 ≥ 12 个月且 IP 展示脱敏，以便合规可解释。
12. 作为站主，我希望 IP 解析不依赖外部 API，以便无限流与可用性风险（ADR-0002）。

## Implementation Decisions

### 模块边界与依赖

- 前端：`src/tracker/`（埋点 SDK：会话 id 生成、路由变化自动 `page_view`、进入/离开上报、sendBeacon 封装，全局注册一次）、`src/views/about/`（`/about` 页骨架：访客地图区 + 留言板区 + **预留给 Spec 07 的简历下载按钮 slot**）、`src/components/about/`（ChinaMap、MessageBoard）、`src/api/stats.ts`、`src/api/message.ts`；后台看板页 `src/views/admin/stats/`（PV/UV 曲线、地图、TOP 页面、来源分布）与留言管理页 `src/views/admin/messages/`（列表/回复/删除）。
- 公共文件改动点：`src/router/index.ts`（`/about` 路由）、`src/main.ts`（tracker SDK 全局安装一行）。
- 后端：`module/track/`（上报接口 + ip2region 解析）、`module/guestmessage/`（留言）、`module/stats/`（聚合查询）。
- 资产入库（不依赖 CDN）：ip2region `xdb`（约 11MB，后端 `resources`）；中国地图 GeoJSON 采用阿里 DataV.GeoAtlas **标准版图（含台湾、南海诸岛九段线），版本锁定**存放于前端 `src/assets/geo/`。
- 依赖：Spec 00（表骨架、鉴权）。表结构如需调整用本册迁移区间 **V120–V129**。
- **埋点事件类型（PRD §7.2 代决项为基线，站主可推翻）：** `page_view` / `resume_download`（Spec 07 服务端记）/ `cmd_palette_use`（Spec 10）/ `easter_egg`（Spec 11）/ `github_outbound`（Spec 03/04）/ `message_submit`。
- 会话定义：`crypto.randomUUID()` 生成，存 `sessionStorage`；`visit_log` 一行 = 一次会话；UV = 当日去重 sessionId。
- **本页 3D/重动效锚点**：访客地图的呼吸打点动效（`/about` 页唯一锚点）。
- 留言安全：内容纯文本存储，输出转义（防 XSS）；昵称 ≤ 20、内容 ≤ 500 字符；同 IP 每分钟 ≤ 3 条（超限 429）；默认 `status=approved` 自动通过。
- IP 脱敏：库内存原始 IP（解析归属地必需），后台/接口展示层输出脱敏格式（如 `1.2.*.*`）；`visit_log` 不提供原始 IP 的任何对外接口，合规说明写入部署文档。
- 保留策略：不做 12 个月内清理；12 个月以上数据保留归档（不删除，超出 PRD 最低要求）。

### API 契约

通用约定见 Spec 00。

| 方法 | 路径 | 鉴权 | 请求 | 响应 data | 说明 |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/track/session` | 公开 | `{sessionId, referrer?}` | 空 | 会话开始，写 `visit_log`（后端补 IP/归属地/UA） |
| POST | `/api/track/session/{sessionId}/end` | 公开 | `{lastPagePath?}` | 空 | 会话结束（幂等：已结束则忽略），服务端算停留时长 |
| POST | `/api/track/event` | 公开 | `{sessionId, eventType, pagePath?, detail?}` | 空 | 统一事件入口（`page_view` 走这里） |
| GET | `/api/visit-stats/geo` | 公开 | 无 | `[{province, count, cities:[{city,count}](≤5)}]` | 省级聚合 + 城市 TOP（关于本站地图） |
| POST | `/api/messages` | 公开 | `{nickname, content, email?}` | 空 | 限流同 IP 每分钟 ≤ 3 条（429） |
| GET | `/api/messages` | 公开 | query `limit`（默认 20） | `MessageDTO[]` | 仅 `approved` |
| GET | `/admin/api/stats/daily` | JWT | query `days`（默认 30） | `[{date, pv, uv}]` | |
| GET | `/admin/api/stats/top-pages` | JWT | query `days` | `[{pagePath, pv}]` | |
| GET | `/admin/api/stats/referrers` | JWT | query `days` | `[{referrer, count}]` | |
| GET | `/admin/api/stats/geo` | JWT | 无 | 同公开 geo 全量 | 看板地图 |
| GET | `/admin/api/messages` | JWT | query `status` | 全量含状态 | |
| PUT | `/admin/api/messages/{id}/reply` | JWT | `{reply}` | 空 | 回复（`replied_at`） |
| PUT | `/admin/api/messages/{id}/status` | JWT | `{status}` | 空 | `approved`/`hidden`（下架） |
| DELETE | `/admin/api/messages/{id}` | JWT | 无 | 空 | 删除 |

**MessageDTO（公开）：** `id`、`nickname`、`content`、`reply`（可空）、`repliedAt`、`createdAt`（不含 email）。

**校验：** `nickname` 必填 ≤ 20；`content` 必填 ≤ 500；`email` 可空需邮箱格式；`eventType` 必须在枚举内（400）。`page_view` 事件频率不做硬限流（正常浏览量级内服务端可承受；异常洪峰按 IP 维度软限流兜底）。

### 表结构

**`visit_log`**（一次会话一行；M1 V1 建骨架，本册调整用 V120–V129）：

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK, 自增 | |
| session_id | varchar(36) | 唯一, 非空 | 前端生成 UUID |
| ip | varchar(45) | 非空 | 支持 IPv6 长度；展示层脱敏 |
| province / city | varchar(50) | 可空 | ip2region 解析（境外可为空/"境外"） |
| user_agent | varchar(500) | 非空 | 原始 UA |
| referrer | varchar(500) | 可空 | 来源页 |
| entry_page | varchar(200) | 非空 | 进入页路径 |
| entry_time | datetime | 非空 | |
| leave_time | datetime | 可空 | end 上报写入 |
| duration_seconds | int | 可空 | 服务端计算 |
| visit_date | date | 非空 | 聚合用（entry_time 的日期） |

索引：`uk_session_id`、`idx_visit_date (visit_date)`、`idx_province (province)`。

**`track_event`：**

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK, 自增 | |
| event_type | varchar(32) | 非空 | 六类事件枚举 |
| session_id | varchar(36) | 非空 | 关联 visit_log |
| page_path | varchar(200) | 可空 | |
| detail | json | 可空 | 事件明细（如命令面板命令名） |
| ip | varchar(45) | 非空 | |
| created_at | datetime | 非空 | |

索引：`idx_event_type_date (event_type, created_at)`、`idx_session_id`、`idx_page_path (page_path)`。

**`guest_message`：**

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK, 自增 | |
| nickname | varchar(20) | 非空 | |
| email | varchar(100) | 可空 | 仅站主可见，不下发前台 |
| content | varchar(500) | 非空 | 纯文本，输出转义 |
| status | varchar(16) | 非空, 默认 approved | approved / hidden |
| reply | varchar(500) | 可空 | 管理员回复 |
| replied_at | datetime | 可空 | |
| ip | varchar(45) | 非空 | 限流与追溯用 |
| created_at | datetime | 非空 | |

索引：`idx_status_created (status, created_at)`、`idx_ip_created (ip, created_at)`（限流计数）。

## 验收标准（逐条搬运自 PRD §F5）

* [ ] 前台零登录门槛：不弹窗、不强制登记即可看完全部内容

* [ ] 前端埋点上报：页面 PV、进入/离开时间（计算停留时长）、来源 referrer；后台可见聚合报表（按日 PV/UV 曲线、TOP 页面、来源分布）

* [ ] 访客地图：后台与前台"关于本站"区域渲染中国地图打点（IP 归属地），按**省级聚合**，点上有呼吸动画，hover 显示"该省访问 N 次"及城市 TOP 列表

* [ ] IP 归属地解析使用离线库 ip2region（境内精确到城市级，零外部 API 依赖；决策记录：ADR-0002）

* [ ] 中国地图 GeoJSON 采用阿里 DataV.GeoAtlas 标准版图（含台湾、南海诸岛九段线），版本锁定并存放于本仓库，不依赖 CDN 实时拉取

* [ ] 留言板：访客可自愿填写昵称（必填）+ 留言内容（必填）+ 邮箱（选填）；提交成功有终端风 toast 反馈（`> message sent ✓`）

* [ ] 留言支持管理员后台回复与删除；前台仅展示已通过审核（默认自动通过）的留言

* [ ] 统计数据保留 ≥ 12 个月；IP 存储做脱敏说明（个人网站合规提示）

* [ ] 埋点上报异步且不阻塞页面渲染（sendBeacon 或 img 打点）

## Testing Decisions

- 测试缝：HTTP 上报/聚合接口（MockMvc + 真实库）+ 前端 SDK 行为 + Playwright。SDK 不测 sendBeacon 实现细节，测调用契约。
- 后端集成测试：session start/end 写 `visit_log` 且 duration 计算（时钟注入）；end 幂等；`page_view` 计入 PV；daily 聚合按日去重 sessionId 算 UV；top-pages/referrers 聚合正确；ip2region 对样例 IP 解析出正确省市（内置几组已知断言）；geo 聚合省级 + 城市 TOP。
- 留言测试：必填/长度/邮箱格式 400；同 IP 第 4 条/分钟 429；默认 approved；reply/status/delete 全流程；公开接口不含 email 字段（契约断言）。
- 前端组件测试：路由切换触发一次 `page_view`；页面隐藏触发 end（mock sendBeacon 断言 URL 与 payload）；留言提交成功 toast 文案 `> message sent ✓`。
- Playwright：浏览三个页面 → 后台看板 PV/UV 出现对应数据；地图渲染（断言省份图形与打点元素数量）；留言提交后前台列表出现。

## Out of Scope

- 留言邮件通知（PRD §7.2：一期不做，后台看板可见即可）。
- 境外 IP 高精度定位（ADR-0002：暂用离线库覆盖范围）。
- 访客身份识别/账号体系（永远不做，零登录门槛是红线）。
- 世界地图（只做中国地图）。
- 埋点数据的实时推送（看板为查询式聚合）。

## Further Notes

- 关于本站页的地图数据来自公开 `GET /api/visit-stats/geo`（不含任何 IP 信息），前台与后台共用同一渲染组件。
- tracker SDK 是全站横切件：Spec 03/04/10/11 直接调用其 `trackEvent(eventType, detail)` 方法，不要各自实现上报。
- `visit_log` 的原始 IP 永不出现在任何公开响应中；后台看板如需展示 IP 一律脱敏。
