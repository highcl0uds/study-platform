package com.example.studyplatform.integration;

import com.example.studyplatform.dto.CourseDetails;
import com.example.studyplatform.dto.CourseRequest;
import com.example.studyplatform.dto.LessonRequest;
import com.example.studyplatform.dto.ModuleRequest;
import com.example.studyplatform.entity.Course;
import com.example.studyplatform.entity.Role;
import com.example.studyplatform.repository.CourseRepository;
import com.example.studyplatform.repository.EnrollmentRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Демонстрация ленивой загрузки: тесты намеренно НЕ транзакционные, чтобы сессия Hibernate закрывалась сразу
 * после вызова репозитория (spring.jpa.open-in-view=false).
 */
class LazyLoadingTest extends IntegrationTestBase {

    @Autowired private CourseRepository courses;
    @Autowired private EnrollmentRepository enrollments;
    @Autowired private TransactionTemplate tx;
    @Autowired private EntityManagerFactory emf;

    @Test
    void lazyCollectionOutsideTransactionThrowsLazyInitializationException() {
        Long courseId = newCourse(newUser(Role.TEACHER).id()).course().id();

        Course course = courses.findById(courseId).orElseThrow();   

        assertThat(Hibernate.isInitialized(course.getModules())).isFalse();
        assertThatThrownBy(() -> course.getModules().size()).isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void lazyManyToOneIsAProxyAndFailsOutsideTransaction() {
        Long courseId = newCourse(newUser(Role.TEACHER).id()).course().id();

        Course course = courses.findById(courseId).orElseThrow();

        assertThat(Hibernate.isInitialized(course.getTeacher())).isFalse();
        assertThat(course.getTeacher().getId()).isNotNull();          // id доступен без инициализации
        assertThatThrownBy(() -> course.getTeacher().getName()).isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void joinFetchLoadsCollectionBeforeSessionCloses() {
        Long courseId = newCourse(newUser(Role.TEACHER).id()).course().id();

        Course course = courses.findWithModulesById(courseId).orElseThrow();

        assertThat(course.getModules()).hasSize(2);                   
    }

    @Test
    void hibernateInitializeInsideTransactionLoadsCollection() {
        Long courseId = newCourse(newUser(Role.TEACHER).id()).course().id();

        Course course = tx.execute(status -> {
            Course c = courses.findById(courseId).orElseThrow();
            Hibernate.initialize(c.getModules());
            return c;
        });

        assertThat(course.getModules()).hasSize(2);
    }

    @Test
    void entityGraphLoadsAssociationsInRepositoryQuery() {
        Long teacherId = newUser(Role.TEACHER).id();
        newCourse(teacherId);

        Course course = courses.findByTeacherIdOrderByTitle(teacherId).get(0);

        assertThat(course.getCategory().getName()).startsWith("Category");   
        assertThat(course.getTeacher().getId()).isEqualTo(teacherId);
    }

    @Test
    void serviceReturnsFullyLoadedDtoWithoutLazyErrors() {
        CourseDetails created = newCourse(newUser(Role.TEACHER).id());

        CourseDetails details = courseService.get(created.course().id());

        assertThat(details.modules()).hasSize(2);
        assertThat(details.modules().get(0).lessons()).hasSize(2);
        assertThat(details.modules().get(1).lessons()).hasSize(1);
    }

    @Test
    void courseStructureIsLoadedWithConstantNumberOfQueries() {
        Long teacherId = newUser(Role.TEACHER).id();
        Long smallCourse = newCourse(teacherId).course().id();
        Long bigCourse = courseWithModules(teacherId, 6);

        long small = countStatements(smallCourse);
        long big = countStatements(bigCourse);

        assertThat(big).isEqualTo(small);
    }

    private Long courseWithModules(Long teacherId, int count) {
        List<ModuleRequest> modules = IntStream.rangeClosed(1, count)
                .mapToObj(i -> new ModuleRequest("Module " + i, i, null,
                        List.of(new LessonRequest("Lesson " + i, "text", null, 1))))
                .toList();
        return courseService.create(new CourseRequest("Big " + next(), null, null, null,
                newCategory(), teacherId, null, modules)).course().id();
    }

    private long countStatements(Long courseId) {
        Statistics statistics = emf.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        courseService.get(courseId);
        return statistics.getPrepareStatementCount();
    }

    @Test
    void entityGraphLoadsStudentOfEnrollment() {
        var student = newUser(Role.STUDENT);
        Long courseId = newCourse(newUser(Role.TEACHER).id()).course().id();
        enrollmentService.enrollStudent(courseId, student.id());

        var found = enrollments.findByCourseIdOrderByEnrollDateAsc(courseId);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getStudent().getName()).startsWith("STUDENT");   
    }
}
