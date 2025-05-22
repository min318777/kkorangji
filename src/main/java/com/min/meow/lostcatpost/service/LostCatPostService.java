package com.min.meow.lostcatpost.service;

import com.min.meow.global.exception.CustomException;
import com.min.meow.global.exception.ErrorCode;
import com.min.meow.lostcatpost.domain.dto.CreateLostCatPostDto;
import com.min.meow.lostcatpost.domain.dto.GetLostCatPostDto;
import com.min.meow.lostcatpost.domain.dto.UpdateLostCatPostDto;
import com.min.meow.lostcatpost.entity.LostCatPost;
import com.min.meow.lostcatpost.domain.request.CreateLostCatPostRequest;
import com.min.meow.lostcatpost.domain.request.UpdateLostCatPostRequest;
import com.min.meow.lostcatpost.repository.LostCatRepository;
import com.min.meow.user.config.PrincipalUser;
import com.min.meow.user.entity.User;
import com.min.meow.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LostCatPostService {

    private final LostCatRepository lostCatRepository;
    private final UserRepository userRepository;
    // 글 전체 조회
    public Page<UpdateLostCatPostDto> getAllLostCatPosts(Pageable pageable){

        return lostCatRepository.findAll(pageable).map(UpdateLostCatPostDto::convertToDto);
    }

    // 글 상세 조회
    public GetLostCatPostDto getLostCatPost(Long lostCatPostId){

        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        return GetLostCatPostDto.convertToDto(lostCatPost);
    }

    // 글 생성
    public CreateLostCatPostDto createLostCatPost(CreateLostCatPostRequest createLostCatPostRequest, User user){
        User writer = userRepository.findByLoginId(user.getLoginId());
        LostCatPost lostCatPost = LostCatPost.convertToEntity(createLostCatPostRequest, writer);
        lostCatRepository.save(lostCatPost);

        return CreateLostCatPostDto.convertToDto(lostCatPost, writer);
    }
    
    // 글 수정
    @Transactional
    public UpdateLostCatPostDto updateLostCatPost(Long lostCatPostId, UpdateLostCatPostRequest updateLostCatPostRequest){

        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        lostCatPost.update(updateLostCatPostRequest);
        return UpdateLostCatPostDto.convertToDto(lostCatPost);
    }

    // 글 삭제
    // 성능개선-> findById 이후 delete를 db호출 2번발생-> deleteById 한번의 호출로 성능개성 -> existById도 있는데? -> 존재여부만 확인하므로 엔티티 조회보다 가벼운 호출이다. -> query dsl로 해볼까?
    // 물리적삭제 대신 소프트삭제도 고려해보자.
    @Transactional
    public void deleteLostCatPost(Long lostCatPostId) {
        /*
        LostCatPost lostCatPost = lostCatRepository.findById(lostCatPostId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        lostCatRepository.delete(lostCatPost);
        */
        if (!lostCatRepository.existsById(lostCatPostId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }
        lostCatRepository.deleteById(lostCatPostId);
    }

}
