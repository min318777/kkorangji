package com.min.meow.user.service;

import com.min.meow.global.PostType;
import com.min.meow.user.dto.reponse.MyCommentListResponse;
import com.min.meow.user.dto.reponse.MyPageSummaryResponse;
import com.min.meow.user.dto.reponse.MyPostListResponse;
import org.springframework.data.domain.Pageable;


public interface MyPageService {

    MyPageSummaryResponse getMyPageSummary(String loginId);
    MyPostListResponse getMyPosts(String loginId, Pageable pageable, PostType type);
    MyCommentListResponse getMyComments(Long userId, Pageable pageable);
}