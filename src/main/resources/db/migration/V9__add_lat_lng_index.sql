-- lost_cat_post: BB 방식 성능 비교를 위한 위도/경도 B-Tree 인덱스 추가
CREATE INDEX idx_lost_post_latitude ON lost_cat_post (latitude);
CREATE INDEX idx_lost_post_longitude ON lost_cat_post (longitude);