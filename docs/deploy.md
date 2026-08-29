# 部署与运维手册（M1 地基）

> 适用范围：本地开发与验收的 Docker Compose 全栈部署。生产部署（境内服务器 + ICP + HTTPS）属于 PRD 里程碑 M5，另行补充。

## 1. 前置要求

- Docker Desktop（或等价 Docker + Docker Compose v2）
- 已克隆本仓库并切到 main 分支

## 2. 一键起全栈

```bash
# 1) 准备环境变量（首次）
cp .env.example .env
#    编辑 .env：设置 DB_PASSWORD、JWT_SECRET（≥ 32 字符随机串）

# 2) 起栈（首次会构建镜像，较慢）
docker compose up -d --build

# 3) 访问
#    前台：http://localhost/
#    管理后台：http://localhost/admin  （初始账号见下）
```

三个容器：

| 服务 | 说明 | 端口（默认） |
| --- | --- | --- |
| `frontend` | Nginx：前端静态资源 + 反代 `/api`、`/admin/api` | 80（`HTTP_PORT` 可覆盖） |
| `backend` | Spring Boot 3（Java 17） | 8080（`BACKEND_PORT` 可覆盖） |
| `mysql` | MySQL 8.0，数据落在 `mysql-data` 卷 | 3306（`DB_PORT` 可覆盖） |

卷：`mysql-data`（数据库数据）、`uploads`（简历/证书文件，挂到后端 `/app/uploads`，Spec 07/08 使用）。

数据库表结构由 Flyway 自动迁移（`V1__init_schema.sql` 建齐 12 张表，`V2__init_seed.sql` 写入种子数据），无需手工执行 SQL。

## 3. 初始管理员账号与首次改密

初始账号由 Flyway 种子脚本（`V2__init_seed.sql`）插入，密码以 **bcrypt 密文**存储，无明文：

- 用户名：`admin`
- 初始密码：`Admin@whoami2026`

> ⚠️ 该初始密码随仓库公开，**仅供本地开发与首次部署**。公网可访问前必须改密。

### 3.1 生成 bcrypt 密文

任选其一（cost 10，`$2a$` / `$2b$` / `$2y$` 前缀均可）：

```bash
# 方式 A：Docker（推荐，无需本地环境）
docker run --rm httpd:2-alpine htpasswd -bnBC 10 "" '你的新密码' | tr -d ':\n'

# 方式 B：Node（bcryptjs）
npx --yes bcryptjs-cli hash '你的新密码' 10
```

### 3.2 更新密码（改密）

用生成的密文替换下例中的 `<新密文>`：

```bash
docker compose exec mysql mysql -u root -p"$DB_PASSWORD" whoami -e \
  "UPDATE admin_user SET password_hash='<新密文>', failed_attempts=0, locked_until=NULL WHERE username='admin';"
```

> 后续版本（Spec 06 管理后台框架）会提供页面上改密，届时以页面为准。

## 4. 冒烟验收

```powershell
# Windows
.\scripts\smoke.ps1
```

```bash
# Linux / macOS / Git Bash
./scripts/smoke.sh
```

脚本自动完成：起栈 → `GET /admin/api/health` 返回 up → 管理员登录成功 → `GET /admin/api/auth/me` 校验 token → 未登录访问受保护接口返回 401 → 停栈。

## 5. 本地开发（不走 Docker）

前后端分离开发模式：

```bash
# 后端（需本机 JDK 17+，连本地 MySQL 3306）
cd backend
mvn spring-boot:run        # 自动读取仓库根目录 .env，无需手工传环境变量

# 前端（Vite 开发服务器已配置代理：/api 与 /admin/api → localhost:8080）
cd frontend
npm install
npm run dev
```

`application.yml` 已配置 `spring.config.import` 自动导入根目录 `.env`，IDEA 直接启动主类同样生效（工作目录需为 `backend/` 或仓库根目录）。Docker Compose 部署时注入的真实环境变量优先级更高，互不影响。

### 5.1 本地 MySQL 一次性初始化

本机 MySQL 需要先建库和账号（用 root 执行一次；口令与 `.env` 的 `DB_PASSWORD` 保持一致）：

