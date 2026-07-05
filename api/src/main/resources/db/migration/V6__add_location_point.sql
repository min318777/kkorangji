-- lost_cat_post: POINT 컬럼 추가 (SPATIAL INDEX용)
-- 기존 latitude/longitude DOUBLE 컬럼은 bbox 방식 비교를 위해 그대로 유지
-- 기존 데이터(100만 건)는 scripts/migrate_location.sql로 별도 배치 실행
ALTER TABLE lost_cat_post
  ADD COLUMN location POINT SRID 4326;

CREATE SPATIAL INDEX idx_lost_post_location ON lost_cat_post (location);
