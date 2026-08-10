-- user.name 컬럼이 엔티티에 없어 INSERT 시 오류 발생 → nullable로 변경
ALTER TABLE user MODIFY COLUMN name VARCHAR(255) NULL DEFAULT NULL;
