package com.min.meow.post.event;

import java.util.List;

/**
 * 게시글 삭제/수정으로 인해 정리가 필요한 S3 이미지 key 목록 이벤트
 * 트랜잭션 커밋 후 비동기로 S3 삭제를 수행하기 위해 발행된다.
 */
public record PostImageDeleteEvent(List<String> imageKeys) {}
