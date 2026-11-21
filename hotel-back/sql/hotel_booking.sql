-- Hotel booking platform schema for MySQL 8.0+
CREATE DATABASE IF NOT EXISTS hotel_booking DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hotel_booking;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS tb_private_message, tb_order_status_log, tb_hotel_audit, tb_system_dict, tb_banner, tb_favorite, tb_comment, tb_order_guest, tb_order, tb_room_calendar, tb_room_type, tb_hotel, tb_merchant, tb_user;

CREATE TABLE tb_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, phone VARCHAR(20) NOT NULL, email VARCHAR(128) NULL, password VARCHAR(100) NOT NULL,
  name VARCHAR(64) NOT NULL, avatar VARCHAR(500), id_card VARCHAR(64), role VARCHAR(20) NOT NULL DEFAULT 'user',
  status TINYINT NOT NULL DEFAULT 1, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_phone(phone), UNIQUE KEY uk_user_email(email), KEY idx_user_role_status(role,status)
) ENGINE=InnoDB;

CREATE TABLE tb_merchant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, company_name VARCHAR(128) NOT NULL,
  contact_name VARCHAR(64), license_no VARCHAR(64), status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_merchant_user(user_id), CONSTRAINT fk_merchant_user FOREIGN KEY(user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB;

CREATE TABLE tb_hotel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, merchant_id BIGINT NOT NULL, name VARCHAR(128) NOT NULL, city VARCHAR(64) NOT NULL,
  district VARCHAR(64), address VARCHAR(255), type VARCHAR(32), cover_image VARCHAR(500), gallery JSON, description TEXT,
  facilities JSON, nearby_info JSON, status VARCHAR(20) NOT NULL DEFAULT 'PENDING_AUDIT', min_price DECIMAL(10,2) DEFAULT 0, score DECIMAL(3,2) DEFAULT 0,
  review_count INT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_hotel_city_status(city,status), KEY idx_hotel_merchant(merchant_id,status), KEY idx_hotel_type(type),
  CONSTRAINT fk_hotel_merchant FOREIGN KEY(merchant_id) REFERENCES tb_merchant(id)
) ENGINE=InnoDB;

CREATE TABLE tb_room_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, hotel_id BIGINT NOT NULL, name VARCHAR(128) NOT NULL, image VARCHAR(500), area INT,
  bed VARCHAR(64), max_guests INT NOT NULL DEFAULT 2, base_price DECIMAL(10,2) NOT NULL, stock INT NOT NULL DEFAULT 0,
  facilities JSON, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_room_hotel(hotel_id), CONSTRAINT fk_room_hotel FOREIGN KEY(hotel_id) REFERENCES tb_hotel(id)
) ENGINE=InnoDB;

CREATE TABLE tb_room_calendar (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, room_type_id BIGINT NOT NULL, calendar_date DATE NOT NULL, price DECIMAL(10,2) NOT NULL,
  available_stock INT NOT NULL DEFAULT 0, is_available TINYINT NOT NULL DEFAULT 1, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_room_date(room_type_id,calendar_date), KEY idx_calendar_date(calendar_date,is_available), CONSTRAINT fk_calendar_room FOREIGN KEY(room_type_id) REFERENCES tb_room_type(id)
) ENGINE=InnoDB;

CREATE TABLE tb_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, order_no VARCHAR(40) NOT NULL, user_id BIGINT NOT NULL, hotel_id BIGINT NOT NULL, room_type_id BIGINT NOT NULL,
  check_in DATE NOT NULL, check_out DATE NOT NULL, nights INT NOT NULL, guests INT NOT NULL DEFAULT 1, total_amount DECIMAL(10,2) NOT NULL,
  order_status VARCHAR(24) NOT NULL, payment_status VARCHAR(24) NOT NULL, cancel_reason VARCHAR(255), remark VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_order_no(order_no), KEY idx_order_user(user_id,order_status,create_time), KEY idx_order_hotel(hotel_id,check_in),
  CONSTRAINT fk_order_user FOREIGN KEY(user_id) REFERENCES tb_user(id), CONSTRAINT fk_order_hotel FOREIGN KEY(hotel_id) REFERENCES tb_hotel(id), CONSTRAINT fk_order_room FOREIGN KEY(room_type_id) REFERENCES tb_room_type(id)
) ENGINE=InnoDB;

