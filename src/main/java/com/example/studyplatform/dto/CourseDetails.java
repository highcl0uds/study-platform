package com.example.studyplatform.dto;

import java.util.List;

public record CourseDetails(CourseSummary course, List<ModuleResponse> modules) {
}
