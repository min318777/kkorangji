-- notification 테이블에 post_type 컬럼 추가
-- Notification 엔티티의 postType 필드와 스키마 동기화
ALTER TABLE notification
    ADD COLUMN post_type VARCHAR(10) NOT NULL DEFAULT 'BOAST';
