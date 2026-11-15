package com.example.studyplatform.service;

import com.example.studyplatform.entity.Assignment;
import com.example.studyplatform.entity.Category;
import com.example.studyplatform.entity.Course;
import com.example.studyplatform.entity.Lesson;
import com.example.studyplatform.entity.Module;
import com.example.studyplatform.entity.Role;
import com.example.studyplatform.entity.User;
import com.example.studyplatform.exception.BusinessRuleException;
import com.example.studyplatform.exception.ResourceNotFoundException;
import com.example.studyplatform.repository.AssignmentRepository;
import com.example.studyplatform.repository.CategoryRepository;
import com.example.studyplatform.repository.CourseRepository;
import com.example.studyplatform.repository.LessonRepository;
import com.example.studyplatform.repository.ModuleRepository;
import com.example.studyplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntityLookup {

    private final UserRepository users;
    private final CourseRepository courses;
    private final CategoryRepository categories;
    private final ModuleRepository modules;
    private final LessonRepository lessons;
    private final AssignmentRepository assignments;

    public User user(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Пользователь", id));
    }

    public User student(Long id) {
        return requireRole(user(id), Role.STUDENT);
    }

    public User teacher(Long id) {
        return requireRole(user(id), Role.TEACHER);
    }

    private User requireRole(User user, Role role) {
        if (user.getRole() != role) {
            throw new BusinessRuleException("Пользователь id=" + user.getId() + " не имеет роли " + role);
        }
        return user;
    }

    public Course course(Long id) {
        return courses.findById(id).orElseThrow(() -> new ResourceNotFoundException("Курс", id));
    }

    public Category category(Long id) {
        return categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Категория", id));
    }

    public Module module(Long id) {
        return modules.findById(id).orElseThrow(() -> new ResourceNotFoundException("Модуль", id));
    }

    public Lesson lesson(Long id) {
        return lessons.findById(id).orElseThrow(() -> new ResourceNotFoundException("Урок", id));
    }

    public Lesson lessonWithCourse(Long id) {
        return lessons.findWithCourseById(id).orElseThrow(() -> new ResourceNotFoundException("Урок", id));
    }

    public Assignment assignment(Long id) {
        return assignments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Задание", id));
    }

    public Assignment assignmentWithCourse(Long id) {
        return assignments.findWithCourseById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Задание", id));
    }
}
