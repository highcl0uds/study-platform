package com.example.studyplatform.service;

import com.example.studyplatform.dto.AssignmentRequest;
import com.example.studyplatform.dto.AssignmentResponse;
import com.example.studyplatform.entity.Assignment;
import com.example.studyplatform.entity.Lesson;
import com.example.studyplatform.mapper.CourseMapper;
import com.example.studyplatform.repository.AssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignments;
    private final EntityLookup lookup;

    public AssignmentResponse create(Long lessonId, AssignmentRequest request) {
        Lesson lesson = lookup.lesson(lessonId);
        Assignment assignment = new Assignment();
        apply(assignment, request);
        assignment.setLesson(lesson);
        assignments.save(assignment);
        log.info("К уроку id={} добавлено задание id={}", lessonId, assignment.getId());
        return CourseMapper.toResponse(assignment);
    }

    @Transactional(readOnly = true)
    public AssignmentResponse get(Long id) {
        return CourseMapper.toResponse(lookup.assignment(id));
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listByLesson(Long lessonId) {
        lookup.lesson(lessonId);
        return assignments.findByLessonIdOrderById(lessonId).stream().map(CourseMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listByCourse(Long courseId) {
        lookup.course(courseId);
        return assignments.findByLessonModuleCourseIdOrderById(courseId).stream()
                .map(CourseMapper::toResponse).toList();
    }

    public AssignmentResponse update(Long id, AssignmentRequest request) {
        Assignment assignment = lookup.assignment(id);
        apply(assignment, request);
        return CourseMapper.toResponse(assignment);
    }

    public void delete(Long id) {
        assignments.delete(lookup.assignment(id));
        log.info("Удалено задание id={}", id);
    }

    private void apply(Assignment assignment, AssignmentRequest request) {
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());
        if (request.maxScore() != null) {
            assignment.setMaxScore(request.maxScore());
        }
    }
}
