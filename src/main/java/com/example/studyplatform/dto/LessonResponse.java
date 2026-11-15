package com.example.studyplatform.dto;

public record LessonResponse(Long id, Long moduleId, String title, String content, String videoUrl,
                             Integer orderIndex) {
}
