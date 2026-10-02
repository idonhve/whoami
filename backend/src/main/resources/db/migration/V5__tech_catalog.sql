-- 可选技术目录（后台选取后加入公开技术栈）；自定义图标内容存于 upload_blob。
CREATE TABLE IF NOT EXISTS tech_catalog (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    icon        VARCHAR(50)  NULL COMMENT 'Devicon CSS 图标后缀',
    icon_path   VARCHAR(255) NULL COMMENT '自定义图标在 upload_blob 中的相对路径',
    category    VARCHAR(20)  NOT NULL,
    is_custom   TINYINT(1)   NOT NULL DEFAULT 0,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tech_catalog_name (name),
    KEY idx_tech_catalog_category_sort (category, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '可选技术目录';

ALTER TABLE tech_stack
    ADD COLUMN catalog_id BIGINT NULL COMMENT '来源技术目录；删除展示项后目录项保留',
    ADD COLUMN icon_path VARCHAR(255) NULL COMMENT '自定义图标相对路径';

-- TiDB cannot index a column in the same ALTER statement that introduces it.
ALTER TABLE tech_stack
    ADD KEY idx_tech_stack_catalog_id (catalog_id);

INSERT INTO tech_catalog (name, icon, category, is_custom, sort_order) VALUES
    ('Java', 'java-original', '后端', 0, 10),
    ('JVM', 'java-original', '后端', 0, 20),
    ('JUC', 'java-original', '后端', 0, 30),
    ('Spring Framework', 'spring-original', '后端', 0, 40),
    ('Spring Boot', 'spring-original', '后端', 0, 50),
    ('Spring MVC', 'spring-original', '后端', 0, 60),
    ('Spring Security', 'spring-original', '后端', 0, 70),
    ('Spring Cloud', 'spring-original', '后端', 0, 80),
    ('MyBatis', 'java-original', '后端', 0, 90),
    ('Hibernate', 'hibernate-original', '后端', 0, 100),
    ('Maven', 'maven-original', '后端', 0, 110),
    ('Gradle', 'gradle-original', '后端', 0, 120),
    ('JUnit 5', 'junit-original', '测试', 0, 130),
    ('Kotlin', 'kotlin-original', '后端', 0, 140),
    ('Groovy', 'groovy-original', '后端', 0, 150),
    ('Quarkus', 'quarkus-original', '后端', 0, 160),
    ('Thymeleaf', 'thymeleaf-original', '后端', 0, 170),
    ('HTML5', 'html5-original', '前端', 0, 210),
    ('CSS3', 'css3-original', '前端', 0, 220),
    ('JavaScript', 'javascript-original', '前端', 0, 230),
    ('TypeScript', 'typescript-original', '前端', 0, 240),
    ('Vue.js', 'vuejs-original', '前端', 0, 250),
    ('React', 'react-original', '前端', 0, 260),
    ('Node.js', 'nodejs-original', '前端', 0, 270),
    ('Vite', 'vitejs-original', '前端', 0, 280),
    ('Axios', 'axios-plain', '前端', 0, 290),
    ('Tailwind CSS', 'tailwindcss-original', '前端', 0, 300),
    ('Bootstrap', 'bootstrap-original', '前端', 0, 310),
    ('jQuery', 'jquery-original', '前端', 0, 320),
    ('MySQL', 'mysql-original', '数据库', 0, 410),
    ('PostgreSQL', 'postgresql-original', '数据库', 0, 420),
    ('Redis', 'redis-original', '数据库', 0, 430),
    ('MongoDB', 'mongodb-original', '数据库', 0, 440),
    ('Elasticsearch', 'elasticsearch-original', '数据库', 0, 450),
    ('Oracle Database', 'oracle-original', '数据库', 0, 460),
    ('SQLite', 'sqlite-original', '数据库', 0, 470),
    ('MariaDB', 'mariadb-original', '数据库', 0, 480),
    ('RabbitMQ', 'rabbitmq-original', '中间件', 0, 510),
    ('Apache Kafka', 'apachekafka-original', '中间件', 0, 520),
    ('Apache Tomcat', 'tomcat-original', '中间件', 0, 530),
    ('Nginx', 'nginx-original', '工具', 0, 610),
    ('Git', 'git-original', '工具', 0, 620),
    ('GitHub', 'github-original', '工具', 0, 630),
    ('GitLab', 'gitlab-original', '工具', 0, 640),
    ('Docker', 'docker-original', '工具', 0, 650),
    ('Kubernetes', 'kubernetes-plain', '工具', 0, 660),
    ('Linux', 'linux-original', '工具', 0, 670),
    ('Jenkins', 'jenkins-original', '工具', 0, 680),
    ('IntelliJ IDEA', 'intellij-original', '工具', 0, 690),
    ('Visual Studio Code', 'vscode-original', '工具', 0, 700),
    ('Postman', 'postman-original', '工具', 0, 710),
    ('Swagger / OpenAPI', 'swagger-original', '工具', 0, 720),
    ('Prometheus', 'prometheus-original', '工具', 0, 730),
    ('SonarQube', 'sonarqube-original', '工具', 0, 740);

-- 把已有技术项纳入目录，保留用户此前录入的数据；目录里没有的旧图标仍按原值展示。
INSERT INTO tech_catalog (name, icon, category, is_custom, sort_order)
SELECT ts.name, MIN(ts.icon), MIN(ts.category), 1, 1000 + MIN(ts.sort_order)
FROM tech_stack ts
LEFT JOIN tech_catalog tc ON tc.name = ts.name
WHERE tc.id IS NULL
GROUP BY ts.name;

UPDATE tech_stack ts
JOIN tech_catalog tc ON tc.name = ts.name
SET ts.catalog_id = tc.id,
    ts.icon_path = tc.icon_path,
    ts.icon = COALESCE(ts.icon, tc.icon);
