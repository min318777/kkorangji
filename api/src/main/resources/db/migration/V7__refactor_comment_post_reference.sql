-- Comment 테이블: boast/lost FK 컬럼 → postId + postType 으로 통합
-- 기존 FK 제약 및 인덱스 제거
ALTER TABLE comment DROP FOREIGN KEY fk_comment_boast_cat_post;
ALTER TABLE comment DROP FOREIGN KEY fk_comment_lost_cat_post;
ALTER TABLE comment DROP INDEX idx_comment_boast_post_created_at;
ALTER TABLE comment DROP INDEX idx_comment_lost_post_created_at;

-- 기존 컬럼 제거
ALTER TABLE comment DROP COLUMN boast_cat_post_id;
ALTER TABLE comment DROP COLUMN lost_cat_post_id;

-- 새 컬럼 추가
ALTER TABLE comment ADD COLUMN post_id BIGINT NOT NULL;
ALTER TABLE comment ADD COLUMN post_type VARCHAR(10) NOT NULL;

-- 통합 인덱스 생성
CREATE INDEX idx_comment_post_created_at ON comment (post_id, post_type, created_at DESC);
