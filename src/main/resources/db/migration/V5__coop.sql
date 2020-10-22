CREATE TABLE coop(
    identifier VARCHAR(64) PRIMARY KEY,
    host VARCHAR NOT NULL,
    name VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    config TEXT,
    CONSTRAINT uc_host UNIQUE(host)
);
CREATE INDEX idx_coop_host ON coop(host);

INSERT INTO coop VALUES ('ampnet', 'staging.ampnet.io', 'AMPnet', NOW(), null);
ALTER TABLE app_user ADD COLUMN coop VARCHAR(64) NOT NULL DEFAULT 'ampnet';
ALTER TABLE app_user ADD CONSTRAINT uc_email_in_coop UNIQUE(email, coop);

CREATE INDEX idx_app_user_uuid_coop ON app_user(uuid, coop);
CREATE INDEX idx_app_user_email_coop ON app_user(email, coop);
DROP INDEX idx_app_user_role;
