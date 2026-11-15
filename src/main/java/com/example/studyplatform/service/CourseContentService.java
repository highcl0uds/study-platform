package com.example.studyplatform.service;

import com.example.studyplatform.dto.LessonRequest;
import com.example.studyplatform.dto.LessonResponse;
import com.example.studyplatform.dto.ModuleRequest;
import com.example.studyplatform.dto.ModuleResponse;
import com.example.studyplatform.entity.Course;
import com.example.studyplatform.entity.Lesson;
import com.example.studyplatform.entity.Module;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.mapper.CourseMapper;
import com.example.studyplatform.repository.LessonRepository;
import com.example.studyplatform.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseContentService {

    private final ModuleRepository modules;
    private final LessonRepository lessons;
    private final EntityLookup lookup;

    public ModuleResponse addModule(Long courseId, ModuleRequest request) {
        Course course = lookup.course(courseId);
        if (modules.existsByCourseIdAndOrderIndex(courseId, request.orderIndex())) {
            throw new ConflictException("В курсе уже есть модуль с номером " + request.orderIndex());
        }
        Module module = ContentBuilder.module(request);
        module.setCourse(course);
        modules.save(module);
        log.info("В курс id={} добавлен модуль id={}", courseId, module.getId());
        return CourseMapper.toResponse(module, true);
    }

    @Transactional(readOnly = true)
    public List<ModuleResponse> listModules(Long courseId) {
        lookup.course(courseId);
        return CourseMapper.toResponses(modules.findWithLessonsByCourseId(courseId));
    }

    @Transactional(readOnly = true)
    public ModuleResponse getModule(Long id) {
        return CourseMapper.toResponse(lookup.module(id), true);
    }

    public ModuleResponse updateModule(Long id, ModuleRequest request) {
        Module module = lookup.module(id);
        if (modules.existsByCourseIdAndOrderIndexAndIdNot(module.getCourse().getId(), request.orderIndex(), id)) {
            throw new ConflictException("В курсе уже есть модуль с номером " + request.orderIndex());
        }
        ContentBuilder.applyModule(module, request);
        return CourseMapper.toResponse(module, true);
    }

    public void deleteModule(Long id) {
        modules.delete(lookup.module(id));
        log.info("Удалён модуль id={}", id);
    }

    public LessonResponse addLesson(Long moduleId, LessonRequest request) {
        Module module = lookup.module(moduleId);
        Lesson lesson = new Lesson();
        ContentBuilder.applyLesson(lesson, request, lessons.findByModuleIdOrderByOrderIndex(moduleId).size());
        lesson.setModule(module);
        lessons.save(lesson);
        log.info("В модуль id={} добавлен урок id={}", moduleId, lesson.getId());
        return CourseMapper.toResponse(lesson);
    }

    @Transactional(readOnly = true)
    public LessonResponse getLesson(Long id) {
        return CourseMapper.toResponse(lookup.lesson(id));
    }

    public LessonResponse updateLesson(Long id, LessonRequest request) {
        Lesson lesson = lookup.lesson(id);
        ContentBuilder.applyLesson(lesson, request, lesson.getOrderIndex());
        return CourseMapper.toResponse(lesson);
    }

    public void deleteLesson(Long id) {
        lessons.delete(lookup.lesson(id));
        log.info("Удалён урок id={}", id);
    }
}
