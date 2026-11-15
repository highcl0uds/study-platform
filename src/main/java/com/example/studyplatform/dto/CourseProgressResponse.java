package com.example.studyplatform.dto;

public record CourseProgressResponse(Long courseId, Long studentId, long completedLessons, long totalLessons,
                                     int percent, Double averageScore) {
}
