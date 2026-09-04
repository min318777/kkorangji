-- post:update, comment:update를 ROLE_USER/ROLE_VIEWER에서 제거
-- 본인 글/댓글 수정은 서비스 레이어의 소유권 체크로 별도 허용되므로 영향 없음
-- 이 권한은 이제 "타인 글/댓글까지 수정 가능한 관리자 자격"만 의미함 (ROLE_ADMIN 전용)
DELETE rp FROM role_permission rp
JOIN role r ON rp.role_id = r.id
JOIN permission p ON rp.permission_id = p.id
WHERE r.name IN ('ROLE_USER', 'ROLE_VIEWER')
  AND p.code IN ('post:update', 'comment:update');
