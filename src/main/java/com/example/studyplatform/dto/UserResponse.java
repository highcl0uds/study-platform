package com.example.studyplatform.dto;

import com.example.studyplatform.entity.Role;

public record UserResponse(Long id, String name, String email, Role role, ProfileResponse profile) {
}
