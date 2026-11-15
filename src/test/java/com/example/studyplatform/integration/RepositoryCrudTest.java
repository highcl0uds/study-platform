package com.example.studyplatform.integration;

import com.example.studyplatform.entity.Category;
import com.example.studyplatform.entity.Course;
import com.example.studyplatform.entity.Lesson;
import com.example.studyplatform.entity.Module;
import com.example.studyplatform.entity.Profile;
import com.example.studyplatform.entity.Role;
import com.example.studyplatform.entity.Tag;
import com.example.studyplatform.entity.User;
import com.example.studyplatform.repository.CategoryRepository;
import com.example.studyplatform.repository.CourseRepository;
import com.example.studyplatform.repository.ModuleRepository;
import com.example.studyplatform.repository.ProfileRepository;
import com.example.studyplatform.repository.TagRepository;
import com.example.studyplatform.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryCrudTest {

    @Autowired private UserRepository users;
    @Autowired private ProfileRepository profiles;
    @Autowired private CategoryRepository categories;
    @Autowired private CourseRepository courses;
    @Autowired private ModuleRepository modules;
    @Autowired private TagRepository tags;
    @Autowired private EntityManager em;

    private User teacher;
    private Category category;

    @BeforeEach
    void setUp() {
        teacher = users.save(user("teacher@repo.test", Role.TEACHER));
        category = categories.save(category("Repo category"));
    }

    @Test
    void createsAndReadsUserWithOneToOneProfile() {
        User student = user("student@repo.test", Role.STUDENT);
        Profile profile = new Profile();
        profile.setBio("bio");
        student.attachProfile(profile);

        users.saveAndFlush(student);
        em.clear();

        User loaded = users.findWithProfileById(student.getId()).orElseThrow();
        assertThat(loaded.getProfile().getBio()).isEqualTo("bio");
        assertThat(profiles.findByUserId(student.getId())).isPresent();
    }

    @Test
    void emailMustBeUnique() {
        users.saveAndFlush(user("dup@repo.test", Role.STUDENT));

        assertThatThrownBy(() -> users.saveAndFlush(user("dup@repo.test", Role.STUDENT)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savingCourseCascadesToModulesAndLessons() {
        Course course = course("Cascade course");
        Module module = module("M1", 1);
        module.addLesson(lesson("L1"));
        module.addLesson(lesson("L2"));
        course.addModule(module);

        courses.saveAndFlush(course);
        em.clear();

        Course loaded = courses.findWithModulesById(course.getId()).orElseThrow();
        assertThat(loaded.getModules()).hasSize(1);
        assertThat(modules.findWithLessonsByCourseId(course.getId()).get(0).getLessons()).hasSize(2);
    }

    @Test
    void updatesCourseDescription() {
        Course course = courses.saveAndFlush(course("Before"));

        course.setDescription("After");
        courses.saveAndFlush(course);
        em.clear();

        assertThat(courses.findById(course.getId()).orElseThrow().getDescription()).isEqualTo("After");
    }

    @Test
    void deletingCourseCascadesToModules() {
        Course course = course("To delete");
        course.addModule(module("M", 1));
        courses.saveAndFlush(course);
        Long moduleId = course.getModules().get(0).getId();

        courses.delete(course);
        courses.flush();

        assertThat(modules.existsById(moduleId)).isFalse();
    }

    @Test
    void orphanRemovalDeletesModuleRemovedFromCourse() {
        Course course = course("Orphans");
        Module module = module("M", 1);
        course.addModule(module);
        courses.saveAndFlush(course);

        course.removeModule(module);
        courses.saveAndFlush(course);

        assertThat(modules.existsById(module.getId())).isFalse();
    }

    @Test
    void manyToManyTagsDoNotCascadeOnCourseDelete() {
        Tag tag = new Tag();
        tag.setName("Hibernate-" + System.nanoTime());
        tags.save(tag);
        Course course = course("Tagged");
        course.addTag(tag);
        courses.saveAndFlush(course);
        em.clear();

        assertThat(courses.findByTagName(tag.getName())).hasSize(1);

        courses.deleteById(course.getId());
        courses.flush();

        assertThat(tags.findById(tag.getId())).isPresent();
    }

    @Test
    void findsCoursesByCategoryAndTeacher() {
        courses.save(course("B course"));
        courses.save(course("A course"));
        courses.flush();

        List<Course> byCategory = courses.findByCategoryIdOrderByTitle(category.getId());
        assertThat(byCategory).extracting(Course::getTitle).containsExactly("A course", "B course");
        assertThat(courses.findByTeacherIdOrderByTitle(teacher.getId())).hasSize(2);
        assertThat(courses.existsByCategoryId(category.getId())).isTrue();
    }

    private User user(String email, Role role) {
        User user = new User();
        user.setName("User");
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private Category category(String name) {
        Category c = new Category();
        c.setName(name);
        return c;
    }

    private Course course(String title) {
        Course course = new Course();
        course.setTitle(title);
        course.setStartDate(LocalDate.now());
        course.setCategory(category);
        course.setTeacher(teacher);
        return course;
    }

    private Module module(String title, int order) {
        Module module = new Module();
        module.setTitle(title);
        module.setOrderIndex(order);
        return module;
    }

    private Lesson lesson(String title) {
        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        return lesson;
    }
}
