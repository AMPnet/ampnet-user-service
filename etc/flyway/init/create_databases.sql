DROP DATABASE IF EXISTS user_service;
CREATE DATABASE user_service ENCODING 'UTF-8';

DROP DATABASE IF EXISTS user_service_test;
CREATE DATABASE user_service_test ENCODING 'UTF-8';

DROP USER IF EXISTS user_service;
CREATE USER user_service WITH PASSWORD 'password';

DROP USER IF EXISTS user_service_test;
CREATE USER user_service_test WITH PASSWORD 'password';

\connect user_service
CREATE EXTENSION IF NOT EXISTS pgcrypto;
\connect user_service_test
CREATE EXTENSION IF NOT EXISTS pgcrypto;
