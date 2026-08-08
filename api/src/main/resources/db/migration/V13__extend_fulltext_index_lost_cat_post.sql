-- 기존 인덱스 제거 후 cat_name, lost_location 포함한 새 인덱스 생성
ALTER TABLE lost_cat_post DROP INDEX ft_lost_cat_post_title_contents;

ALTER TABLE lost_cat_post
ADD FULLTEXT INDEX ft_lost_cat_post_title_contents (title, contents, cat_name, lost_location) WITH PARSER ngram;
