package com.example.studyplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AssignmentRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        LocalDateTime dueDate,
        @Positive Integer maxScore) {
}
