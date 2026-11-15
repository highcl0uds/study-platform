package com.example.studyplatform.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LessonRequest(
        @NotBlank @Size(max = 200) String title,
        String content,
        @Size(max = 500) String videoUrl,
        @Min(0) Integer orderIndex) {
}
