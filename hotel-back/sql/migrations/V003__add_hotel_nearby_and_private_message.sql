USE hotel_booking;

SET @nearby_column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_hotel' AND COLUMN_NAME = 'nearby_info'
);
SET @add_nearby_sql = IF(
    @nearby_column_exists = 0,
    'ALTER TABLE tb_hotel ADD COLUMN nearby_info JSON NULL AFTER facilities',
    'SELECT 1'
);
PREPARE add_nearby_statement FROM @add_nearby_sql;
EXECUTE add_nearby_statement;
DEALLOCATE PREPARE add_nearby_statement;

CREATE TABLE IF NOT EXISTS tb_private_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, hotel_id BIGINT NOT NULL, user_id BIGINT NOT NULL, merchant_id BIGINT NOT NULL,
  sender_user_id BIGINT NOT NULL, sender_role VARCHAR(20) NOT NULL, content VARCHAR(1000) NOT NULL, is_read TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_message_user_hotel(user_id,hotel_id,create_time), KEY idx_message_merchant_thread(merchant_id,hotel_id,user_id,create_time),
  KEY idx_message_unread(merchant_id,is_read,sender_role),
  CONSTRAINT fk_message_hotel FOREIGN KEY(hotel_id) REFERENCES tb_hotel(id),
  CONSTRAINT fk_message_user FOREIGN KEY(user_id) REFERENCES tb_user(id),
  CONSTRAINT fk_message_merchant FOREIGN KEY(merchant_id) REFERENCES tb_merchant(id),
  CONSTRAINT fk_message_sender FOREIGN KEY(sender_user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB;

UPDATE tb_hotel SET nearby_info = JSON_ARRAY(
  JSON_OBJECT('type','景点','name','广州塔','distance','步行 12 分钟','description','夜景与珠江观光地标'),
  JSON_OBJECT('type','交通','name','广州塔地铁站','distance','步行 8 分钟','description','地铁 3 号线与 APM 线'),
  JSON_OBJECT('type','餐饮','name','珠江琶醍','distance','驾车 9 分钟','description','江景餐厅与夜间休闲街区')
) WHERE id = 1 AND nearby_info IS NULL;

UPDATE tb_hotel SET nearby_info = JSON_ARRAY(
  JSON_OBJECT('type','景点','name','西湖风景区','distance','步行 6 分钟','description','可沿湖散步并游览断桥'),
  JSON_OBJECT('type','交通','name','龙翔桥地铁站','distance','步行 10 分钟','description','地铁 1 号线'),
  JSON_OBJECT('type','餐饮','name','湖滨步行街','distance','步行 12 分钟','description','杭帮菜与本地小吃集中区域')
) WHERE id = 2 AND nearby_info IS NULL;
