package com.min.meow.comment.service;


import com.min.kafka.producer.NotificationSender;
import com.min.meow.notice.NoticeDto;
import com.min.meow.kafka.producer.KafkaMessageProducer;
import com.min.meow.comment.domain.dto.RegisterPostCommentDto;
import com.min.meow.comment.domain.dto.UpdatePostCommentDto;
import com.min.meow.comment.domain.request.RegisterPostCommentRequest;
import com.min.meow.comment.domain.request.UpdatePostCommentRequest;
import com.min.meow.comment.entity.PostComment;
import com.min.meow.comment.repository.CommentRepository;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.lostcatpost.entity.LostCatPost;
import com.min.meow.lostcatpost.repository.LostCatRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class CommentService {

    private final LostCatRepository lostCatRepository;
    private final CommentRepository commentRepository;
    private final KafkaMessageProducer kafkaMessageProducer;
    private final NotificationSender notificationSender;

    // 댓글 작성
    @Transactional
    public RegisterPostCommentDto registerLostCatPostComment(RegisterPostCommentRequest registerPostCommentRequest, Long lostCatPostId){
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        PostComment postComment = PostComment.convertToEntity(registerPostCommentRequest, lostCatPost);
        lostCatPost.getPostComments().add(postComment);
        commentRepository.save(postComment);

        NoticeDto noticeDto = NoticeDto.builder()
                .commentId(postComment.getPostCommentId())
                .postId(lostCatPostId)
                .message("게시글에 댓글이 달렸습니다.")
                .isRead(false)
                .receiverLoginId(lostCatPost.getUser().getLoginId())
                .createdAt(LocalDateTime.now())
                .build();
        produceTopic(noticeDto);
        return RegisterPostCommentDto.convertToDto(postComment);
    }

    // 댓글 수정
    @Transactional
    public UpdatePostCommentDto updateLostCatPostComment(UpdatePostCommentRequest updatePostCommentRequest, Long lostCatPostCommentId){
        PostComment postComment = commentRepository.findById(lostCatPostCommentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        postComment.update(updatePostCommentRequest);

        return UpdatePostCommentDto.convertToDto(postComment);
    }

    // 댓글 삭제
    @Transactional
    public void deleteLostCatPostComment(Long lostCatPostCommentId){

        if(!commentRepository.existsById(lostCatPostCommentId)){
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }

        commentRepository.deleteById(lostCatPostCommentId);
    }

    public void produceTopic(NoticeDto noticeDto){

        notificationSender.send("lost-cat-topic", NoticeDto.toKafkaDto(noticeDto));
    }
}
