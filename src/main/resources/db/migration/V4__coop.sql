CREATE TABLE coop_config(
    id SERIAL PRIMARY KEY
);

CREATE TABLE coop(
    id SERIAL PRIMARY KEY,
    identifier VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    url VARCHAR(1024) NOT NULL,
    coop_config_id INT REFERENCES coop_config,
    CONSTRAINT uc_identifier UNIQUE(identifier)
);
CREATE INDEX idx_coop_identifier ON coop(identifier);

ALTER TABLE app_user ADD COLUMN coop VARCHAR(64) NOT NULL DEFAULT 'ampnet';
ALTER TABLE app_user ADD CONSTRAINT uc_email_in_coop UNIQUE(email, coop);