CREATE TABLE tb_order_guest (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL, name VARCHAR(64) NOT NULL, phone VARCHAR(20) NOT NULL, id_card VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_guest_order(order_id), CONSTRAINT fk_guest_order FOREIGN KEY(order_id) REFERENCES tb_order(id)
) ENGINE=InnoDB;

CREATE TABLE tb_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL, hotel_id BIGINT NOT NULL, user_id BIGINT NOT NULL, score TINYINT NOT NULL,
  content VARCHAR(1000) NOT NULL, images JSON, merchant_reply VARCHAR(1000), status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_comment_hotel(hotel_id,status,create_time), KEY idx_comment_user(user_id), CONSTRAINT fk_comment_order FOREIGN KEY(order_id) REFERENCES tb_order(id), CONSTRAINT fk_comment_hotel FOREIGN KEY(hotel_id) REFERENCES tb_hotel(id), CONSTRAINT fk_comment_user FOREIGN KEY(user_id) REFERENCES tb_user(id)
) ENGINE=InnoDB;

CREATE TABLE tb_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, hotel_id BIGINT NOT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_favorite(user_id,hotel_id), CONSTRAINT fk_favorite_user FOREIGN KEY(user_id) REFERENCES tb_user(id), CONSTRAINT fk_favorite_hotel FOREIGN KEY(hotel_id) REFERENCES tb_hotel(id)
) ENGINE=InnoDB;

CREATE TABLE tb_banner (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, title VARCHAR(128) NOT NULL, image_url VARCHAR(500) NOT NULL, link_url VARCHAR(500), position VARCHAR(32) NOT NULL DEFAULT 'HOME', sort_no INT NOT NULL DEFAULT 0, status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE tb_system_dict (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, dict_type VARCHAR(64) NOT NULL, dict_label VARCHAR(128) NOT NULL, dict_value VARCHAR(128) NOT NULL, sort_no INT NOT NULL DEFAULT 0, status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_dict(dict_type,dict_value)
) ENGINE=InnoDB;

CREATE TABLE tb_hotel_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, hotel_id BIGINT NOT NULL, auditor_id BIGINT NOT NULL, result VARCHAR(20) NOT NULL, reason VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_audit_hotel(hotel_id,create_time), CONSTRAINT fk_audit_hotel FOREIGN KEY(hotel_id) REFERENCES tb_hotel(id), CONSTRAINT fk_audit_user FOREIGN KEY(auditor_id) REFERENCES tb_user(id)
) ENGINE=InnoDB;

CREATE TABLE tb_order_status_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, order_id BIGINT NOT NULL, from_status VARCHAR(24), to_status VARCHAR(24) NOT NULL, operator_id BIGINT, remark VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_log_order(order_id,create_time), CONSTRAINT fk_log_order FOREIGN KEY(order_id) REFERENCES tb_order(id), CONSTRAINT fk_log_user FOREIGN KEY(operator_id) REFERENCES tb_user(id)
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

