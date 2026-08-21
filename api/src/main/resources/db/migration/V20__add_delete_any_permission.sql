-- post:delete/comment:delete의 의미를 "본인 콘텐츠 삭제 게이트"로 재정의하고
-- (제재된 ROLE_RESTRICTED만 제외, 나머지 역할은 본인 삭제 가능),
-- "타인 콘텐츠 삭제(관리자용)"는 새 권한 post:delete:any / comment:delete:any로 분리한다.

INSERT IGNORE INTO permission (code, description) VALUES
    ('post:delete:any',    '게시글 삭제 (타인 포함, 관리자용)'),
    ('comment:delete:any', '댓글 삭제 (타인 포함, 관리자용)');

-- ROLE_USER: 본인 삭제 게이트(post:delete, comment:delete) 부여
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.name = 'ROLE_USER'
  AND p.code IN ('post:delete', 'comment:delete');

-- ROLE_VIEWER, ROLE_ADMIN: 기존 post:delete/comment:delete가 하던 "타인 삭제" 권한을
-- post:delete:any/comment:delete:any로 이관 (본인 삭제 게이트는 이미 보유)
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.name IN ('ROLE_VIEWER', 'ROLE_ADMIN')
  AND p.code IN ('post:delete:any', 'comment:delete:any');