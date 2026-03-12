package com.min.meow.post.search.controller;

import com.min.meow.global.ApiResponse;
import com.min.meow.post.search.domain.PostDto;
import com.min.meow.post.search.domain.request.PostSearchRequest;
import com.min.meow.post.search.service.PostSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "검색", description = "게시글 통합 검색 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meow")
public class PostSearchController {

    private final PostSearchService postSearchService;

    @Operation(summary = "게시글 검색",
            description = "제목, 내용, 작성자 등의 조건으로 게시글을 검색합니다. 인증 불필요.")
    @SecurityRequirements
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<PostDto>>> search(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam (defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam (defaultValue = "10") int size,
            @RequestBody PostSearchRequest postSearchRequest){
        Pageable pageable = PageRequest.of(page, size);
        Page<PostDto> posts = postSearchService.search(postSearchRequest, pageable);
        return ResponseEntity.ok(ApiResponse.success("검색 성공", posts));
    }
}
