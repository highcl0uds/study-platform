package com.example.studyplatform.dto;

import java.time.LocalDate;
import java.util.List;

public record CourseSummary(
        Long id,
        String title,
        String description,
        Integer duration,
        LocalDate startDate,
        String category,
        Long teacherId,
        String teacherName,
        List<String> tags) {
}
