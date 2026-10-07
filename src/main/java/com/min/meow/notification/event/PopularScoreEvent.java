package com.min.meow.notification.event;

// 인기글 점수 변경 이벤트
public record PopularScoreEvent(Long postId, int scoreDelta) {}
