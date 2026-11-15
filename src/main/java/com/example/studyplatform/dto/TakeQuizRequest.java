package com.example.studyplatform.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Set;

/** {@code answers}: идентификатор вопроса -> идентификаторы выбранных вариантов. */
public record TakeQuizRequest(@NotNull Long studentId, @NotNull Map<Long, Set<Long>> answers) {
}
