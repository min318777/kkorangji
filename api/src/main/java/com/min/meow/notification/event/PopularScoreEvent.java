package com.min.meow.notification.event;

/**
 * 인기글 점수 변경 이벤트
 * 좋아요(±3), 댓글(±2) 발생 시 발행 → PopularRankingService가 Sorted Set ZINCRBY
 */
public record PopularScoreEvent(Long postId, int scoreDelta) {}
