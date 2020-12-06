ALTER TABLE user_info DROP COLUMN client_session_uuid;
ALTER TABLE user_info DROP COLUMN identyum_user_uuid;
ALTER TABLE user_info DROP COLUMN verified_email;
ALTER TABLE user_info DROP COLUMN phone_number;
ALTER TABLE user_info DROP COLUMN personal_number;
ALTER TABLE user_info DROP COLUMN document_issued_by;
ALTER TABLE user_info DROP COLUMN address;
ALTER TABLE user_info DROP COLUMN nationality;
ALTER TABLE user_info DROP COLUMN document_date_of_expiry;

ALTER TABLE user_info RENAME COLUMN document_issuing_country TO document_country;
ALTER TABLE user_info ADD COLUMN document_valid_from VARCHAR;
ALTER TABLE user_info ADD COLUMN document_valid_until VARCHAR;
ALTER TABLE user_info ADD COLUMN id_number VARCHAR;
ALTER TABLE user_info ADD COLUMN session_id VARCHAR NOT NULL;
ALTER TABLE user_info ADD COLUMN place_of_birth VARCHAR;
ALTER TABLE user_info ADD COLUMN nationality VARCHAR;
