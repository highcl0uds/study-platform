package com.example.studyplatform.dto;

import com.example.studyplatform.entity.QuestionType;

import java.util.List;

public record QuizResponse(Long id, Long moduleId, String title, Integer timeLimit, Integer passingPercent,
                           List<QuestionResponse> questions) {

    public record QuestionResponse(Long id, String text, QuestionType type, List<OptionResponse> options) {
    }

    public record OptionResponse(Long id, String text) {
    }
}
