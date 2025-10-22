package com.min.meow.post.comment.service.impl;


import com.min.kafka.producer.NotificationSender;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.notification.domain.NotificationDto;
import com.min.meow.post.comment.domain.request.RegisterCommentRequest;
import com.min.meow.post.comment.domain.request.UpdateCommentRequest;
import com.min.meow.post.comment.domain.response.GetCommentResponse;
import com.min.meow.post.comment.domain.response.RegisterCommentResponse;
import com.min.meow.post.comment.domain.response.UpdateCommentResponse;
import com.min.meow.post.comment.entity.Comment;
import com.min.meow.post.comment.repository.CommentRepository;
import com.min.meow.post.comment.service.CommentService;
import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.repository.BoastCatPostRepository;
import com.min.meow.post.repository.LostCatRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final LostCatRepository lostCatRepository;
    private final BoastCatPostRepository boastCatPostRepository;
    private final CommentRepository commentRepository;
    private final NotificationSender notificationSender;


    // 고양이 자랑 게시글 댓글 조회
    @Override
    public List<GetCommentResponse> getBoastCatPostComment(Long boastCatPostId) {

        List<Comment> comments = commentRepository.findByBoastCatPostIdWithUser(boastCatPostId);

        return comments.stream()
                .map(GetCommentResponse::toResponse)
                .collect(Collectors.toList());
    }

    // 실종 고양이 게시글 댓글 조회
    @Override
    public List<GetCommentResponse> getLostCatPostComment(Long lostCatPostId) {

        List<Comment> comments = commentRepository.findByLostCatPostIdWithUser(lostCatPostId);

        return comments.stream()
                .map(GetCommentResponse::toResponse)
                .collect(Collectors.toList());
    }

    // 고양이 자랑 게시글 댓글 작성
    @Transactional
    @Override
    public RegisterCommentResponse registerBoastCatPostComment(RegisterCommentRequest registerCommentRequest, Long boastCatPostId, String writer){
        BoastCatPost boastCatPost = boastCatPostRepository.findById(boastCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        Comment comment = Comment.convertToEntity(registerCommentRequest, boastCatPost, writer);
        boastCatPost.getComments().add(comment);
        commentRepository.save(comment);

        NotificationDto notificationDto = NotificationDto.builder()
                .commentId(comment.getId())
                .postId(boastCatPostId)
                .message("게시글에 댓글이 달렸습니다.")
                .isRead(false)
                .receiverLoginId(boastCatPost.getUser().getLoginId())
                .createdAt(LocalDateTime.now())
                .build();
        produceTopic(notificationDto);
        return RegisterCommentResponse.convertToResponse(comment);
    }

    // 실종 고양이 게시글 댓글 작성
    @Transactional
    @Override
    public RegisterCommentResponse registerLostCatPostComment(RegisterCommentRequest registerCommentRequest, Long lostCatPostId, String writer){
        var lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        Comment comment = Comment.convertToEntityForLostCat(registerCommentRequest, lostCatPost, writer);
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
    public UpdateCommentResponse updateComment(UpdateCommentRequest updateCommentRequest, Long commentId){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        comment.update(updateCommentRequest);

        return UpdateCommentResponse.convertToResponse(comment);
    }

    // 댓글 삭제
    @Transactional
    @Override
    public void deleteComment(Long commentId){
        if(!commentRepository.existsById(commentId)){
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }
        commentRepository.deleteById(commentId);
    }

    public void produceTopic(NotificationDto notificationDto){

        notificationSender.send("comment-notification", NotificationDto.toKafkaDto(notificationDto));
    }


}
