CREATE TABLE tb_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, phone VARCHAR(20) NOT NULL, email VARCHAR(128) NULL, password VARCHAR(100) NOT NULL,
  name VARCHAR(64) NOT NULL, avatar VARCHAR(500), id_card VARCHAR(64), role VARCHAR(20) NOT NULL DEFAULT 'user',
  status TINYINT NOT NULL DEFAULT 1, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_phone(phone), UNIQUE KEY uk_user_email(email), KEY idx_user_role_status(role,status)
) ;
CREATE TABLE tb_merchant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, company_name VARCHAR(128) NOT NULL,
  contact_name VARCHAR(64), license_no VARCHAR(64), status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_merchant_user(user_id), CONSTRAINT fk_merchant_user FOREIGN KEY(user_id) REFERENCES tb_user(id)
) ;
CREATE TABLE tb_hotel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, merchant_id BIGINT NOT NULL, name VARCHAR(128) NOT NULL, city VARCHAR(64) NOT NULL,
  district VARCHAR(64), address VARCHAR(255), type VARCHAR(32), cover_image VARCHAR(500), gallery JSON, description TEXT,
  facilities JSON, nearby_info JSON, status VARCHAR(20) NOT NULL DEFAULT 'PENDING_AUDIT', min_price DECIMAL(10,2) DEFAULT 0, score DECIMAL(3,2) DEFAULT 0,
  review_count INT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_hotel_city_status(city,status), KEY idx_hotel_merchant(merchant_id,status), KEY idx_hotel_type(type),
  CONSTRAINT fk_hotel_merchant FOREIGN KEY(merchant_id) REFERENCES tb_merchant(id)
) ;
CREATE TABLE tb_room_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, hotel_id BIGINT NOT NULL, name VARCHAR(128) NOT NULL, image VARCHAR(500), area INT,
  bed VARCHAR(64), max_guests INT NOT NULL DEFAULT 2, base_price DECIMAL(10,2) NOT NULL, stock INT NOT NULL DEFAULT 0,
  facilities JSON, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_room_hotel(hotel_id), CONSTRAINT fk_room_hotel FOREIGN KEY(hotel_id) REFERENCES tb_hotel(id)
) ;
CREATE TABLE tb_room_calendar (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, room_type_id BIGINT NOT NULL, calendar_date DATE NOT NULL, price DECIMAL(10,2) NOT NULL,
  available_stock INT NOT NULL DEFAULT 0, is_available TINYINT NOT NULL DEFAULT 1, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_room_date(room_type_id,calendar_date), KEY idx_calendar_date(calendar_date,is_available), CONSTRAINT fk_calendar_room FOREIGN KEY(room_type_id) REFERENCES tb_room_type(id)
) ;
CREATE TABLE tb_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, order_no VARCHAR(40) NOT NULL, user_id BIGINT NOT NULL, hotel_id BIGINT NOT NULL, room_type_id BIGINT NOT NULL,
  check_in DATE NOT NULL, check_out DATE NOT NULL, nights INT NOT NULL, guests INT NOT NULL DEFAULT 1, total_amount DECIMAL(10,2) NOT NULL,
  order_status VARCHAR(24) NOT NULL, payment_status VARCHAR(24) NOT NULL, cancel_reason VARCHAR(255), remark VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_order_no(order_no), KEY idx_order_user(user_id,order_status,create_time), KEY idx_order_hotel(hotel_id,check_in),
  CONSTRAINT fk_order_user FOREIGN KEY(user_id) REFERENCES tb_user(id), CONSTRAINT fk_order_hotel FOREIGN KEY(hotel_id) REFERENCES tb_hotel(id), CONSTRAINT fk_order_room FOREIGN KEY(room_type_id) REFERENCES tb_room_type(id)
) ;
CREATE TABLE tb_order_guest (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL, name VARCHAR(64) NOT NULL, phone VARCHAR(20) NOT NULL, id_card VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_guest_order(order_id), CONSTRAINT fk_guest_order FOREIGN KEY(order_id) REFERENCES tb_order(id)
) ;
CREATE TABLE tb_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL, hotel_id BIGINT NOT NULL, user_id BIGINT NOT NULL, score TINYINT NOT NULL,
  content VARCHAR(1000) NOT NULL, images JSON, merchant_reply VARCHAR(1000), status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_comment_hotel(hotel_id,status,create_time), KEY idx_comment_user(user_id), CONSTRAINT fk_comment_order FOREIGN KEY(order_id) REFERENCES tb_order(id), CONSTRAINT fk_comment_hotel FOREIGN KEY(hotel_id) REFERENCES tb_hotel(id), CONSTRAINT fk_comment_user FOREIGN KEY(user_id) REFERENCES tb_user(id)
) ;
CREATE TABLE tb_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, hotel_id BIGINT NOT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_favorite(user_id,hotel_id), CONSTRAINT fk_favorite_user FOREIGN KEY(user_id) REFERENCES tb_user(id), CONSTRAINT fk_favorite_hotel FOREIGN KEY(hotel_id) REFERENCES tb_hotel(id)
) ;
CREATE TABLE tb_banner (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, title VARCHAR(128) NOT NULL, image_url VARCHAR(500) NOT NULL, link_url VARCHAR(500), position VARCHAR(32) NOT NULL DEFAULT 'HOME', sort_no INT NOT NULL DEFAULT 0, status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0
) ;
CREATE TABLE tb_system_dict (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, dict_type VARCHAR(64) NOT NULL, dict_label VARCHAR(128) NOT NULL, dict_value VARCHAR(128) NOT NULL, sort_no INT NOT NULL DEFAULT 0, status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_dict(dict_type,dict_value)
) ;
CREATE TABLE tb_hotel_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, hotel_id BIGINT NOT NULL, auditor_id BIGINT NOT NULL, result VARCHAR(20) NOT NULL, reason VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_audit_hotel(hotel_id,create_time), CONSTRAINT fk_audit_hotel FOREIGN KEY(hotel_id) REFERENCES tb_hotel(id), CONSTRAINT fk_audit_user FOREIGN KEY(auditor_id) REFERENCES tb_user(id)
) ;
CREATE TABLE tb_order_status_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL, from_status VARCHAR(24), to_status VARCHAR(24) NOT NULL, operator_id BIGINT, remark VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_log_order(order_id,create_time), CONSTRAINT fk_log_order FOREIGN KEY(order_id) REFERENCES tb_order(id), CONSTRAINT fk_log_user FOREIGN KEY(operator_id) REFERENCES tb_user(id)
) ;
CREATE TABLE tb_private_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, hotel_id BIGINT NOT NULL, user_id BIGINT NOT NULL, merchant_id BIGINT NOT NULL,
  sender_user_id BIGINT NOT NULL, sender_role VARCHAR(20) NOT NULL, content VARCHAR(1000) NOT NULL, is_read TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_message_user_hotel(user_id,hotel_id,create_time), KEY idx_message_merchant_thread(merchant_id,hotel_id,user_id,create_time),
  KEY idx_message_unread(merchant_id,is_read,sender_role),
  CONSTRAINT fk_message_hotel FOREIGN KEY(hotel_id) REFERENCES tb_hotel(id),
  CONSTRAINT fk_message_user FOREIGN KEY(user_id) REFERENCES tb_user(id),
  CONSTRAINT fk_message_merchant FOREIGN KEY(merchant_id) REFERENCES tb_merchant(id),
  CONSTRAINT fk_message_sender FOREIGN KEY(sender_user_id) REFERENCES tb_user(id)
) ;