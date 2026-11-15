package com.example.studyplatform.service;

import com.example.studyplatform.dto.CourseDetails;
import com.example.studyplatform.dto.CourseRequest;
import com.example.studyplatform.dto.CourseSummary;
import com.example.studyplatform.dto.ModuleRequest;
import com.example.studyplatform.entity.Course;
import com.example.studyplatform.entity.EnrollmentStatus;
import com.example.studyplatform.entity.Module;
import com.example.studyplatform.entity.Tag;
import com.example.studyplatform.exception.BusinessRuleException;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.exception.ResourceNotFoundException;
import com.example.studyplatform.mapper.CourseMapper;
import com.example.studyplatform.repository.CourseRepository;
import com.example.studyplatform.repository.EnrollmentRepository;
import com.example.studyplatform.repository.ModuleRepository;
import com.example.studyplatform.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseService {

    private final CourseRepository courses;
    private final ModuleRepository modules;
    private final TagRepository tags;
    private final EnrollmentRepository enrollments;
    private final EntityLookup lookup;

    public CourseDetails create(CourseRequest request) {
        Course course = new Course();
        applyBasics(course, request);
        if (request.modules() != null) {
            checkUniqueOrder(request.modules());
            request.modules().forEach(m -> course.addModule(ContentBuilder.module(m)));
        }
        courses.save(course);
        log.info("Создан курс id={} '{}' (модулей: {})", course.getId(), course.getTitle(), course.getModules().size());
        return toDetails(course.getId());
    }

    @Transactional(readOnly = true)
    public CourseDetails get(Long id) {
        return toDetails(id);
    }

    @Transactional(readOnly = true)
    public List<CourseSummary> list(Long categoryId, String tag, String query) {
        List<Course> found;
        if (categoryId != null) {
            found = courses.findByCategoryIdOrderByTitle(categoryId);
        } else if (tag != null && !tag.isBlank()) {
            found = courses.findByTagName(tag);
        } else if (query != null && !query.isBlank()) {
            found = courses.findByTitleContainingIgnoreCaseOrderByTitle(query);
        } else {
            found = courses.findAllWithDetails();
        }
        return found.stream().map(CourseMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<CourseSummary> listByTeacher(Long teacherId) {
        lookup.user(teacherId);
        return courses.findByTeacherIdOrderByTitle(teacherId).stream().map(CourseMapper::toSummary).toList();
    }

    public CourseDetails update(Long id, CourseRequest request) {
        Course course = courses.findWithTagsById(id).orElseThrow(() -> new ResourceNotFoundException("Курс", id));
        applyBasics(course, request);
        log.info("Обновлён курс id={}", id);
        return toDetails(id);
    }

    public void delete(Long id) {
        Course course = lookup.course(id);
        if (enrollments.existsByCourseIdAndStatus(id, EnrollmentStatus.ACTIVE)) {
            throw new ConflictException("Нельзя удалить курс id=" + id + ": есть активные записи студентов");
        }
        courses.delete(course);
        log.info("Удалён курс id={}", id);
    }

    private void applyBasics(Course course, CourseRequest request) {
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setDuration(request.duration());
        course.setStartDate(request.startDate());
        course.setCategory(lookup.category(request.categoryId()));
        course.setTeacher(lookup.teacher(request.teacherId()));
        if (request.tagIds() != null) {
            course.getTags().clear();
            List<Tag> found = tags.findByIdIn(request.tagIds());
            if (found.size() != request.tagIds().size()) {
                throw new ResourceNotFoundException("Один или несколько тегов из " + request.tagIds() + " не найдены");
            }
            found.forEach(course::addTag);
        }
    }

    private void checkUniqueOrder(List<ModuleRequest> requests) {
        Set<Integer> seen = new HashSet<>();
        for (ModuleRequest m : requests) {
            if (!seen.add(m.orderIndex())) {
                throw new BusinessRuleException("Порядковые номера модулей должны быть уникальны: " + m.orderIndex());
            }
        }
    }

    private CourseDetails toDetails(Long id) {
        Course course = courses.findWithModulesById(id).orElseThrow(() -> new ResourceNotFoundException("Курс", id));
        List<Module> withLessons = modules.findWithLessonsByCourseId(id);
        return new CourseDetails(CourseMapper.toSummary(course), CourseMapper.toResponses(withLessons));
    }
}
