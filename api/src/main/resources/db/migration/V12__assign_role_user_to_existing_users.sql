-- 기존 유저 중 역할이 없는 유저에게 ROLE_USER 일괄 부여
INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM user u, role r
WHERE r.name = 'ROLE_USER'
  AND u.id NOT IN (SELECT user_id FROM user_role);
