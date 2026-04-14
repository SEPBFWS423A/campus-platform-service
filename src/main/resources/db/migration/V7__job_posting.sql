CREATE TABLE job_posting (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    title             VARCHAR(255)   NOT NULL,
    department        VARCHAR(255)   NOT NULL,
    type              VARCHAR(50)    NOT NULL,
    status            VARCHAR(50)    NOT NULL DEFAULT 'ENTWURF',
    description       TEXT,
    requirements      TEXT,
    deadline          DATE,
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    application_count INT            NOT NULL DEFAULT 0,
    auto_publish      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_by        VARCHAR(255)
);
