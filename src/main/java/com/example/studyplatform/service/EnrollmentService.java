package com.example.studyplatform.service;

import com.example.studyplatform.dto.EnrollmentResponse;
import com.example.studyplatform.entity.Course;
import com.example.studyplatform.entity.Enrollment;
import com.example.studyplatform.entity.EnrollmentStatus;
import com.example.studyplatform.entity.User;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.exception.ResourceNotFoundException;
import com.example.studyplatform.mapper.LearningMapper;
import com.example.studyplatform.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollments;
    private final EntityLookup lookup;

    public EnrollmentResponse enrollStudent(Long courseId, Long studentId) {
        Course course = lookup.course(courseId);
        User student = lookup.student(studentId);
        if (enrollments.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ConflictException("Студент id=" + studentId + " уже записан на курс id=" + courseId);
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrollDate(LocalDate.now());
        enrollments.save(enrollment);
        log.info("Студент id={} записан на курс id={}", studentId, courseId);
        return LearningMapper.toResponse(enrollment);
    }

    public void unenrollStudent(Long courseId, Long studentId) {
        Enrollment enrollment = find(courseId, studentId);
        enrollments.delete(enrollment);
        log.info("Студент id={} отписан от курса id={}", studentId, courseId);
    }

    public EnrollmentResponse complete(Long courseId, Long studentId) {
        Enrollment enrollment = find(courseId, studentId);
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        return LearningMapper.toResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listByCourse(Long courseId) {
        lookup.course(courseId);
        return enrollments.findByCourseIdOrderByEnrollDateAsc(courseId).stream()
                .map(LearningMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listByStudent(Long studentId) {
        lookup.user(studentId);
        return enrollments.findByStudentIdOrderByEnrollDateDesc(studentId).stream()
                .map(LearningMapper::toResponse).toList();
    }

    private Enrollment find(Long courseId, Long studentId) {
        return enrollments.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Запись студента id=" + studentId + " на курс id=" + courseId + " не найдена"));
    }
}
