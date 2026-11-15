package com.example.studyplatform.integration;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SchemaCreationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void modelContainsFifteenToTwentyEntities() {
        Set<String> names = entityManagerFactory.getMetamodel().getEntities().stream()
                .map(EntityType::getName).collect(Collectors.toSet());

        assertThat(names).hasSizeBetween(15, 20).contains("User", "Profile", "Course", "Module", "Lesson",
                "Assignment", "Submission", "Quiz", "Question", "AnswerOption", "QuizSubmission", "Enrollment",
                "CourseReview", "Tag", "Category");
    }

    @Test
    void allTablesAndJoinTableAreCreated() {
        Set<String> tables = tableNames();

        assertThat(tables).contains("users", "profiles", "categories", "courses", "enrollments", "modules",
                "lessons", "assignments", "submissions", "quizzes", "questions", "answer_options",
                "quiz_submissions", "course_reviews", "tags", "course_tag", "lesson_progress", "quiz_answers");
    }

    @Test
    void uniqueConstraintsAreCreated() {
        Set<String> constraints = jdbc.queryForList(
                        "select lower(constraint_name) from information_schema.table_constraints "
                                + "where constraint_type = 'UNIQUE'", String.class)
                .stream().collect(Collectors.toSet());

        assertThat(constraints).contains("uk_enrollment_student_course", "uk_submission_student_assignment",
                "uk_review_student_course", "uk_progress_student_lesson");
    }

    @Test
    void foreignKeysConnectCourseToCategoryAndTeacher() {
        Set<String> fkColumns = jdbc.queryForList(
                        "select lower(column_name) from information_schema.key_column_usage "
                                + "where lower(table_name) = 'courses'", String.class)
                .stream().collect(Collectors.toSet());

        assertThat(fkColumns).contains("category_id", "teacher_id");
    }

    private Set<String> tableNames() {
        return jdbc.queryForList("select lower(table_name) from information_schema.tables "
                + "where lower(table_schema) = 'public'", String.class).stream().collect(Collectors.toSet());
    }
}
