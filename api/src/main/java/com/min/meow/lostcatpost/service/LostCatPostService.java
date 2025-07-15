package com.min.meow.lostcatpost.service;


import com.min.meow.lostcatpost.domain.dto.CreateLostCatPostResponse;
import com.min.meow.lostcatpost.domain.dto.GetLostCatPostResponse;
import com.min.meow.lostcatpost.domain.dto.UpdateLostCatPostResponse;
import com.min.meow.lostcatpost.domain.request.CreateLostCatPostRequest;
import com.min.meow.lostcatpost.domain.request.UpdateLostCatPostRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LostCatPostService {

    Page<UpdateLostCatPostResponse> getAllLostCatPosts(Pageable pageable);

    GetLostCatPostResponse getLostCatPost(Long lostCatPostId);

    CreateLostCatPostResponse createLostCatPost(CreateLostCatPostRequest createLostCatPostRequest, String loginId);

    UpdateLostCatPostResponse updateLostCatPost(Long lostCatPostId, UpdateLostCatPostRequest updateLostCatPostRequest, String loginId);

    void deleteLostCatPost(Long lostCatPostId, String loginId, String password);
}
