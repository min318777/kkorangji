package com.min.meow.lostcatpost.controller;


import com.min.meow.global.PageResponse;
import com.min.meow.global.ResponseDto;
import com.min.meow.lostcatpost.domain.dto.CreateLostCatPostDto;
import com.min.meow.lostcatpost.domain.dto.GetLostCatPostDto;
import com.min.meow.lostcatpost.domain.dto.UpdateLostCatPostDto;
import com.min.meow.lostcatpost.domain.request.CreateLostCatPostRequest;
import com.min.meow.lostcatpost.domain.request.UpdateLostCatPostRequest;
import com.min.meow.lostcatpost.entity.LostCatPost;
import com.min.meow.lostcatpost.repository.LostCatRepository;
import com.min.meow.lostcatpost.service.LostCatPostService;
import com.min.meow.user.config.PrincipalUser;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/meow/lost-cat")
public class LostCatPostController {

    private final LostCatPostService lostCatPostService;
    private final LostCatRepository lostCatRepository;

    // 모든 게시물 조회
    @GetMapping
    public ResponseEntity<?> getAllLostCatPosts(@RequestParam (defaultValue = "0") int page,
                                                @RequestParam (defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UpdateLostCatPostDto> posts = lostCatPostService.getAllLostCatPosts(pageable);
        PageResponse<UpdateLostCatPostDto> pageResponse = PageResponse.from(posts);

        return ResponseEntity.ok(new ResponseDto<>(true, "모든 글 조회 성공", pageResponse));

        //n+1 해결->페치 조인 하지만 페이징안됨
        //return ResponseEntity.ok(new ResponseDto<>(true, "모든 글 조회 성공", lostCatRepository.findAllFetch(pageable).stream().map(UpdateLostCatPostDto::convertToDto)));
    }

    // 게시물 상세 조회
    @GetMapping("/{lostCatPostId}")
    public ResponseEntity<?> getLostCatPostDetail(@PathVariable Long lostCatPostId){

        GetLostCatPostDto lostCatPostDto = lostCatPostService.getLostCatPost(lostCatPostId);
        return ResponseEntity.ok(new ResponseDto<>(true, "상세 조회 성공", lostCatPostDto));
    }

    // 글 생성
    @PostMapping("/create")
    public ResponseEntity<?> createLostCatPost(@RequestBody @Valid CreateLostCatPostRequest createLostCatPostRequest,
                                               BindingResult bindingResult, @AuthenticationPrincipal PrincipalUser user){

        CreateLostCatPostDto post = lostCatPostService.createLostCatPost(createLostCatPostRequest, user.getUser());

        return ResponseEntity.ok(new ResponseDto<>(true, "글 생성 성공", post));
    }

    // 글 수정
    @PutMapping("/update/{lostCatPostId}")
    public ResponseEntity<?> updateLostCatPost(@PathVariable Long lostCatPostId,
                                               @RequestBody @Valid UpdateLostCatPostRequest updateLostCatPostRequest,
                                               BindingResult bindingResult, PrincipalUser user){

        UpdateLostCatPostDto post = lostCatPostService.updateLostCatPost(lostCatPostId, updateLostCatPostRequest);
        return ResponseEntity.ok(new ResponseDto<>(true, "글 수정 성공", post));

    }

    // 글 삭제
    @DeleteMapping("/delete/{lostCatPostId}")
    public ResponseEntity<?> deleteLostCatPost(@PathVariable Long lostCatPostId){

        lostCatPostService.deleteLostCatPost(lostCatPostId);

        return ResponseEntity.ok(new ResponseDto<>(true, "글 삭제 성공", null));
    }


}
