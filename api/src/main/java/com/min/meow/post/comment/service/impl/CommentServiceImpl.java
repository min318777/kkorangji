package com.min.meow.post.comment.service.impl;


import com.min.kafka.producer.NotificationSender;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.kafka.producer.KafkaMessageProducer;
import com.min.meow.notification.domain.NotificationDto;
import com.min.meow.post.comment.domain.request.RegisterCommentRequest;
import com.min.meow.post.comment.domain.request.UpdateCommentRequest;
import com.min.meow.post.comment.domain.response.RegisterCommentResponse;
import com.min.meow.post.comment.domain.response.UpdateCommentResponse;
import com.min.meow.post.comment.entity.Comment;
import com.min.meow.post.comment.repository.CommentRepository;
import com.min.meow.post.comment.service.CommentService;
import com.min.meow.post.entity.LostCatPost;
import com.min.meow.post.repository.LostCatRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final LostCatRepository lostCatRepository;
    private final CommentRepository commentRepository;
    private final NotificationSender notificationSender;

    // 댓글 작성
    @Transactional
    @Override
    public RegisterCommentResponse registerLostCatPostComment(RegisterCommentRequest registerCommentRequest, Long lostCatPostId){
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));
        
        Comment comment = Comment.convertToEntity(registerCommentRequest, lostCatPost);
        lostCatPost.getComments().add(comment);
        commentRepository.save(comment);

        NotificationDto notificationDto = NotificationDto.builder()
                .commentId(comment.getId())
                .postId(lostCatPostId)
                .message("게시글에 댓글이 달렸습니다.")
                .isRead(false)
                .receiverLoginId(lostCatPost.getUser().getLoginId())
                .createdAt(LocalDateTime.now())
                .build();
        produceTopic(notificationDto);
        return RegisterCommentResponse.convertToResponse(comment);
    }

    // 댓글 수정
    @Transactional
    @Override
    public UpdateCommentResponse updateLostCatPostComment(UpdateCommentRequest updateCommentRequest, Long lostCatPostCommentId){
        Comment comment = commentRepository.findById(lostCatPostCommentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        comment.update(updateCommentRequest);

        return UpdateCommentResponse.convertToResponse(comment);
    }

    // 댓글 삭제
    @Transactional
    @Override
    public void deleteLostCatPostComment(Long lostCatPostCommentId){
        if(!commentRepository.existsById(lostCatPostCommentId)){
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }
        commentRepository.deleteById(lostCatPostCommentId);
    }

    public void produceTopic(NotificationDto notificationDto){

        notificationSender.send("comment-notification", NotificationDto.toKafkaDto(notificationDto));
    }
}
