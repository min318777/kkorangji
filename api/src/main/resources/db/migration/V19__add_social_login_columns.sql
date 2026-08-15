-- 카카오 소셜 로그인 지원: provider/social_id 컬럼 추가, login_id는 소셜 계정이 null로 저장되므로 nullable 전환
ALTER TABLE user
    ADD COLUMN provider VARCHAR(20) NULL,
    ADD COLUMN social_id VARCHAR(50) NULL;

ALTER TABLE user
    ADD UNIQUE KEY uq_provider_social_id (provider, social_id);

ALTER TABLE user
    MODIFY COLUMN login_id VARCHAR(20) NULL;
