-- V1__init_schema.sql — M1 地基：建齐全部 12 张表骨架
-- 字段口径唯一来源：docs/spec/02~09 各册「表结构」章节（经 Spec 00 汇总）
-- 后续各模块用 ALTER 调整自己的表，迁移编号区间见 docs/spec/00-m1-foundation.md
-- （Spec 02: V100–V109 / Spec 04: V110–V119 / Spec 05: V120–V129 / Spec 06: V130–V139
--   Spec 07: V140–V149 / Spec 08: V150–V159 / Spec 09: V160–V169）

SET NAMES utf8mb4;

-- 1. 管理员账号（Spec 00 本册实现并维护）
CREATE TABLE admin_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL COMMENT '登录名',
    password_hash   VARCHAR(100) NOT NULL COMMENT 'bcrypt 密文',
    failed_attempts INT          NOT NULL DEFAULT 0 COMMENT '连续失败计数',
    locked_until    DATETIME     NULL COMMENT '锁定截止时间',
    last_login_at   DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员账号（Spec 00）';

-- 2. 技术栈（Spec 02）
CREATE TABLE tech_stack (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '技术名称（如 Vue 3）',
    icon        VARCHAR(50) NULL COMMENT 'devicon 图标名',
    category    VARCHAR(20) NOT NULL COMMENT '分类（前端/后端/数据库/工具/其他…）',
    proficiency VARCHAR(16) NOT NULL COMMENT 'master / proficient / familiar',
    weight      INT         NOT NULL DEFAULT 1 COMMENT '饼图权重 1~100',
    sort_order  INT         NOT NULL DEFAULT 0 COMMENT '展示排序',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_category (category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '技术栈（Spec 02）';

-- 3. 作品/GitHub 仓库（Spec 04）
CREATE TABLE project (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    repo_id          BIGINT       NOT NULL COMMENT 'GitHub 仓库数字 id（同步幂等键）',
    repo_name        VARCHAR(100) NOT NULL COMMENT '仓库名',
    full_name        VARCHAR(200) NOT NULL COMMENT 'owner/repo',
    cn_title         VARCHAR(200) NULL COMMENT '中文描述（运营字段，同步不覆盖）',
    description_en   VARCHAR(500) NULL COMMENT 'GitHub 原始描述',
    language         VARCHAR(50)  NULL COMMENT '主语言',
    stargazers_count INT          NOT NULL DEFAULT 0,
    forks_count      INT          NOT NULL DEFAULT 0,
    html_url         VARCHAR(300) NOT NULL COMMENT '仓库页 URL',
    pushed_at        DATETIME     NULL COMMENT '最近推送时间',
    is_pinned        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '置顶（业务上限 3，服务端校验）',
    is_hidden        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '隐藏（同步发现仓库消失时自动置 1）',
    sort_order       INT          NOT NULL DEFAULT 0 COMMENT '运营排序',
    last_synced_at   DATETIME     NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_repo_id (repo_id),
    KEY idx_is_pinned (is_pinned),
    KEY idx_is_hidden (is_hidden)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '作品/GitHub 仓库（Spec 04）';

-- 4. GitHub 同步日志（Spec 04）
CREATE TABLE sync_task_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    trigger_type VARCHAR(16)  NOT NULL COMMENT 'scheduled / manual',
    status       VARCHAR(16)  NOT NULL COMMENT 'success / failed',
    repo_count   INT          NOT NULL DEFAULT 0 COMMENT '本次同步仓库数',
    hidden_gone  INT          NOT NULL DEFAULT 0 COMMENT '因仓库消失被自动隐藏的数量',
    message      VARCHAR(500) NULL COMMENT '失败原因（如 PAT 无效、网络超时）',
    started_at   DATETIME     NULL,
    finished_at  DATETIME     NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'GitHub 同步日志（Spec 04）';

-- 5. 访问日志（Spec 05）一次会话一行
CREATE TABLE visit_log (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    session_id       VARCHAR(36)  NOT NULL COMMENT '前端生成 UUID',
    ip               VARCHAR(45)  NOT NULL COMMENT '支持 IPv6 长度；展示层脱敏',
    province         VARCHAR(50)  NULL COMMENT 'ip2region 解析（境外可为空）',
    city             VARCHAR(50)  NULL COMMENT 'ip2region 解析（境外可为空）',
    user_agent       VARCHAR(500) NOT NULL COMMENT '原始 UA',
    referrer         VARCHAR(500) NULL COMMENT '来源页',
    entry_page       VARCHAR(200) NOT NULL COMMENT '进入页路径',
    entry_time       DATETIME     NOT NULL,
    leave_time       DATETIME     NULL COMMENT 'end 上报写入',
    duration_seconds INT          NULL COMMENT '服务端计算',
    visit_date       DATE         NOT NULL COMMENT '聚合用（entry_time 的日期）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_id (session_id),
    KEY idx_visit_date (visit_date),
    KEY idx_province (province)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '访问日志（Spec 05）';

-- 6. 行为事件（Spec 05）
CREATE TABLE track_event (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(32)  NOT NULL COMMENT 'page_view/resume_download/cmd_palette_use/easter_egg/github_outbound/message_submit',
    session_id VARCHAR(36)  NOT NULL COMMENT '关联 visit_log',
    page_path  VARCHAR(200) NULL,
    detail     JSON         NULL COMMENT '事件明细（如命令面板命令名）',
    ip         VARCHAR(45)  NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_event_type_date (event_type, created_at),
    KEY idx_session_id (session_id),
    KEY idx_page_path (page_path)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '行为事件（Spec 05）';

-- 7. 访客留言（Spec 05）
CREATE TABLE guest_message (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    nickname   VARCHAR(20)  NOT NULL,
    email      VARCHAR(100) NULL COMMENT '仅站主可见，不下发前台',
    content    VARCHAR(500) NOT NULL COMMENT '纯文本，输出转义',
    status     VARCHAR(16)  NOT NULL DEFAULT 'approved' COMMENT 'approved / hidden',
    reply      VARCHAR(500) NULL COMMENT '管理员回复',
    replied_at DATETIME     NULL,
    ip         VARCHAR(45)  NOT NULL COMMENT '限流与追溯用',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_status_created (status, created_at),
    KEY idx_ip_created (ip, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '访客留言（Spec 05）';

-- 8. 站点配置（Spec 06 维护，V2 写入种子）
CREATE TABLE site_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    config_key   VARCHAR(64)  NOT NULL COMMENT '键（snake_case）',
    config_value TEXT         NULL COMMENT '值统一字符串存储，消费方自行转型',
    description  VARCHAR(200) NULL COMMENT '用途说明（后台管理页展示）',
    updated_by   BIGINT       NULL COMMENT '最后修改的管理员 id',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站点配置（Spec 06）';

-- 9. 后台操作日志（Spec 06 AOP 切面写入）
CREATE TABLE admin_op_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    admin_user_id BIGINT       NOT NULL COMMENT '操作者',
    action        VARCHAR(16)  NOT NULL COMMENT 'HTTP 方法（LOGIN 也记）',
    resource      VARCHAR(200) NOT NULL COMMENT '请求路径',
    resource_id   VARCHAR(64)  NULL COMMENT '目标资源 id',
    detail        JSON         NULL COMMENT '参数摘要（密码等敏感字段脱敏为 ***）',
    ip            VARCHAR(45)  NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_admin_created (admin_user_id, created_at),
    KEY idx_resource (resource)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '后台操作日志（Spec 06）';

-- 10. 简历版本（Spec 07）
CREATE TABLE resume_file (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    version_no   INT          NOT NULL COMMENT '递增版本号',
    file_path    VARCHAR(300) NOT NULL COMMENT '存储相对路径（uploads 卷内）',
    display_name VARCHAR(100) NOT NULL COMMENT '下载文件名（{姓名}_简历_{年月}.pdf）',
    size_bytes   BIGINT       NOT NULL,
    is_current   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '全表至多一个 1（服务端事务保证）',
    uploaded_at  DATETIME     NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_version (version_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '简历版本（Spec 07）';

-- 11. 证书/奖状（Spec 08）
CREATE TABLE certificate (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100) NOT NULL COMMENT '证书/奖状名称',
    obtained_at    DATE         NOT NULL COMMENT '获取时间',
    original_file  VARCHAR(300) NOT NULL COMMENT '压缩原图相对路径',
    thumbnail_file VARCHAR(300) NOT NULL COMMENT '缩略图相对路径',
    sort_order     INT          NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sort (sort_order),
    KEY idx_obtained (obtained_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '证书/奖状（Spec 08）';

-- 12. 工作经历（Spec 09）复合结构用 JSON 列
CREATE TABLE experience (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    company      VARCHAR(50) NOT NULL,
    title        VARCHAR(50) NOT NULL COMMENT '职位',
    start_date   DATE        NOT NULL COMMENT '入职',
    end_date     DATE        NULL COMMENT 'null = 至今',
    achievements JSON        NOT NULL COMMENT '[{value, context}] 战果数组',
    radar        JSON        NOT NULL COMMENT '[{dimension, score}] 3~8 维',
    tech_tags    JSON        NOT NULL COMMENT '技术标签字符串数组',
    highlights   JSON        NULL COMMENT '展开要点列表',
    sort_order   INT         NOT NULL DEFAULT 0,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sort (sort_order),
    KEY idx_start_date (start_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作经历（Spec 09）';
