DELETE rp FROM role_permission rp
JOIN role r ON rp.role_id = r.id
JOIN permission p ON rp.permission_id = p.id
WHERE r.name = 'ROLE_VIEWER'
  AND p.code IN ('post:delete', 'comment:delete');

UPDATE role SET description = '뷰어 (콘텐츠 작성/수정 테스트용, 타인 글 삭제 불가)' WHERE name = 'ROLE_VIEWER';
