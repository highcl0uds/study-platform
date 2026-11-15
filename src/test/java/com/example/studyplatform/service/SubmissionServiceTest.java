package com.example.studyplatform.service;

import com.example.studyplatform.dto.GradeRequest;
import com.example.studyplatform.dto.SubmissionRequest;
import com.example.studyplatform.entity.Assignment;
import com.example.studyplatform.entity.Course;
import com.example.studyplatform.entity.Lesson;
import com.example.studyplatform.entity.Module;
import com.example.studyplatform.entity.Role;
import com.example.studyplatform.entity.Submission;
import com.example.studyplatform.entity.SubmissionStatus;
import com.example.studyplatform.entity.User;
import com.example.studyplatform.exception.BusinessRuleException;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.repository.EnrollmentRepository;
import com.example.studyplatform.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubmissionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 10, 12, 0);

    @Mock private SubmissionRepository submissions;
    @Mock private EnrollmentRepository enrollments;
    @Mock private EntityLookup lookup;

    private SubmissionService service;
    private Assignment assignment;
    private User student;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new SubmissionService(submissions, enrollments, lookup, clock);

        Course course = new Course();
        ReflectionTestUtils.setField(course, "id", 10L);
        Module module = new Module();
        module.setCourse(course);
        Lesson lesson = new Lesson();
        lesson.setModule(module);
        assignment = new Assignment();
        ReflectionTestUtils.setField(assignment, "id", 5L);
        assignment.setTitle("HW");
        assignment.setLesson(lesson);
        assignment.setMaxScore(100);
        assignment.setDueDate(NOW.plusDays(1));

        student = new User();
        ReflectionTestUtils.setField(student, "id", 7L);
        student.setName("Bob");
        student.setRole(Role.STUDENT);
        when(lookup.assignmentWithCourse(5L)).thenReturn(assignment);
        when(lookup.student(7L)).thenReturn(student);
    }

    @Test
    void savesSubmissionWithCurrentTime() {
        when(enrollments.existsByStudentIdAndCourseId(7L, 10L)).thenReturn(true);
        when(submissions.existsByStudentIdAndAssignmentId(7L, 5L)).thenReturn(false);

        var response = service.submit(5L, new SubmissionRequest(7L, "answer"));

        assertThat(response.submittedAt()).isEqualTo(NOW);
        assertThat(response.status()).isEqualTo(SubmissionStatus.SUBMITTED);
        verify(submissions).save(any(Submission.class));
    }

    @Test
    void rejectsSecondSubmission() {
        when(enrollments.existsByStudentIdAndCourseId(7L, 10L)).thenReturn(true);
        when(submissions.existsByStudentIdAndAssignmentId(7L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.submit(5L, new SubmissionRequest(7L, "answer")))
                .isInstanceOf(ConflictException.class);
        verify(submissions, never()).save(any());
    }

    @Test
    void rejectsSubmissionAfterDeadline() {
        when(enrollments.existsByStudentIdAndCourseId(7L, 10L)).thenReturn(true);
        assignment.setDueDate(NOW.minusMinutes(1));

        assertThatThrownBy(() -> service.submit(5L, new SubmissionRequest(7L, "late")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("истёк");
    }

    @Test
    void rejectsSubmissionFromNotEnrolledStudent() {
        when(enrollments.existsByStudentIdAndCourseId(7L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> service.submit(5L, new SubmissionRequest(7L, "x")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void gradeWritesScoreFeedbackAndStatus() {
        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setContent("c");
        submission.setSubmittedAt(NOW);
        when(submissions.findWithDetailsById(1L)).thenReturn(Optional.of(submission));

        var response = service.grade(1L, new GradeRequest(80, "nice"));

        assertThat(response.score()).isEqualTo(80);
        assertThat(submission.getFeedback()).isEqualTo("nice");
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.GRADED);
    }
}
