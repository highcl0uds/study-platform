package com.example.studyplatform.dto;

import java.util.List;

public record ModuleResponse(Long id, String title, Integer orderIndex, String description,
                             List<LessonResponse> lessons) {
}
