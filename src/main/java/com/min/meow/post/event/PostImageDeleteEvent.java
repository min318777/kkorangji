package com.min.meow.post.event;

import java.util.List;

// 게시글 삭제/수정으로 정리가 필요한 S3 이미지 key 목록 이벤트 — 트랜잭션 커밋 후 비동기 S3 삭제용
public record PostImageDeleteEvent(List<String> imageKeys) {}
