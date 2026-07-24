-- Comment 테이블: boast/lost FK 컬럼 → postId + postType 으로 통합

-- 1. 새 컬럼 추가 (임시 DEFAULT로 NOT NULL 허용)
ALTER TABLE comment ADD COLUMN post_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE comment ADD COLUMN post_type VARCHAR(10) NOT NULL DEFAULT 'UNKNOWN';

-- 2. 기존 데이터 마이그레이션 (10만 건 데이터 보존)
UPDATE comment SET post_id = boast_cat_post_id, post_type = 'BOAST'
WHERE boast_cat_post_id IS NOT NULL;

UPDATE comment SET post_id = lost_cat_post_id, post_type = 'LOST'
WHERE lost_cat_post_id IS NOT NULL;

-- 3. 기존 FK 제약 제거 (Hibernate 자동 생성 이름)
ALTER TABLE comment DROP FOREIGN KEY FKnfgsiwi0rkscxk6kcxop76896;
ALTER TABLE comment DROP FOREIGN KEY FKjo22sc7incye67vstxeb13adc;

-- 4. 기존 인덱스 제거
ALTER TABLE comment DROP INDEX idx_comment_boast_post_created_at;
ALTER TABLE comment DROP INDEX idx_comment_lost_post_created_at;

-- 5. 기존 컬럼 제거
ALTER TABLE comment DROP COLUMN boast_cat_post_id;
ALTER TABLE comment DROP COLUMN lost_cat_post_id;

-- 6. 임시 DEFAULT 제거
ALTER TABLE comment ALTER COLUMN post_id DROP DEFAULT;
ALTER TABLE comment ALTER COLUMN post_type DROP DEFAULT;

-- 7. 통합 인덱스 생성
CREATE INDEX idx_comment_post_created_at ON comment (post_id, post_type, created_at DESC);
