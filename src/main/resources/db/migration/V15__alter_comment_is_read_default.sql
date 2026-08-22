-- comment.is_read 컬럼 기본값 없음으로 INSERT 오류 발생 → DEFAULT 0 추가
ALTER TABLE comment MODIFY COLUMN is_read TINYINT(1) NOT NULL DEFAULT 0;