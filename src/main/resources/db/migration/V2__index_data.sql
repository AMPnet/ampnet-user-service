CREATE INDEX idx_app_user_email ON app_user(email);
CREATE INDEX idx_app_user_role ON app_user(role_id);
CREATE INDEX idx_user_info_user_session_uuid ON user_info(user_session_uuid);

CREATE INDEX idx_bank_account_user_uuid ON bank_account(user_uuid);
CREATE INDEX idx_bank_account_user_uuid_and_id ON bank_account(user_uuid, id);
CREATE INDEX idx_forgot_password_token_token ON forgot_password_token(token);

CREATE INDEX idx_mail_token_token ON mail_token(token);
CREATE INDEX idx_mail_token_user_uuid ON mail_token(user_uuid);
CREATE INDEX idx_refresh_token_token ON refresh_token(token);
CREATE INDEX idx_refresh_token_user_uuid ON refresh_token(user_uuid);
