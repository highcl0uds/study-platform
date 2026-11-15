package com.example.studyplatform.dto;

import java.time.LocalDateTime;

public record QuizResultResponse(Long id, Long quizId, String quizTitle, Long studentId, String studentName,
                                 int score, int maxScore, int percent, boolean passed, LocalDateTime takenAt) {
}
