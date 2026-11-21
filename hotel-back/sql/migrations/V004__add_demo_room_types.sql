USE hotel_booking;

INSERT INTO tb_room_type(hotel_id,name,image,area,bed,max_guests,base_price,stock,facilities)
SELECT 1,'湖景露台双床房','https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1000&q=85',38,'两张1.2米床',2,598,3,JSON_ARRAY('独立卫浴','观景露台','免费 WiFi')
WHERE NOT EXISTS (SELECT 1 FROM tb_room_type WHERE hotel_id=1 AND name='湖景露台双床房');

INSERT INTO tb_room_type(hotel_id,name,image,area,bed,max_guests,base_price,stock,facilities)
SELECT 1,'亲子家庭套房','https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1000&q=85',52,'1.8米大床 + 儿童床',4,828,2,JSON_ARRAY('儿童用品','独立客厅','免费 WiFi')
WHERE NOT EXISTS (SELECT 1 FROM tb_room_type WHERE hotel_id=1 AND name='亲子家庭套房');

INSERT INTO tb_room_calendar(room_type_id,calendar_date,price,available_stock,is_available)
WITH RECURSIVE dates AS (
  SELECT DATE('2026-01-01') AS calendar_date
  UNION ALL
  SELECT calendar_date + INTERVAL 1 DAY FROM dates WHERE calendar_date < DATE('2027-12-31')
)
SELECT room.id, dates.calendar_date, room.base_price, room.stock, 1
FROM tb_room_type room
CROSS JOIN dates
LEFT JOIN tb_room_calendar calendar
  ON calendar.room_type_id = room.id AND calendar.calendar_date = dates.calendar_date
WHERE room.hotel_id = 1
  AND room.name IN ('湖景露台双床房','亲子家庭套房')
  AND calendar.id IS NULL;
