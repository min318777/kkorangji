package com.min.meow.boastcatpost.controller;


import com.min.meow.boastcatpost.domain.dto.BoastCatPostResponse;
import com.min.meow.boastcatpost.domain.dto.CreateBoastCatPostResponse;
import com.min.meow.boastcatpost.domain.dto.UpdateBoastCatPostResponse;
import com.min.meow.boastcatpost.domain.request.CreateBoastCatPostRequest;
import com.min.meow.boastcatpost.domain.request.UpdateBoastCatPostRequest;
import com.min.meow.boastcatpost.service.BoastCatPostService;
import com.min.meow.global.PageResponse;
import com.min.meow.global.exception.ErrorResponse;
import com.min.meow.config.PrincipalUser;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/meow/boast-cat")
public class BoastCatPostController {
    private final BoastCatPostService boastCatPostService;

    // 모든 글 조회
    @GetMapping
    public ResponseEntity<?> getAllBoastCatPost(@RequestParam (defaultValue = "0") int page,
                                                @RequestParam (defaultValue = "10") int size ){

        Pageable pageable = PageRequest.of(page, size);
        Page<BoastCatPostResponse> posts = boastCatPostService.getAllBoastCatPosts(pageable);
        PageResponse<BoastCatPostResponse> pageResponse = PageResponse.from(posts);

        return ResponseEntity.ok(new ErrorResponse<>(true, "모든 글 조회 성공", pageResponse));
    }
    // 글 생성
    @PostMapping
    public ResponseEntity<?> createBoastCatPost(@RequestBody @Valid CreateBoastCatPostRequest createBoastCatPostRequest, @AuthenticationPrincipal PrincipalUser user){

        CreateBoastCatPostResponse post = boastCatPostService.createBoastCatPost(createBoastCatPostRequest, user.getUser().getLoginId());
        return ResponseEntity.ok(new ErrorResponse<>(true, "글 생성 성공", post));
    }

    // 글 수정
    @PutMapping("/{boastCatPostId}")
    public ResponseEntity<?> updateBoastCatPost(@RequestBody @Valid UpdateBoastCatPostRequest updateBoastCatPostRequest, @PathVariable Long boastCatPostId, @AuthenticationPrincipal PrincipalUser user){

        UpdateBoastCatPostResponse post = boastCatPostService.updateBoastCatPost(updateBoastCatPostRequest, boastCatPostId, user.getUser().getLoginId());
        return ResponseEntity.ok(new ErrorResponse<>(true, "글 수정 성공", post));
    }

    // 글 삭제
    @DeleteMapping("/{boastCatPostId}")
    public ResponseEntity<?> deleteBoastCatPost(@PathVariable Long boastCatPostId, @AuthenticationPrincipal PrincipalUser user){

        boastCatPostService.deleteBoastCatPost(boastCatPostId, user.getUser().getLoginId(), user.getUser().getPassword());
        return ResponseEntity.ok(new ErrorResponse<>(true, "글 삭제 성공", null));
    }
}
