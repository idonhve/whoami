# Sites 前端 + Render 后端 + TiDB 数据库

> 当前已选择 Sites 前端 + Render 免费 Java 后端 + TiDB Starter 免费数据库。保留现有 Java 技术栈，不实施后端迁移。云端部署完成后不依赖电脑开机。

数据库已经创建：TiDB Cloud Starter，Singapore，月度支出上限为 0。现有站点内容、简历及证书文件已迁入云数据库，Flyway 已完成 V1–V3，TLS 和文件完整性校验通过。首次发布由代理使用已授权的官方 CLI 和 Sites 完成。

## 1. 创建云数据库（需要站主账号）

打开 [TiDB Cloud](https://tidbcloud.com/)，注册或登录，创建 **Starter 免费实例**。保留零支出配置，不启用付费额度。区域尽量选靠近 Render Singapore 的可用区域。

在 SQL Editor 执行：

```sql
CREATE DATABASE IF NOT EXISTS whoami;
```

在实例连接面板选择 Java / JDBC，获取连接串、用户名和密码。数据库必须允许 Render 连接；按 TiDB 连接面板配置网络访问，并保留 TLS 验证。

填写仓库根目录 `.env.render` 中的 `DB_URL`、`DB_USER`、`DB_PASSWORD`。JDBC 串中的库名必须为 `whoami`。例如（主机及用户名必须使用面板返回的真实值）：

```dotenv
DB_URL=jdbc:mysql://<数据库主机>:4000/whoami?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3
DB_USER=<连接用户名>
DB_PASSWORD=<连接密码>
```

`.env.render` 被现有 `.gitignore` 排除，不提交到 Git，也不把密码发送到聊天。其余配置已准备，其中 JWT 密钥已随机生成。

官方说明：[创建 TiDB Starter 实例](https://docs.pingcap.com/tidbcloud/create-tidb-cluster-serverless/?plan=starter)。

## 2. 首次创建 Render Docker 后端

Render 插件的创建工具不支持首次创建 Docker 服务，但已安装并校验的官方 Render CLI v2.28.0 支持创建，可以由代理直接配置，无需站主手工创建服务。代理已安装并校验 TiDB CLI，可在账号 OAuth 登录后直接创建免费数据库。控制台备用入口为 [创建 Web Service](https://dashboard.render.com/web/new)，配置如下：

| 项目 | 值 |
| --- | --- |
| 工作区 | My Workspace |
| Repository | `https://github.com/idonhve/whoami` |
| Branch | `codex/sites-render-deployment` |
| Name | `idonhve-whoami-api` |
| Region | Singapore |
| Language / Runtime | Docker |
| Root Directory | 留空（仓库根目录） |
| Dockerfile Path | `./Dockerfile` |
| Instance Type | Free |
| Health Check Path | `/admin/api/health` |

云配置保存在被 Git 忽略的 `.env.render`。通过 Render 的 secret file 注入 Spring Boot，密钥不作为命令行参数，也不进入镜像。使用 `SPRING_CONFIG_IMPORT=optional:file:/etc/secrets/production.properties` 导入该文件。`SERVER_PORT=10000` 和 `JAVA_OPTS` 作为非敏感运行时环境变量设置。无需创建 Render 前端静态服务，前端由 Sites 托管；`render.yaml` 已仅保留 Java 后端。

服务会自动构建与启动，Flyway 自动校验数据库。云端管理员密码已在首次公开后端之前更换为随机值，登录凭据保存在被 Git 忽略的 `.env.admin`，不要提交或上传该文件。后续部署继续使用云数据库中的密码，不能恢复仓库种子的默认密码。

官方说明：[Render Docker 部署](https://render.com/docs/docker)。

## 3. 自动发布 Sites 前端

站主只需完成账号登录授权。后续数据库创建、数据库账号配置、后端服务创建、部署检查、健康接口及跨域验证由代理执行；无需再次授权工作区。

确认后端可用后，使用真实后端 HTTPS 地址设置前端构建变量 `VITE_API_BASE`，运行 `node scripts/build-sites.mjs` 重新构建并把 `frontend/dist` 输出复制到根目录 `dist`，使用 Sites 工作流推送、打包和发布，并确认成功后提供访问链接。站点沿用 `.openai/hosting.json` 中的身份，避免重复创建。Sites 静态配置启用 SPA 路由回退。

当前登记的前端来源是 `https://idonhve-whoami.wiry-rose-5919.chatgpt.site`，仅用于 `CORS_ALLOWED_ORIGINS`；登记不代表该地址已经能访问。Sites 默认保持私有访问。若需要面向所有访客公开，应在发布前明确变更分享范围。
