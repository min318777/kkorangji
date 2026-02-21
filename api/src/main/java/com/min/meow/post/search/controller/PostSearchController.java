package com.min.meow.post.search.controller;
import com.min.meow.global.ApiResponse;
import com.min.meow.post.search.domain.PostDto;
import com.min.meow.post.search.domain.request.PostSearchRequest;
import com.min.meow.post.search.service.impl.PostSearchServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        Page<PostDto> posts = postSearchServiceImpl.search(postSearchRequest, pageable);
        return ResponseEntity.ok(ApiResponse.success("검색 성공", posts));
    }
}
