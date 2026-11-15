package com.example.studyplatform.dto;

import com.example.studyplatform.entity.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotNull Role role,
        @Valid ProfileData profile) {

    public record ProfileData(
            @Size(max = 2000) String bio,
            @Size(max = 500) String avatarUrl,
            @Size(max = 30) String phone) {
    }
}