-- Demo password for all accounts: Hotel@123
INSERT INTO tb_user(id,phone,password,name,role,status) VALUES
(1,'13800000001','$2b$10$Id3e63UfpJNgN0JQS0esYefK8SeVdOjaB.pd2qnv/h8IJEJ7TC7Sa','平台管理员','admin',1),
(2,'13800000002','$2b$10$Id3e63UfpJNgN0JQS0esYefK8SeVdOjaB.pd2qnv/h8IJEJ7TC7Sa','山里见民宿','merchant',1),
(3,'13800000003','$2b$10$Id3e63UfpJNgN0JQS0esYefK8SeVdOjaB.pd2qnv/h8IJEJ7TC7Sa','体验住客','user',1);
INSERT INTO tb_merchant(id,user_id,company_name,contact_name,status) VALUES(1,2,'山里见旅宿管理','山里见','APPROVED');
INSERT INTO tb_hotel(id,merchant_id,name,city,district,address,type,cover_image,gallery,description,facilities,status,min_price,score,review_count) VALUES
(1,1,'月牙湾·湖畔民宿','杭州','西湖区','杭州西湖边 18 号','民宿','https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=85'),'临湖而居的安静民宿。',JSON_ARRAY('免费 WiFi','停车场','早餐'),'ONLINE',688,4.9,12),
(2,1,'云上白房子','杭州','余杭区','杭州余杭径山 77 号','民宿','https://images.unsplash.com/photo-1600607687920-4e2a09cf159d?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1600607687920-4e2a09cf159d?auto=format&fit=crop&w=1200&q=85'),'山间院落与自然光。',JSON_ARRAY('免费 WiFi','停车场','投影'),'ONLINE',528,4.8,9);
UPDATE tb_hotel SET nearby_info = JSON_ARRAY(
  JSON_OBJECT('type','景点','name','广州塔','distance','步行 12 分钟','description','夜景与珠江观光地标'),
  JSON_OBJECT('type','交通','name','广州塔地铁站','distance','步行 8 分钟','description','地铁 3 号线与 APM 线'),
  JSON_OBJECT('type','餐饮','name','珠江琶醍','distance','驾车 9 分钟','description','江景餐厅与夜间休闲街区')
) WHERE id = 1;
UPDATE tb_hotel SET nearby_info = JSON_ARRAY(
  JSON_OBJECT('type','景点','name','西湖风景区','distance','步行 6 分钟','description','可沿湖散步并游览断桥'),
  JSON_OBJECT('type','交通','name','龙翔桥地铁站','distance','步行 10 分钟','description','地铁 1 号线'),
  JSON_OBJECT('type','餐饮','name','湖滨步行街','distance','步行 12 分钟','description','杭帮菜与本地小吃集中区域')
) WHERE id = 2;
INSERT INTO tb_hotel(id,merchant_id,name,city,district,address,type,cover_image,gallery,description,facilities,status,min_price,score,review_count) VALUES
(3000,1,'秦淮河畔·桨声民宿','南京','秦淮区','南京秦淮区演示地址','民宿','https://images.unsplash.com/photo-1600607687920-4e2a09cf159d?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1600607687920-4e2a09cf159d?auto=format&fit=crop&w=1200&q=85'),'夫子庙与老门东之间的河畔民宿',JSON_ARRAY('免费 WiFi','免费停车','空调'),'ONLINE',368,4.7,0),
(3001,1,'钟山景序酒店','南京','玄武区','南京玄武区演示地址','精品酒店','https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=1200&q=85'),'靠近钟山风景区的城市度假酒店',JSON_ARRAY('免费 WiFi','免费停车','早餐'),'ONLINE',488,4.8,0),
(3002,1,'山城夜景·江畔民宿','重庆','渝中区','重庆渝中区演示地址','民宿','https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=85'),'步行可达解放碑与洪崖洞的城市民宿',JSON_ARRAY('免费 WiFi','免费停车','观景台'),'ONLINE',428,4.7,0),
(3003,1,'南山观景酒店','重庆','南岸区','重庆南岸区演示地址','精品酒店','https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=85'),'面向两江夜景的山城精品酒店',JSON_ARRAY('免费 WiFi','免费停车','观景台'),'ONLINE',568,4.8,0),
(3004,1,'大唐不夜城·唐风客栈','西安','雁塔区','西安雁塔区演示地址','民宿','https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=1200&q=85'),'适合步行探索大雁塔与大唐不夜城',JSON_ARRAY('免费 WiFi','免费停车','行李寄存'),'ONLINE',298,4.7,0),
(3005,1,'城墙里精品酒店','西安','莲湖区','西安莲湖区演示地址','精品酒店','https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=1200&q=85'),'靠近钟楼与古城墙的城市住宿',JSON_ARRAY('免费 WiFi','免费停车','早餐'),'ONLINE',398,4.8,0),
(3006,1,'八大关·海风小院','青岛','市南区','青岛市南区演示地址','民宿','https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=1200&q=85'),'临近海滨与老城区的庭院民宿',JSON_ARRAY('免费 WiFi','免费停车','庭院'),'ONLINE',438,4.7,0),
(3007,1,'崂山云海度假酒店','青岛','崂山区','青岛崂山区演示地址','度假屋','https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=85'),'适合海边休闲与山野度假的度假酒店',JSON_ARRAY('免费 WiFi','免费停车','山景'),'ONLINE',628,4.8,0),
(3008,1,'椰林湾·海景民宿','三亚','吉阳区','三亚吉阳区演示地址','民宿','https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=85'),'靠近海湾与椰林的轻度假民宿',JSON_ARRAY('免费 WiFi','免费停车','海景'),'ONLINE',598,4.7,0),
(3009,1,'南海云端度假酒店','三亚','天涯区','三亚天涯区演示地址','度假屋','https://images.unsplash.com/photo-1564501049412-61c2a3083791?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1564501049412-61c2a3083791?auto=format&fit=crop&w=1200&q=85'),'适合亲子与海边度假的滨海酒店',JSON_ARRAY('免费 WiFi','免费停车','泳池'),'ONLINE',888,4.8,0),
(3010,1,'东湖听涛·湖景民宿','武汉','武昌区','武汉武昌区演示地址','民宿','https://images.unsplash.com/photo-1600047509807-ba8f99d2cdde?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1600047509807-ba8f99d2cdde?auto=format&fit=crop&w=1200&q=85'),'邻近东湖与楚河汉街的湖畔住宿',JSON_ARRAY('免费 WiFi','免费停车','湖景'),'ONLINE',328,4.7,0),
(3011,1,'江汉路城市酒店','武汉','江汉区','武汉江汉区演示地址','精品酒店','https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1200&q=85'),'适合城市漫游与商务出行的酒店',JSON_ARRAY('免费 WiFi','免费停车','早餐'),'ONLINE',358,4.8,0),
(3012,1,'平江路·园林小筑','苏州','姑苏区','苏州姑苏区演示地址','民宿','https://images.unsplash.com/photo-1600607688969-a5bfcd646154?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1600607688969-a5bfcd646154?auto=format&fit=crop&w=1200&q=85'),'靠近平江路与拙政园的园林民宿',JSON_ARRAY('免费 WiFi','免费停车','庭院'),'ONLINE',468,4.7,0),
(3013,1,'山塘夜泊酒店','苏州','虎丘区','苏州虎丘区演示地址','精品酒店','https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=1200&q=85'),'方便探索山塘街与古城街巷的酒店',JSON_ARRAY('免费 WiFi','免费停车','早餐'),'ONLINE',428,4.8,0),
(3014,1,'漓江晨雾·江景客栈','桂林','秀峰区','桂林秀峰区演示地址','民宿','https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=1200&q=85'),'适合山水观景与城市漫步的客栈',JSON_ARRAY('免费 WiFi','免费停车','江景'),'ONLINE',388,4.7,0),
(3015,1,'遇龙河畔度假屋','桂林','阳朔县','桂林阳朔县演示地址','度假屋','https://images.unsplash.com/photo-1535827841776-24afc1e255ac?auto=format&fit=crop&w=1200&q=85',JSON_ARRAY('https://images.unsplash.com/photo-1535827841776-24afc1e255ac?auto=format&fit=crop&w=1200&q=85'),'靠近遇龙河与十里画廊的度假住宿',JSON_ARRAY('免费 WiFi','免费停车','山景'),'ONLINE',528,4.8,0);
INSERT INTO tb_room_type(id,hotel_id,name,image,area,bed,max_guests,base_price,stock,facilities) VALUES
(3000,3000,'舒适大床房','',32,'1.8米大床',2,368,6,JSON_ARRAY('空调','免费 WiFi')),(3001,3001,'舒适大床房','',32,'1.8米大床',2,488,6,JSON_ARRAY('空调','免费 WiFi')),(3002,3002,'舒适大床房','',32,'1.8米大床',2,428,6,JSON_ARRAY('空调','免费 WiFi')),(3003,3003,'舒适大床房','',32,'1.8米大床',2,568,6,JSON_ARRAY('空调','免费 WiFi')),(3004,3004,'舒适大床房','',32,'1.8米大床',2,298,6,JSON_ARRAY('空调','免费 WiFi')),(3005,3005,'舒适大床房','',32,'1.8米大床',2,398,6,JSON_ARRAY('空调','免费 WiFi')),(3006,3006,'舒适大床房','',32,'1.8米大床',2,438,6,JSON_ARRAY('空调','免费 WiFi')),(3007,3007,'舒适大床房','',32,'1.8米大床',2,628,6,JSON_ARRAY('空调','免费 WiFi')),(3008,3008,'舒适大床房','',32,'1.8米大床',2,598,6,JSON_ARRAY('空调','免费 WiFi')),(3009,3009,'舒适大床房','',32,'1.8米大床',2,888,6,JSON_ARRAY('空调','免费 WiFi')),(3010,3010,'舒适大床房','',32,'1.8米大床',2,328,6,JSON_ARRAY('空调','免费 WiFi')),(3011,3011,'舒适大床房','',32,'1.8米大床',2,358,6,JSON_ARRAY('空调','免费 WiFi')),(3012,3012,'舒适大床房','',32,'1.8米大床',2,468,6,JSON_ARRAY('空调','免费 WiFi')),(3013,3013,'舒适大床房','',32,'1.8米大床',2,428,6,JSON_ARRAY('空调','免费 WiFi')),(3014,3014,'舒适大床房','',32,'1.8米大床',2,388,6,JSON_ARRAY('空调','免费 WiFi')),(3015,3015,'舒适大床房','',32,'1.8米大床',2,528,6,JSON_ARRAY('空调','免费 WiFi'));
INSERT INTO tb_room_type(id,hotel_id,name,image,area,bed,max_guests,base_price,stock,facilities) VALUES
(1,1,'湖景大床房','',42,'1.8米大床',2,688,4,JSON_ARRAY('浴缸','落地窗')),
(2,2,'山景大床房','',36,'1.8米大床',2,528,5,JSON_ARRAY('投影','露台'));
INSERT INTO tb_room_type(hotel_id,name,image,area,bed,max_guests,base_price,stock,facilities)
SELECT 1,'湖景露台双床房','https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1000&q=85',38,'两张1.2米床',2,598,3,JSON_ARRAY('独立卫浴','观景露台','免费 WiFi')
WHERE NOT EXISTS (SELECT 1 FROM tb_room_type WHERE hotel_id=1 AND name='湖景露台双床房');
INSERT INTO tb_room_type(hotel_id,name,image,area,bed,max_guests,base_price,stock,facilities)
SELECT 1,'亲子家庭套房','https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1000&q=85',52,'1.8米大床 + 儿童床',4,828,2,JSON_ARRAY('儿童用品','独立客厅','免费 WiFi')
WHERE NOT EXISTS (SELECT 1 FROM tb_room_type WHERE hotel_id=1 AND name='亲子家庭套房');
INSERT INTO tb_banner(title,image_url,link_url,position,sort_no,status) VALUES('住进好日子','https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?auto=format&fit=crop&w=1600&q=85','/search','HOME',1,1);
INSERT INTO tb_system_dict(dict_type,dict_label,dict_value,sort_no,status) VALUES
('hotel_type','民宿','民宿',1,1),('hotel_type','精品酒店','精品酒店',2,1),('hotel_type','度假屋','度假屋',3,1),('facility','免费 WiFi','免费 WiFi',1,1),('facility','停车场','停车场',2,1),('facility','早餐','早餐',3,1);

INSERT INTO tb_room_calendar(room_type_id,calendar_date,price,available_stock,is_available)
WITH RECURSIVE dates AS (SELECT DATE('2026-01-01') AS d UNION ALL SELECT d + INTERVAL 1 DAY FROM dates WHERE d < DATE('2027-12-31'))
SELECT r.id,dates.d,r.base_price,r.stock,1 FROM tb_room_type r CROSS JOIN dates;
SET FOREIGN_KEY_CHECKS = 1;
