package com.example.studyplatform.service;

import com.example.studyplatform.dto.ReviewRequest;
import com.example.studyplatform.dto.ReviewResponse;
import com.example.studyplatform.entity.Course;
import com.example.studyplatform.entity.CourseReview;
import com.example.studyplatform.entity.User;
import com.example.studyplatform.exception.BusinessRuleException;
import com.example.studyplatform.exception.ConflictException;
import com.example.studyplatform.exception.ResourceNotFoundException;
import com.example.studyplatform.mapper.LearningMapper;
import com.example.studyplatform.repository.CourseReviewRepository;
import com.example.studyplatform.repository.EnrollmentRepository;
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
public class ReviewService {

    private final CourseReviewRepository reviews;
    private final EnrollmentRepository enrollments;
    private final EntityLookup lookup;
    private final Clock clock;

    public ReviewResponse add(Long courseId, ReviewRequest request) {
        Course course = lookup.course(courseId);
        User student = lookup.student(request.studentId());
        if (!enrollments.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new BusinessRuleException("Оставить отзыв может только записанный студент");
        }
        if (reviews.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new ConflictException("Студент id=" + student.getId() + " уже оставил отзыв на курс id=" + courseId);
        }
        CourseReview review = new CourseReview();
        review.setCourse(course);
        review.setStudent(student);
        review.setRating(request.rating());
        review.setComment(request.comment());
        review.setCreatedAt(LocalDateTime.now(clock));
        reviews.save(review);
        log.info("Студент id={} оставил отзыв на курс id={}", student.getId(), courseId);
        return LearningMapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listByCourse(Long courseId) {
        lookup.course(courseId);
        return reviews.findByCourseIdOrderByCreatedAtDesc(courseId).stream().map(LearningMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Double averageRating(Long courseId) {
        lookup.course(courseId);
        return reviews.averageRating(courseId);
    }

    public void delete(Long reviewId) {
        CourseReview review = reviews.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Отзыв", reviewId));
        reviews.delete(review);
    }
}
