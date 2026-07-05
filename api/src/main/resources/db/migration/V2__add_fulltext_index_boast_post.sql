ALTER TABLE boast_cat_post
ADD FULLTEXT INDEX ft_boast_post_title_contents (title, contents) WITH PARSER ngram;
