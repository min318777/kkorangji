-- RBAC 초기 데이터 삽입 (멱등성 보장: 이미 존재하면 스킵)

-- ── Permission 10개 ──────────────────────────────────────────────
INSERT IGNORE INTO permission (code, description) VALUES
    ('post:read',      '게시글 조회'),
    ('post:create',    '게시글 작성'),
    ('post:update',    '게시글 수정'),
    ('post:delete',    '게시글 삭제 (타인 포함)'),
    ('comment:create', '댓글 작성'),
    ('comment:update', '댓글 수정'),
    ('comment:delete', '댓글 삭제 (타인 포함)'),
    ('user:read',      '유저 목록/통계 조회'),
    ('user:restrict',  '유저 계정 제재/복원'),
    ('user:delete',    '유저 강제 탈퇴');

-- ── Role 4개 ─────────────────────────────────────────────────────
INSERT IGNORE INTO role (name, description) VALUES
    ('ROLE_USER',       '일반 사용자'),
    ('ROLE_ADMIN',      '관리자'),
    ('ROLE_VIEWER',     '뷰어 (콘텐츠 관리 가능)'),
    ('ROLE_RESTRICTED', '제한된 사용자');

-- ── RolePermission 매핑 ──────────────────────────────────────────
-- ROLE_USER: 조회, 작성, 수정, 댓글 작성/수정
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.name = 'ROLE_USER'
  AND p.code IN ('post:read', 'post:create', 'post:update', 'comment:create', 'comment:update');

-- ROLE_VIEWER: 콘텐츠 전체 관리 + 유저 조회
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.name = 'ROLE_VIEWER'
  AND p.code IN ('post:read', 'post:create', 'post:update', 'post:delete',
                 'comment:create', 'comment:update', 'comment:delete', 'user:read');

-- ROLE_ADMIN: 모든 권한
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.name = 'ROLE_ADMIN';

-- ROLE_RESTRICTED: 조회만 가능
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p
WHERE r.name = 'ROLE_RESTRICTED'
  AND p.code = 'post:read';
