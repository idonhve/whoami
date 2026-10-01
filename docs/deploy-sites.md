# Sites 前端 + Render 后端 + TiDB 数据库

本部署保留 Vue 3 和 Java 17 / Spring Boot。前端、后端、数据库均在云端运行，电脑关机后仍可访问。

## 云端资源

| 服务 | 配置 |
| --- | --- |
| 前端 | [whoami Sites](https://idonhve-whoami-live.ambagowda929633355.chatgpt.site)；身份见 `.openai/hosting.json`；SPA 路由回退；站主私有访问范围 |
| 后端 | [idonhve-whoami-api](https://idonhve-whoami-api.onrender.com)，Render Free，Singapore |
| 后端控制台 | [Render 服务](https://dashboard.render.com/web/srv-dav66trncjis739dqsgg)；工作区 My Workspace |
| 后端源码 | `https://github.com/idonhve/whoami`，分支 `codex/sites-render-deployment`，根目录 Dockerfile |
| 数据库 | TiDB Cloud Starter，Singapore，集群 `idonhve-whoami`，库 `whoami`，月度支出上限 0 |

TiDB 已应用 Flyway V1–V3，现有站点内容已迁移。简历 PDF、证书原图及缩略图存入 `upload_blob`，读取内容与本地文件的 SHA-256 一致。上传文件不依赖 Render 的临时磁盘。

## 凭据与运行配置

仓库根目录的 `.env.render` 存放云数据库配置、JWT 密钥及后端配置，`.env.admin` 存放云端管理员登录凭据。二者均被 Git 忽略，不能提交到源码或前端产物。云端管理员密码已在创建后端服务之前更换为随机值，不能恢复仓库种子的默认密码。

Render 通过名为 `production.properties` 的 secret file 注入 `.env.render` 内容。运行时环境变量：

```dotenv
SPRING_CONFIG_IMPORT=optional:file:/etc/secrets/production.properties
SERVER_PORT=10000
JAVA_OPTS=-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k
```

数据库连接保留 `sslMode=VERIFY_IDENTITY`。连接池上限 6、空闲连接 2、Tomcat 线程上限 50，适配免费实例。`CORS_ALLOWED_ORIGINS` 仅允许登记的 Sites 来源。修改 secret file 时需同步 Render 配置并重新部署，修改本地文件不会自动更新云端密钥。

## 后续发布

后端配置为跟随 `codex/sites-render-deployment` 分支自动部署。推送后应检查 Render 构建日志与 `/admin/api/health`。

前端构建须传入 `VITE_API_BASE=https://idonhve-whoami-api.onrender.com`，运行 `node scripts/build-sites.mjs`，输出到根目录 `dist`。通过 Sites 技能的工作流推送准确源码、打包并发布，复用 `.openai/hosting.json` 中的 `project_id`，无需重新创建站点。数据库密码和 JWT 密钥仅存在于后端，不能传入前端构建。

Windows 发布使用 Codex 自带的 Node.js 和 Git；当前电脑的 Node.js 24.9.0 复制目录时出现原生崩溃，Codex 自带的 24.19.0 已验证可完成构建与复制。使用独立 Node.js 时可通过 `npm_execpath` 指定已安装的 `npm-cli.js`。Git 2.28 不支持工作流所需的 `--config-env`，Codex 自带的 Git 2.53 已通过源码准备检查。Git Bash 打包 Windows 盘符路径时设置 `TAR_OPTIONS=--force-local`。

首次后端发布已成功，健康接口、公开内容接口、管理员登录、简历下载、三个上传文件的内容完整性及 Sites 来源的 CORS 预检均通过。原 Sites 身份在当前账号中不可访问；按站主后续重新部署的要求，已在当前账号发布新站点。`.openai/hosting.json` 保存新的准确身份，后续更新复用该站点，不重新注册。新站点的源码历史已与本仓库合并，可直接从本仓库进行后续 Sites 发布。

Sites 新站点保持私有访问；如需让所有访客免登录访问，需由站主明确变更分享范围。

## 免费服务行为

Render Free 在闲置 15 分钟后休眠，下一次请求可能等待约一分钟唤醒。TiDB 月度支出上限为 0，达到免费额度后可能暂停服务，需要在控制台查看用量。

参考：[Render 免费实例](https://render.com/docs/free)、[Render Docker 部署](https://render.com/docs/docker)、[TiDB Starter](https://docs.pingcap.com/tidbcloud/create-tidb-cluster-serverless/?plan=starter)。
