package com.min.meow.post.search.controller;


import com.min.meow.global.exception.ApiResponse;
import com.min.meow.post.search.domain.PostDto;
import com.min.meow.post.search.domain.RestPage;
import com.min.meow.post.search.domain.request.PostSearchRequest;
import com.min.meow.post.search.service.impl.PostSearchServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meow")
public class PostSearchController {

    private final PostSearchServiceImpl postSearchServiceImpl;


    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<PostDto>>> search(@RequestParam (defaultValue = "0") int page,
                                                            @RequestParam (defaultValue = "10") int size,
                                                            @RequestBody PostSearchRequest postSearchRequest){
        Pageable pageable = PageRequest.of(page, size);
        RestPage<PostDto> posts = postSearchServiceImpl.search(postSearchRequest, pageable);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "검색 성공", posts));
    }
}
