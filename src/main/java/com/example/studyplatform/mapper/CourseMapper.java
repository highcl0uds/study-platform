package com.example.studyplatform.mapper;

import com.example.studyplatform.dto.AssignmentResponse;
import com.example.studyplatform.dto.CourseSummary;
import com.example.studyplatform.dto.LessonResponse;
import com.example.studyplatform.dto.ModuleResponse;
import com.example.studyplatform.dto.NameResponse;
import com.example.studyplatform.entity.Assignment;
import com.example.studyplatform.entity.Category;
import com.example.studyplatform.entity.Course;
import com.example.studyplatform.entity.Lesson;
import com.example.studyplatform.entity.Module;
import com.example.studyplatform.entity.Tag;

import java.util.Comparator;
import java.util.List;

/** Методы обращаются к ленивым связям: только внутри транзакции. */
public final class CourseMapper {

    private CourseMapper() {
    }

    public static NameResponse toResponse(Category category) {
        return new NameResponse(category.getId(), category.getName());
    }

    public static NameResponse toResponse(Tag tag) {
        return new NameResponse(tag.getId(), tag.getName());
    }

    public static CourseSummary toSummary(Course course) {
        List<String> tags = course.getTags().stream().map(Tag::getName).sorted().toList();
        return new CourseSummary(course.getId(), course.getTitle(), course.getDescription(),
                course.getDuration(), course.getStartDate(), course.getCategory().getName(),
                course.getTeacher().getId(), course.getTeacher().getName(), tags);
    }

    public static ModuleResponse toResponse(Module module, boolean withLessons) {
        List<LessonResponse> lessons = withLessons
                ? module.getLessons().stream().map(CourseMapper::toResponse).toList()
                : null;
        return new ModuleResponse(module.getId(), module.getTitle(), module.getOrderIndex(),
                module.getDescription(), lessons);
    }

    public static LessonResponse toResponse(Lesson lesson) {
        return new LessonResponse(lesson.getId(), lesson.getModule().getId(), lesson.getTitle(),
                lesson.getContent(), lesson.getVideoUrl(), lesson.getOrderIndex());
    }

    public static AssignmentResponse toResponse(Assignment assignment) {
        return new AssignmentResponse(assignment.getId(), assignment.getLesson().getId(),
                assignment.getTitle(), assignment.getDescription(), assignment.getDueDate(),
                assignment.getMaxScore());
    }

    public static List<ModuleResponse> toResponses(List<Module> modules) {
        return modules.stream()
                .sorted(Comparator.comparing(Module::getOrderIndex))
                .map(m -> toResponse(m, true))
                .toList();
    }
}
