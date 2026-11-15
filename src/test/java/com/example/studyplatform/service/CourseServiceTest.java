package com.example.studyplatform.service;

import com.example.studyplatform.entity.Course;
import com.example.studyplatform.entity.EnrollmentStatus;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.exception.ResourceNotFoundException;
import com.example.studyplatform.repository.CourseRepository;
import com.example.studyplatform.repository.EnrollmentRepository;
import com.example.studyplatform.repository.ModuleRepository;
import com.example.studyplatform.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseRepository courses;
    @Mock private ModuleRepository modules;
    @Mock private TagRepository tags;
    @Mock private EnrollmentRepository enrollments;
    @Mock private EntityLookup lookup;

    private CourseService service;

    @BeforeEach
    void setUp() {
        service = new CourseService(courses, modules, tags, enrollments, lookup);
    }

    @Test
    void deleteIsForbiddenWhileThereAreActiveEnrollments() {
        Course course = new Course();
        when(lookup.course(1L)).thenReturn(course);
        when(enrollments.existsByCourseIdAndStatus(1L, EnrollmentStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ConflictException.class);
        verify(courses, never()).delete(course);
    }

    @Test
    void deleteRemovesCourseWithoutActiveEnrollments() {
        Course course = new Course();
        when(lookup.course(1L)).thenReturn(course);
        when(enrollments.existsByCourseIdAndStatus(1L, EnrollmentStatus.ACTIVE)).thenReturn(false);

        service.delete(1L);

        verify(courses).delete(course);
    }

    @Test
    void getOfUnknownCourseIsNotFound() {
        when(courses.findWithModulesById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
