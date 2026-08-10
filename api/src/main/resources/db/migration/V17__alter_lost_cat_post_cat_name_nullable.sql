-- cat_name 선택 사항으로 변경 → nullable 처리
ALTER TABLE lost_cat_post MODIFY COLUMN cat_name VARCHAR(255) NULL DEFAULT NULL;
