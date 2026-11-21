-- Display copy only. Run against hotel_booking after 20261115_demo_city_catalog.sql.
-- Preserve sample provenance; this does not turn sample listings into real properties.
-- Exact markers and known seed IDs protect independently edited listings. Safe to rerun.
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE tb_hotel
SET description = REPLACE(description,
    '此房源为本地演示数据，名称、图片、地址、价格和库存均用于演示，不代表真实商家。',
    '房源资料仅供功能预览，名称、图片、地址、价格及库存不作为实际入住依据。')
WHERE id BETWEEN 2000 AND 2075 AND is_deleted = 0
  AND description LIKE '%此房源为本地演示数据，名称、图片、地址、价格和库存均用于演示，不代表真实商家。%';
UPDATE tb_hotel
SET address = REPLACE(address, '（演示地址）', '（详细地址待完善）')
WHERE id BETWEEN 2000 AND 2075 AND is_deleted = 0
  AND address LIKE '%（演示地址）%';
COMMIT;
