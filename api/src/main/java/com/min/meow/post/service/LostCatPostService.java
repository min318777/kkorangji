package com.min.meow.post.service;


import com.min.meow.global.PageResponse;
import com.min.meow.post.domain.response.CreateLostCatPostResponse;
import com.min.meow.post.domain.response.GetLostCatPostResponse;
import com.min.meow.post.domain.response.UpdateLostCatPostResponse;
import com.min.meow.post.domain.request.CreateLostCatPostRequest;
import com.min.meow.post.domain.request.UpdateLostCatPostRequest;
import org.springframework.data.domain.Pageable;

public interface LostCatPostService {

    PageResponse<GetLostCatPostResponse> getAllLostCatPosts(Pageable pageable);

    GetLostCatPostResponse getLostCatPost(Long lostCatPostId);

    CreateLostCatPostResponse createLostCatPost(CreateLostCatPostRequest createLostCatPostRequest, String loginId);

    UpdateLostCatPostResponse updateLostCatPost(Long lostCatPostId, UpdateLostCatPostRequest updateLostCatPostRequest, String loginId);

    void deleteLostCatPost(Long lostCatPostId, String loginId, String password);
}
