package com.min.meow.user.dto.reponse;

import com.min.meow.post.comment.entity.Comment;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 내가 쓴 댓글 정보 DTO
 * 댓글이 달린 게시글 정보도 함께 포함
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyCommentDto {

    private Long commentId;           // 댓글 ID
    private String contents;          // 댓글 내용
    private Long postId;              // 게시글 ID
    private String postType;          // 게시글 타입: "BOAST" 또는 "LOST"
    private String postTitle;         // 게시글 제목
    private LocalDateTime createdAt;  // 작성일
    private LocalDateTime updatedAt;  // 수정일

    /**
     * Comment 엔티티를 DTO로 변환
     */
    public static MyCommentDto fromComment(Comment comment) {
        // 댓글이 어느 게시글에 달렸는지 확인
        boolean isBoastPost = comment.getBoastCatPost() != null;

        return MyCommentDto.builder()
                .commentId(comment.getId())
                .contents(comment.getContents())
                .postId(isBoastPost ?
                        comment.getBoastCatPost().getId() :
                        comment.getLostCatPost().getId())
                .postType(isBoastPost ? "BOAST" : "LOST")
                .postTitle(isBoastPost ?
                        comment.getBoastCatPost().getTitle() :
                        comment.getLostCatPost().getTitle())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}