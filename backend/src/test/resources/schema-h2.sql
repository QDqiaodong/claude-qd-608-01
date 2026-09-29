-- H2（MySQL 兼容模式）测试建表：只建放码模块所需表，与主 schema.sql 的 grading_scheme 结构一致。
CREATE TABLE IF NOT EXISTS grading_scheme (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    scheme_no            VARCHAR(40)  NOT NULL,
    name                 VARCHAR(100) NOT NULL,
    style_id             BIGINT,
    temporary            TINYINT(1)   NOT NULL DEFAULT 0,
    schema_version       INT          NOT NULL DEFAULT 2,
    content_hash         VARCHAR(64),
    spec_json            CLOB         NOT NULL,
    result_json          CLOB,
    result_generated_at  TIMESTAMP,
    remark               VARCHAR(300),
    created_at           TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_grading_scheme_no (scheme_no)
);
