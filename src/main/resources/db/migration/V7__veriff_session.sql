CREATE TABLE veriff_session(
    id VARCHAR PRIMARY KEY,
    user_uuid UUID NOT NULL,
    url VARCHAR,
    vendor_data VARCHAR,
    host VARCHAR,
    status VARCHAR,
    connected BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    state INT NOT NULL
);

CREATE TABLE veriff_decision(
    id VARCHAR PRIMARY KEY,
    status VARCHAR NOT NULL,
    code INT NOT NULL,
    reason VARCHAR,
    reason_code INT,
    decision_time VARCHAR,
    acceptance_time VARCHAR,
    created_at TIMESTAMP NOT NULL
);
