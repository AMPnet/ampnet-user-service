CREATE TABLE refresh_token(
    id SERIAL PRIMARY KEY,
    token VARCHAR(128) NOT NULL,
    user_uuid UUID REFERENCES app_user(uuid) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_refresh_token_token ON refresh_token(token);
CREATE INDEX idx_app_user_email ON app_user(email);
