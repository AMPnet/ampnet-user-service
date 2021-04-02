CREATE EXTENSION IF NOT EXISTS pgcrypto;
UPDATE user_info SET
session_id = encode(encrypt(session_id::bytea, '${private_key}', 'aes')::bytea, 'base64'),
id_number = encode(encrypt(id_number::bytea, '${private_key}', 'aes')::bytea, 'base64'),
date_of_birth = encode(encrypt(date_of_birth::bytea, '${private_key}', 'aes')::bytea, 'base64'),
nationality = encode(encrypt(nationality::bytea, '${private_key}', 'aes')::bytea, 'base64'),
place_of_birth = encode(encrypt(place_of_birth::bytea, '${private_key}', 'aes')::bytea, 'base64'),
identyum_user_uuid = encode(encrypt(identyum_user_uuid::bytea, '${private_key}', 'aes')::bytea, 'base64'),
document_type = encode(encrypt(document_type::bytea, '${private_key}', 'aes')::bytea, 'base64'),
document_number = encode(encrypt(document_number::bytea, '${private_key}', 'aes')::bytea, 'base64'),
document_country = encode(encrypt(document_country::bytea, '${private_key}', 'aes')::bytea, 'base64'),
document_valid_until = encode(encrypt(document_valid_until::bytea, '${private_key}', 'aes')::bytea, 'base64'),
document_valid_from = encode(encrypt(document_valid_from::bytea, '${private_key}', 'aes')::bytea, 'base64');