```sql
CREATE DATABASE IF NOT EXISTS whoami CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'whoami'@'%' IDENTIFIED BY '<.env 里的 DB_PASSWORD>';
CREATE USER IF NOT EXISTS 'whoami'@'localhost' IDENTIFIED BY '<.env 里的 DB_PASSWORD>';
GRANT ALL PRIVILEGES ON whoami.* TO 'whoami'@'%';
GRANT ALL PRIVILEGES ON whoami.* TO 'whoami'@'localhost';
FLUSH PRIVILEGES;
```

建表和种子数据由 Flyway 在后端首次启动时自动完成。

常用命令：

| 位置 | 命令 | 说明 |
| --- | --- | --- |
| `backend/` | `mvn test` | 单元测试（无需 Docker） |
| `backend/` | `mvn verify` | 单元 + 集成测试（需 Docker，Testcontainers 起 MySQL） |
| `frontend/` | `npm run build` | 类型检查 + 生产构建 |
| `frontend/` | `npm test` | 路由守卫等组件级测试（Vitest） |
| `frontend/` | `npm run lint` | ESLint |
| `frontend/` | `npm run format` | Prettier 格式化 |

## 6. 环境变量清单（.env）

| 键 | 必填 | 说明 |
| --- | --- | --- |
| `DB_USER` | 否（默认 whoami） | MySQL 业务账号 |
| `DB_PASSWORD` | **是** | MySQL 口令（同时用作容器 root 口令） |
| `JWT_SECRET` | **是** | JWT 签名密钥，≥ 32 字符，必须随机且保密 |
| `GITHUB_TOKEN` | 否 | GitHub PAT（Spec 04 同步用，只读公开仓库权限，可留空） |
| `GITHUB_OWNER` | 否 | GitHub 用户名（Spec 04 用，可留空） |
| `TZ` | 否（默认 Asia/Shanghai） | 容器时区 |
| `HTTP_PORT` / `BACKEND_PORT` / `DB_PORT` | 否 | 端口覆盖（默认 80 / 8080 / 3306） |

密钥红线：以上敏感值只进 `.env`（已被 gitignore），不进 git、不下发前端。

## 7. 给并行开发窗口的约定（重要）

- **目录约定**：前端页面 `frontend/src/views/<module>/`、组件 `frontend/src/components/<module>/`、API 客户端 `frontend/src/api/<module>.ts`、store `frontend/src/stores/<module>.ts`；后端 `backend/src/main/java/com/whoami/module/<module>/{controller,service,mapper,entity}`。
- **迁移编号区间**：M1 已占用 V1~V2。各模块 ALTER/新迁移只用自己区间：Spec 02 → V100–V109、Spec 04 → V110–V119、Spec 05 → V120–V129、Spec 06 → V130–V139、Spec 07 → V140–V149、Spec 08 → V150–V159、Spec 09 → V160–V169。Spec 10/11/12 无表。
- **API 约定**：前台 `/api`，管理 `/admin/api`（JWT 保护）；统一响应包络 `{code, message, data}`，失败时 `code` = HTTP 状态码（400/401/403/404/409/429）。
- **公共文件改动**（`src/router/index.ts`、`docker-compose.yml`、`CONTEXT.md` 等）：先在对应 Issue 声明再改，遵守 `docs/parallel-plan.md` 冲突规避规则。
- **后端 Mapper**：各模块 mapper 接口自行标注 `@Mapper`，不要改动共享配置类。

## 8. 故障排查

| 现象 | 处理 |
| --- | --- |
| `docker compose up` 报变量未设置 | 未创建 `.env`：`cp .env.example .env` 并填写 |
| 后端起不来，日志含 `JWT_SECRET 未配置或长度不足` | `.env` 里 `JWT_SECRET` 少于 32 字符；若在 IDEA 启动还报此错，确认工作目录是 `backend/`（找不到 `.env`） |
| 日志含 `Access denied for user 'whoami'@'localhost'` | 本地 MySQL 未初始化 `whoami` 库/账号，或口令与 `.env` 的 `DB_PASSWORD` 不一致（见 5.1 节） |
| 登录一直 401 | 密码不对（初始密码见第 3 节），或曾被锁（连续失败 5 次锁 10 分钟） |
| 想重置数据库 | `docker compose down -v`（会删 MySQL 数据卷，慎用） |
| 80 端口被占用 | `.env` 设 `HTTP_PORT=8080` 后重启栈 |
