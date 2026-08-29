-- V2__init_seed.sql — M1 地基：初始数据
-- 初始管理员账号：admin / Admin@whoami2026
-- 该密码与仓库一起公开，仅供本地开发与首次部署，上线前必须改密！
-- bcrypt 密文生成与首次改密方式见 docs/deploy.md

INSERT INTO admin_user (username, password_hash, failed_attempts, created_at, updated_at)
VALUES ('admin', '$2b$10$UraGeyfzU/Y.fkWS0uFVuenwM7FTvPceIJMdw9guTpqfDC2vJWHGe', 0, NOW(), NOW());

-- 站点配置初始键值（Spec 06 维护，公开白名单键）
INSERT INTO site_config (config_key, config_value, description, updated_at) VALUES
    ('domain', 'localhost', '开机日志域名文案（Spec 01）', NOW()),
    ('owner_name', '站主', '站主名称占位（Spec 01 Hero / Spec 07 简历文件名）', NOW()),
    ('github_url', '', 'GitHub 主页链接（Spec 03 处理空值）', NOW()),
    ('degrade_force_full', 'false', '强制满血预览开关（Spec 01）', NOW());
