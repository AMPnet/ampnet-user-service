ALTER TABLE coop ADD COLUMN logo VARCHAR NOT NULL DEFAULT 'temporary';
UPDATE coop SET logo = 'logo' WHERE identifier = 'ampnet';
ALTER TABLE coop ALTER COLUMN logo DROP DEFAULT;
