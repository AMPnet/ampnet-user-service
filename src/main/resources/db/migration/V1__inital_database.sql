-- Role
CREATE TABLE role (
  id INT PRIMARY KEY,
  name VARCHAR(32) NOT NULL,
  description VARCHAR NOT NULL
);
INSERT INTO role VALUES
  (1, 'ADMIN', 'Administrators can create new projects to be funded and manage other platform components.');
INSERT INTO role VALUES
  (2, 'USER', 'Regular users invest in offered projects, track their portfolio and manage funds on their wallet.');
INSERT INTO role VALUES
  (3, 'TOKEN_ISSUER', 'User can burn and mint tokens on platform.');
INSERT INTO role VALUES
  (4, 'PLATFORM_MANAGER', 'User can approve users, organizations and projects.');

-- User
CREATE TABLE user_info (
    id SERIAL PRIMARY KEY,
    user_session_uuid VARCHAR NOT NULL,
    identyum_user_uuid VARCHAR NOT NULL,
    first_name VARCHAR NOT NULL,
    last_name VARCHAR NOT NULL,
    verified_email VARCHAR NOT NULL,
    phone_number VARCHAR NOT NULL,
    date_of_birth VARCHAR NOT NULL,
    personal_number VARCHAR NOT NULL,
    document_type VARCHAR NOT NULL,
    document_number VARCHAR NOT NULL,
    document_date_of_expiry VARCHAR NOT NULL,
    document_issuing_country VARCHAR NOT NULL,
    document_issued_by VARCHAR NOT NULL,
    nationality VARCHAR NOT NULL,
    address VARCHAR NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    connected BOOLEAN NOT NULL,
    deactivated BOOLEAN NOT NULL
);
CREATE TABLE app_user (
    uuid UUID PRIMARY KEY,
    first_name VARCHAR NOT NULL,
    last_name VARCHAR NOT NULL,
    email VARCHAR NOT NULL,
    password VARCHAR(60),
    role_id INT REFERENCES role(id) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    auth_method VARCHAR(8) NOT NULL,
    enabled BOOLEAN NOT NULL,
    user_info_id INT REFERENCES user_info(id)
);

-- Token
CREATE TABLE mail_token (
    id SERIAL PRIMARY KEY,
    user_uuid UUID REFERENCES app_user(uuid) NOT NULL,
    token UUID NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL
);
CREATE TABLE refresh_token(
    id SERIAL PRIMARY KEY,
    token VARCHAR(128) NOT NULL UNIQUE,
    user_uuid UUID REFERENCES app_user(uuid) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE forgot_password_token(
    id SERIAL PRIMARY KEY,
    user_uuid UUID REFERENCES app_user(uuid) NOT NULL,
    token UUID NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE bank_account(
    id SERIAL PRIMARY KEY,
    user_uuid UUID REFERENCES app_user(uuid) NOT NULL,
    iban VARCHAR(64) NOT NULL,
    bank_code VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    alias VARCHAR(128)
);
