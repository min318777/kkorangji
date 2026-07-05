ALTER TABLE lost_cat_post
ADD FULLTEXT INDEX ft_lost_cat_post_title_contents (title, contents) WITH PARSER ngram;
