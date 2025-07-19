package com.min.meow.post.comment.service.impl;


import com.min.kafka.producer.NotificationSender;
import com.min.meow.post.comment.domain.response.RegisterCommentDto;
import com.min.meow.post.comment.domain.response.UpdateCommentDto;
import com.min.meow.post.comment.domain.request.RegisterCommentRequest;
import com.min.meow.post.comment.domain.request.UpdateCommentRequest;
import com.min.meow.post.comment.entity.Comment;
import com.min.meow.post.comment.repository.CommentRepository;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.kafka.producer.KafkaMessageProducer;
import com.min.meow.post.comment.service.CommentService;
import com.min.meow.post.lostcatpost.entity.LostCatPost;
import com.min.meow.post.lostcatpost.repository.LostCatRepository;
import com.min.meow.notification.NotificationDto;
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
    private final KafkaMessageProducer kafkaMessageProducer;
    private final NotificationSender notificationSender;

    // 댓글 작성
    @Transactional
    @Override
    public RegisterCommentDto registerLostCatPostComment(RegisterCommentRequest registerCommentRequest, Long lostCatPostId){
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
        return RegisterCommentDto.convertToDto(comment);
    }

    // 댓글 수정
    @Transactional
    @Override
    public UpdateCommentDto updateLostCatPostComment(UpdateCommentRequest updateCommentRequest, Long lostCatPostCommentId){
        Comment comment = commentRepository.findById(lostCatPostCommentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        comment.update(updateCommentRequest);

        return UpdateCommentDto.convertToDto(comment);
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

        notificationSender.send("lost-cat-topic", NotificationDto.toKafkaDto(notificationDto));
    }
}
