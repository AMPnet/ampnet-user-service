ALTER TABLE user_info ADD COLUMN identyum_user_uuid VARCHAR;
ALTER TABLE coop ADD COLUMN kyc_provider_id INT NOT NULL DEFAULT 0;
