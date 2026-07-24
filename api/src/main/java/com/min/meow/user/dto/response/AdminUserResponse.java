package com.min.meow.user.dto.response;

import com.min.meow.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class AdminUserResponse {

    private Long userId;
    private String loginId;
    private String nickname;
    private Set<String> roles;    // 변경 후 역할 목록
    private boolean isRestricted; // ROLE_RESTRICTED 보유 여부
    private boolean isDelete;
    private LocalDateTime registeredAt;

    public static AdminUserResponse from(User user) {
        Set<String> roleNames = user.getRoleNames();
        return AdminUserResponse.builder()
                .userId(user.getId())
                .loginId(user.getLoginId())
                .nickname(user.getNickname())
                .roles(roleNames)
                .isRestricted(roleNames.contains("ROLE_RESTRICTED"))
                .isDelete(user.isDelete())
                .registeredAt(user.getRegisteredAt())
                .build();
    }
}
