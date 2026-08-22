-- boast_cat_post: 인기글 (ORDER BY like_count DESC LIMIT 24)
CREATE INDEX idx_boast_post_like_count ON boast_cat_post (like_count DESC);
