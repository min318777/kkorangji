-- notification.receiver_login_id 컬럼이 엔티티에 없어 INSERT 시 오류 발생 → nullable로 변경
ALTER TABLE notification MODIFY COLUMN receiver_login_id VARCHAR(255) NULL DEFAULT NULL;
