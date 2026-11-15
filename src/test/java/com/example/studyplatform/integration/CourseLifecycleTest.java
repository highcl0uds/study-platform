package com.example.studyplatform.integration;

import com.example.studyplatform.dto.CourseDetails;
import com.example.studyplatform.dto.CourseRequest;
import com.example.studyplatform.dto.LessonRequest;
import com.example.studyplatform.dto.ModuleRequest;
import com.example.studyplatform.dto.NameRequest;
import com.example.studyplatform.entity.Role;
import com.example.studyplatform.exception.BusinessRuleException;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.exception.ResourceNotFoundException;
import com.example.studyplatform.repository.LessonRepository;
import com.example.studyplatform.repository.ModuleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseLifecycleTest extends IntegrationTestBase {

    @Autowired private ModuleRepository modules;
    @Autowired private LessonRepository lessons;

    @Test
    void createsCourseWithModulesAndLessonsInOneCall() {
        CourseDetails course = newCourse(newUser(Role.TEACHER).id());

        assertThat(course.modules()).extracting("title").containsExactly("M1", "M2");
        assertThat(course.modules().get(0).lessons()).extracting("title").containsExactly("L1", "L2");
        assertThat(course.course().teacherName()).startsWith("TEACHER");
    }

    @Test
    void courseCanBeCreatedWithoutStructureAndExtendedLater() {
        var teacher = newUser(Role.TEACHER);
        CourseDetails empty = courseService.create(new CourseRequest("Empty", null, null, null,
                newCategory(), teacher.id(), null, null));
        assertThat(empty.modules()).isEmpty();

        var module = contentService.addModule(empty.course().id(), new ModuleRequest("Late", 1, null, null));
        var lesson = contentService.addLesson(module.id(), new LessonRequest("Late lesson", "x", null, null));

        CourseDetails reloaded = courseService.get(empty.course().id());
        assertThat(reloaded.modules()).hasSize(1);
        assertThat(reloaded.modules().get(0).lessons()).extracting("id").containsExactly(lesson.id());
    }

    @Test
    void onlyTeacherCanOwnCourse() {
        var student = newUser(Role.STUDENT);

        assertThatThrownBy(() -> courseService.create(new CourseRequest("X", null, null, null,
                newCategory(), student.id(), null, null))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void duplicateModuleOrderIsRejected() {
        var teacher = newUser(Role.TEACHER);
        Long categoryId = newCategory();

        assertThatThrownBy(() -> courseService.create(new CourseRequest("Dup", null, null, null,
                categoryId, teacher.id(), null, List.of(new ModuleRequest("A", 1, null, null),
                new ModuleRequest("B", 1, null, null))))).isInstanceOf(BusinessRuleException.class);

        CourseDetails course = newCourse(teacher.id());
        assertThatThrownBy(() -> contentService.addModule(course.course().id(),
                new ModuleRequest("Again", 1, null, null))).isInstanceOf(ConflictException.class);
    }

    @Test
    void updatesCourseAndReplacesTeacher() {
        var teacher = newUser(Role.TEACHER);
        var newTeacher = newUser(Role.TEACHER);
        CourseDetails course = newCourse(teacher.id());
        Long id = course.course().id();

        CourseDetails updated = courseService.update(id, new CourseRequest("Renamed", "New description", 99, null,
                newCategory(), newTeacher.id(), null, null));

        assertThat(updated.course().title()).isEqualTo("Renamed");
        assertThat(updated.course().teacherId()).isEqualTo(newTeacher.id());
        assertThat(updated.modules()).hasSize(2);   
    }

    @Test
    void deletingCourseRemovesModulesAndLessonsByCascade() {
        CourseDetails course = newCourse(newUser(Role.TEACHER).id());
        Long courseId = course.course().id();
        Long moduleId = course.modules().get(0).id();
        Long lessonId = course.modules().get(0).lessons().get(0).id();

        courseService.delete(courseId);

        assertThatThrownBy(() -> courseService.get(courseId)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(modules.existsById(moduleId)).isFalse();
        assertThat(lessons.existsById(lessonId)).isFalse();
    }

    @Test
    void courseWithActiveStudentsCannotBeDeleted() {
        CourseDetails course = newCourse(newUser(Role.TEACHER).id());
        enrollmentService.enrollStudent(course.course().id(), newUser(Role.STUDENT).id());

        assertThatThrownBy(() -> courseService.delete(course.course().id())).isInstanceOf(ConflictException.class);
        assertThat(courseService.get(course.course().id())).isNotNull();
    }

    @Test
    void categoryWithCoursesCannotBeDeletedButEmptyOneCan() {
        CourseDetails course = newCourse(newUser(Role.TEACHER).id());
        Long usedCategory = categoryService.list().stream()
                .filter(c -> c.name().equals(course.course().category())).findFirst().orElseThrow().id();

        assertThatThrownBy(() -> categoryService.delete(usedCategory)).isInstanceOf(ConflictException.class);

        Long emptyCategory = categoryService.create(new NameRequest("Empty " + next())).id();
        categoryService.delete(emptyCategory);
        assertThatThrownBy(() -> categoryService.get(emptyCategory)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void filtersCoursesByCategoryTitleAndTeacher() {
        var teacher = newUser(Role.TEACHER);
        CourseDetails course = newCourse(teacher.id());

        assertThat(courseService.listByTeacher(teacher.id())).hasSize(1);
        assertThat(courseService.list(null, null, course.course().title())).hasSize(1);
        assertThat(courseService.list(null, null, "no-such-title-" + next())).isEmpty();
    }
}
