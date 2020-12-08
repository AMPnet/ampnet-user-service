CREATE TABLE veriff_session(
    id VARCHAR PRIMARY KEY,
    user_uuid UUID NOT NULL,
    url VARCHAR,
    vendor_data VARCHAR,
    host VARCHAR,
    status VARCHAR,
    connected BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL
);
