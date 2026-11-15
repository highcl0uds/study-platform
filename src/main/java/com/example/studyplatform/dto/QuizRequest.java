package com.example.studyplatform.dto;

import com.example.studyplatform.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuizRequest(
        @NotBlank @Size(max = 200) String title,
        @Positive Integer timeLimit,
        @Min(1) @Max(100) Integer passingPercent,
        @NotEmpty @Valid List<QuestionData> questions) {

    public record QuestionData(
            @NotBlank @Size(max = 1000) String text,
            QuestionType type,
            @NotEmpty @Valid List<OptionData> options) {
    }

    public record OptionData(@NotBlank @Size(max = 500) String text, boolean correct) {
    }
}
