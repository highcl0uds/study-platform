package com.example.studyplatform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record CourseRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @Positive Integer duration,
        LocalDate startDate,
        @NotNull Long categoryId,
        @NotNull Long teacherId,
        Set<Long> tagIds,
        @Valid List<ModuleRequest> modules) {
}
