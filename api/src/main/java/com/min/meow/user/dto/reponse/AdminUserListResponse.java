package com.min.meow.user.dto.reponse;

import com.min.meow.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class AdminUserListResponse {

    private Long userId;
    private String loginId;
    private String nickname;
    private Set<String> roles;
    private boolean isRestricted;
    private boolean isDelete;
    private LocalDateTime registeredAt;

    public static AdminUserListResponse from(User user) {
        Set<String> roleNames = user.getRoleNames();
        return AdminUserListResponse.builder()
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
