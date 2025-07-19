package com.min.meow.post.lostcatpost.controller;


import com.min.meow.global.PageResponse;
import com.min.meow.global.exception.ApiResponse;
import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.post.lostcatpost.domain.dto.CreateLostCatPostResponse;
import com.min.meow.post.lostcatpost.domain.dto.GetLostCatPostResponse;
import com.min.meow.post.lostcatpost.domain.dto.UpdateLostCatPostResponse;
import com.min.meow.post.lostcatpost.domain.request.CreateLostCatPostRequest;
import com.min.meow.post.lostcatpost.domain.request.UpdateLostCatPostRequest;
import com.min.meow.post.lostcatpost.service.LostCatPostService;
import com.min.meow.config.PrincipalUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/meow/lost-cat")
public class LostCatPostController {

    private final LostCatPostService lostCatPostService;

    // 모든 게시물 조회
    @GetMapping
    public ResponseEntity<PageResponse<UpdateLostCatPostResponse>> getAllLostCatPosts(@RequestParam (defaultValue = "0") int page,
                                                                                      @RequestParam (defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UpdateLostCatPostResponse> posts = lostCatPostService.getAllLostCatPosts(pageable);
        PageResponse<UpdateLostCatPostResponse> pageResponse = PageResponse.from(posts);

        return ResponseEntity.ok(pageResponse);

        //n+1 해결->페치 조인 하지만 페이징안됨
        //return ResponseEntity.ok(new ResponseDto<>(true, "모든 글 조회 성공", lostCatRepository.findAllFetch(pageable).stream().map(UpdateLostCatPostDto::convertToDto)));
    }

    // 글 상세 조회
    @GetMapping("/{lostCatPostId}")
    public ResponseEntity<ApiResponse<GetLostCatPostResponse>> getLostCatPostDetail(@PathVariable Long lostCatPostId){

        GetLostCatPostResponse lostCatPostDto = lostCatPostService.getLostCatPost(lostCatPostId);
        return ResponseEntity.ok(new ApiResponse<GetLostCatPostResponse>(HttpStatus.OK, "글 조회 성공", lostCatPostDto));
    }

    // 글 생성
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CreateLostCatPostResponse>> createLostCatPost(@RequestBody @Valid CreateLostCatPostRequest createLostCatPostRequest,
                                                                       BindingResult bindingResult, @AuthenticationPrincipal PrincipalUser user){

        if(user == null && user.getUser() == null){
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        CreateLostCatPostResponse lostCatPostDto = lostCatPostService.createLostCatPost(createLostCatPostRequest, user.getUser().getLoginId());

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "글 생성 성공", lostCatPostDto));
    }

    // 글 수정
    @PutMapping("/update/{lostCatPostId}")
    public ResponseEntity<ApiResponse<UpdateLostCatPostResponse>> updateLostCatPost(@PathVariable Long lostCatPostId,
                                                                       @RequestBody @Valid UpdateLostCatPostRequest updateLostCatPostRequest,
                                                                       BindingResult bindingResult, @AuthenticationPrincipal PrincipalUser user){

        UpdateLostCatPostResponse lostCatPostDto = lostCatPostService.updateLostCatPost(lostCatPostId, updateLostCatPostRequest, user.getUser().getLoginId());
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "글 수정 성공", lostCatPostDto));

    }

    // 글 삭제
    @DeleteMapping("/delete/{lostCatPostId}")
    public ResponseEntity<ApiResponse<Void>> deleteLostCatPost(@PathVariable Long lostCatPostId, @AuthenticationPrincipal PrincipalUser user){

        lostCatPostService.deleteLostCatPost(lostCatPostId, user.getUser().getLoginId(), user.getUser().getPassword());

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "글 삭제 성공", null));
    }


}
