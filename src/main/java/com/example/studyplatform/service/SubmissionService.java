package com.example.studyplatform.service;

import com.example.studyplatform.dto.GradeRequest;
import com.example.studyplatform.dto.SubmissionRequest;
import com.example.studyplatform.dto.SubmissionResponse;
import com.example.studyplatform.entity.Assignment;
import com.example.studyplatform.entity.Submission;
import com.example.studyplatform.entity.SubmissionStatus;
import com.example.studyplatform.entity.User;
import com.example.studyplatform.exception.BusinessRuleException;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.exception.ResourceNotFoundException;
import com.example.studyplatform.mapper.LearningMapper;
import com.example.studyplatform.repository.EnrollmentRepository;
import com.example.studyplatform.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubmissionService {

    private final SubmissionRepository submissions;
    private final EnrollmentRepository enrollments;
    private final EntityLookup lookup;
    private final Clock clock;

    public SubmissionResponse submit(Long assignmentId, SubmissionRequest request) {
        Assignment assignment = lookup.assignmentWithCourse(assignmentId);
        User student = lookup.student(request.studentId());
        Long courseId = assignment.getLesson().getModule().getCourse().getId();
        if (!enrollments.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new BusinessRuleException("Студент id=" + student.getId() + " не записан на курс id=" + courseId);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (assignment.getDueDate() != null && now.isAfter(assignment.getDueDate())) {
            throw new BusinessRuleException("Срок сдачи задания id=" + assignmentId + " истёк");
        }
        if (submissions.existsByStudentIdAndAssignmentId(student.getId(), assignmentId)) {
            throw new ConflictException("Студент id=" + student.getId() + " уже сдал задание id=" + assignmentId);
        }
        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setContent(request.content());
        submission.setSubmittedAt(now);
        submissions.save(submission);
        log.info("Студент id={} сдал задание id={}", student.getId(), assignmentId);
        return LearningMapper.toResponse(submission);
    }

    public SubmissionResponse grade(Long submissionId, GradeRequest request) {
        Submission submission = submissions.findWithDetailsById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Решение", submissionId));
        int max = submission.getAssignment().getMaxScore();
        if (request.score() > max) {
            throw new BusinessRuleException("Оценка " + request.score() + " превышает максимум " + max);
        }
        submission.setScore(request.score());
        submission.setFeedback(request.feedback());
        submission.setStatus(SubmissionStatus.GRADED);
        log.info("Решение id={} оценено: {}/{}", submissionId, request.score(), max);
        return LearningMapper.toResponse(submission);
    }

    @Transactional(readOnly = true)
    public SubmissionResponse get(Long id) {
        return LearningMapper.toResponse(submissions.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Решение", id)));
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> listByAssignment(Long assignmentId) {
        lookup.assignment(assignmentId);
        return submissions.findByAssignmentIdOrderBySubmittedAt(assignmentId).stream()
                .map(LearningMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> listByStudent(Long studentId) {
        lookup.user(studentId);
        return submissions.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
                .map(LearningMapper::toResponse).toList();
    }
}
