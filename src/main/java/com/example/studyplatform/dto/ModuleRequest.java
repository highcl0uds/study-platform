package com.example.studyplatform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ModuleRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull @Min(0) Integer orderIndex,
        @Size(max = 2000) String description,
        @Valid List<LessonRequest> lessons) {
}
