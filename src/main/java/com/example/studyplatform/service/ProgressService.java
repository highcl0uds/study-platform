package com.example.studyplatform.service;

import com.example.studyplatform.dto.CourseProgressResponse;
import com.example.studyplatform.entity.Lesson;
import com.example.studyplatform.entity.LessonProgress;
import com.example.studyplatform.entity.User;
import com.example.studyplatform.exception.BusinessRuleException;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.repository.EnrollmentRepository;
import com.example.studyplatform.repository.LessonProgressRepository;
import com.example.studyplatform.repository.LessonRepository;
import com.example.studyplatform.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProgressService {

    private final LessonProgressRepository progress;
    private final LessonRepository lessons;
    private final EnrollmentRepository enrollments;
    private final SubmissionRepository submissions;
    private final EntityLookup lookup;
    private final Clock clock;

    public void completeLesson(Long lessonId, Long studentId) {
        Lesson lesson = lookup.lessonWithCourse(lessonId);
        User student = lookup.student(studentId);
        Long courseId = lesson.getModule().getCourse().getId();
        if (!enrollments.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new BusinessRuleException("Студент id=" + studentId + " не записан на курс id=" + courseId);
        }
        if (progress.existsByStudentIdAndLessonId(studentId, lessonId)) {
            throw new ConflictException("Урок id=" + lessonId + " уже отмечен как пройденный");
        }
        LessonProgress entry = new LessonProgress();
        entry.setLesson(lesson);
        entry.setStudent(student);
        entry.setCompletedAt(LocalDateTime.now(clock));
        progress.save(entry);
        log.info("Студент id={} прошёл урок id={}", studentId, lessonId);
    }

    @Transactional(readOnly = true)
    public CourseProgressResponse courseProgress(Long courseId, Long studentId) {
        lookup.course(courseId);
        lookup.user(studentId);
        long total = lessons.countByModuleCourseId(courseId);
        long done = progress.countByStudentIdAndLessonModuleCourseId(studentId, courseId);
        int percent = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
        return new CourseProgressResponse(courseId, studentId, done, total, percent,
                submissions.averageScore(studentId, courseId));
    }
}
