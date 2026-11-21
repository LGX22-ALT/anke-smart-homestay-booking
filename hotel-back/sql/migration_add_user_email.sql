-- Adds the email field to an existing hotel_booking database.
-- The checks make this script safe to run again after an interrupted migration.
USE hotel_booking;

SET @email_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'tb_user' AND column_name = 'email'
);
SET @sql = IF(
  @email_column_exists = 0,
  'ALTER TABLE tb_user ADD COLUMN email VARCHAR(128) NULL AFTER phone',
  'SELECT 1'
);
PREPARE add_email_column FROM @sql;
EXECUTE add_email_column;
DEALLOCATE PREPARE add_email_column;

UPDATE tb_user
SET email = CONCAT('user', id, '@hotel-out.local')
WHERE email IS NULL OR email = '';

ALTER TABLE tb_user MODIFY COLUMN email VARCHAR(128) NOT NULL;

SET @email_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'tb_user' AND index_name = 'uk_user_email'
);
SET @sql = IF(
  @email_index_exists = 0,
  'ALTER TABLE tb_user ADD UNIQUE KEY uk_user_email(email)',
  'SELECT 1'
);
PREPARE add_email_index FROM @sql;
EXECUTE add_email_index;
DEALLOCATE PREPARE add_email_index;
