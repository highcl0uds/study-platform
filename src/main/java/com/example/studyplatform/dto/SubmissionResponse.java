package com.example.studyplatform.dto;

import com.example.studyplatform.entity.SubmissionStatus;

import java.time.LocalDateTime;

public record SubmissionResponse(Long id, Long assignmentId, String assignmentTitle, Long studentId,
                                 String studentName, LocalDateTime submittedAt, String content,
                                 Integer score, String feedback, SubmissionStatus status) {
}
