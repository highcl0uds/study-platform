package com.example.studyplatform.dto;

import java.time.LocalDateTime;

public record ReviewResponse(Long id, Long courseId, Long studentId, String studentName, int rating,
                             String comment, LocalDateTime createdAt) {
}
