package com.min.meow.user.service;

import com.min.meow.global.PostType;
import com.min.meow.user.domain.reponse.MyCommentListResponse;
import com.min.meow.user.domain.reponse.MyPageSummaryResponse;
import com.min.meow.user.domain.reponse.MyPostListResponse;
import org.springframework.data.domain.Pageable;


public interface MyPageService {

    MyPageSummaryResponse getMyPageSummary(String loginId);
    MyPostListResponse getMyPosts(String loginId, Pageable pageable, PostType type);
    MyCommentListResponse getMyComments(String loginId, Pageable pageable);
}