USE hotel_booking;

SET @email_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tb_user'
      AND COLUMN_NAME = 'email'
);
SET @add_email_sql = IF(
    @email_column_exists = 0,
    'ALTER TABLE tb_user ADD COLUMN email VARCHAR(128) NULL AFTER phone',
    'SELECT 1'
);
PREPARE add_email_statement FROM @add_email_sql;
EXECUTE add_email_statement;
DEALLOCATE PREPARE add_email_statement;

SET @email_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tb_user'
      AND INDEX_NAME = 'uk_user_email'
);
SET @add_email_index_sql = IF(
    @email_index_exists = 0,
    'CREATE UNIQUE INDEX uk_user_email ON tb_user (email)',
    'SELECT 1'
);
PREPARE add_email_index_statement FROM @add_email_index_sql;
EXECUTE add_email_index_statement;
DEALLOCATE PREPARE add_email_index_statement;
