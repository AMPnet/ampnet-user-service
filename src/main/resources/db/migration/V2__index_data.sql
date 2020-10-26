CREATE INDEX idx_coop_host ON coop(host);
CREATE INDEX idx_app_user_uuid_coop ON app_user(uuid, coop);
CREATE INDEX idx_app_user_email_coop ON app_user(email, coop);

CREATE INDEX idx_user_info_client_session_uuidd ON user_info(client_session_uuid);
CREATE INDEX idx_bank_account_user_uuid ON bank_account(user_uuid);
