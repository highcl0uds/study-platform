package com.example.studyplatform.mapper;

import com.example.studyplatform.dto.ProfileResponse;
import com.example.studyplatform.dto.UserResponse;
import com.example.studyplatform.entity.Profile;
import com.example.studyplatform.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    /** Вызывать внутри транзакции: обращается к ленивому профилю. */
    public static UserResponse toResponse(User user) {
        Profile profile = user.getProfile();
        ProfileResponse profileResponse = profile == null ? null
                : new ProfileResponse(profile.getBio(), profile.getAvatarUrl(), profile.getPhone());
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), profileResponse);
    }
}
