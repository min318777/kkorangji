package com.min.meow.post.service;

import com.min.meow.global.PageResponse;
import com.min.meow.post.dto.request.CreateBoastCatPostRequest;
import com.min.meow.post.dto.request.UpdateBoastCatPostRequest;
import com.min.meow.post.dto.response.GetBoastCatPostResponse;
import com.min.meow.post.dto.response.CreateBoastCatPostResponse;
import com.min.meow.post.dto.response.UpdateBoastCatPostResponse;
import org.springframework.data.domain.Pageable;

public interface BoastCatPostService {

    PageResponse<GetBoastCatPostResponse> getAllBoastCatPosts(Pageable pageable);

    GetBoastCatPostResponse getBoastCatPost(Long boastCatPostId);

    CreateBoastCatPostResponse createBoastCatPost(CreateBoastCatPostRequest createBoastCatPostRequest, String loginId);

    UpdateBoastCatPostResponse updateBoastCatPost(UpdateBoastCatPostRequest updateBoastCatPostRequest,Long boastCatPostId, String loginId);

    void deleteBoastCatPost(Long boastCatPostId, String loginId, String password);
}
