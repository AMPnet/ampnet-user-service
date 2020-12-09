ALTER TABLE app_user DROP COLUMN user_info_id;
DROP TABLE user_info;
CREATE TABLE user_info(
    uuid UUID PRIMARY KEY,
    session_id VARCHAR NOT NULL,
    first_name VARCHAR NOT NULL,
    last_name VARCHAR NOT NULL,
    id_number VARCHAR,
    date_of_birth VARCHAR,
    nationality VARCHAR,
    place_of_birth VARCHAR,
    document_type VARCHAR NOT NULL,
    document_number VARCHAR,
    document_country VARCHAR NOT NULL,
    document_valid_from VARCHAR,
    document_valid_until VARCHAR,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    connected BOOLEAN NOT NULL,
    deactivated BOOLEAN NOT NULL
);
ALTER TABLE app_user ADD COLUMN user_info_uuid UUID REFERENCES user_info(uuid);
