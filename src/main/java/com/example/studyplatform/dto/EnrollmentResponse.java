package com.example.studyplatform.dto;

import com.example.studyplatform.entity.EnrollmentStatus;

import java.time.LocalDate;

public record EnrollmentResponse(Long id, Long studentId, String studentName, Long courseId,
                                 String courseTitle, LocalDate enrollDate, EnrollmentStatus status) {
}
