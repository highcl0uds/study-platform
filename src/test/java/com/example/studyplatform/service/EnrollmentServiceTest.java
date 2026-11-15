package com.example.studyplatform.service;

import com.example.studyplatform.entity.Course;
import com.example.studyplatform.entity.Enrollment;
import com.example.studyplatform.entity.Role;
import com.example.studyplatform.entity.User;
import com.example.studyplatform.exception.BusinessRuleException;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.exception.ResourceNotFoundException;
import com.example.studyplatform.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock private EnrollmentRepository enrollments;
    @Mock private EntityLookup lookup;
    @InjectMocks private EnrollmentService service;

    private Course course;
    private User student;

    @BeforeEach
    void setUp() {
        course = new Course();
        ReflectionTestUtils.setField(course, "id", 1L);
        course.setTitle("Hibernate");
        student = new User();
        ReflectionTestUtils.setField(student, "id", 2L);
        student.setName("Alice");
        student.setRole(Role.STUDENT);
    }

    @Test
    void enrollsStudentWhenNotYetEnrolled() {
        when(lookup.course(1L)).thenReturn(course);
        when(lookup.student(2L)).thenReturn(student);
        when(enrollments.existsByStudentIdAndCourseId(2L, 1L)).thenReturn(false);

        var response = service.enrollStudent(1L, 2L);

        assertThat(response.studentName()).isEqualTo("Alice");
        assertThat(response.courseTitle()).isEqualTo("Hibernate");
        verify(enrollments).save(any(Enrollment.class));
    }

    @Test
    void rejectsDuplicateEnrollment() {
        when(lookup.course(1L)).thenReturn(course);
        when(lookup.student(2L)).thenReturn(student);
        when(enrollments.existsByStudentIdAndCourseId(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.enrollStudent(1L, 2L)).isInstanceOf(ConflictException.class);
        verify(enrollments, never()).save(any());
    }

    @Test
    void propagatesRoleViolation() {
        when(lookup.course(1L)).thenReturn(course);
        when(lookup.student(2L)).thenThrow(new BusinessRuleException("not a student"));

        assertThatThrownBy(() -> service.enrollStudent(1L, 2L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void unenrollOfMissingEnrollmentIsNotFound() {
        when(enrollments.findByStudentIdAndCourseId(2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unenrollStudent(1L, 2L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
