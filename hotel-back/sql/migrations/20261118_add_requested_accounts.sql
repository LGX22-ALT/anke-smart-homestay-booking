-- Requested additional local accounts, 2026-11-18. BCrypt only; no plaintext passwords.
SET NAMES utf8mb4;
START TRANSACTION;
INSERT INTO tb_user (phone,email,password,name,role,status)
VALUES ('13900001001','merchant1001@hotel.example','$2b$10$gs9Z8jeBznsQ0NSQp6pcgejOC9azHnYiZT0f/0bTgxeBYA2vqrnNm','新民宿商家','merchant',1);
SET @requested_merchant_user_id = LAST_INSERT_ID();
INSERT INTO tb_merchant (user_id,company_name,contact_name,status)
VALUES (@requested_merchant_user_id,'新民宿商家','商家负责人','APPROVED');
INSERT INTO tb_user (phone,email,password,name,role,status)
VALUES ('13900001002','admin1002@hotel.example','$2b$10$HhgBJQ0j9SYuUSJXDBh7AOCMSyfbDehG3z.msmEHhqnJi1QyXJinq','新平台管理员','admin',1);
COMMIT;
