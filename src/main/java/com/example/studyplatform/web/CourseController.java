package com.example.studyplatform.web;

import com.example.studyplatform.dto.AssignmentResponse;
import com.example.studyplatform.dto.CourseDetails;
import com.example.studyplatform.dto.CourseProgressResponse;
import com.example.studyplatform.dto.CourseRequest;
import com.example.studyplatform.dto.CourseSummary;
import com.example.studyplatform.dto.EnrollmentResponse;
import com.example.studyplatform.dto.ModuleRequest;
import com.example.studyplatform.dto.ModuleResponse;
import com.example.studyplatform.dto.QuizResultResponse;
import com.example.studyplatform.dto.ReviewRequest;
import com.example.studyplatform.dto.ReviewResponse;
import com.example.studyplatform.service.AssignmentService;
import com.example.studyplatform.service.CourseContentService;
import com.example.studyplatform.service.CourseService;
import com.example.studyplatform.service.EnrollmentService;
import com.example.studyplatform.service.ProgressService;
import com.example.studyplatform.service.QuizService;
import com.example.studyplatform.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courses;
    private final CourseContentService content;
    private final EnrollmentService enrollments;
    private final AssignmentService assignments;
    private final QuizService quizzes;
    private final ReviewService reviews;
    private final ProgressService progress;

    @PostMapping
    public ResponseEntity<CourseDetails> create(@Valid @RequestBody CourseRequest request) {
        CourseDetails created = courses.create(request);
        return Responses.created("/api/courses", created.course().id(), created);
    }

    @GetMapping
    public List<CourseSummary> list(@RequestParam(required = false) Long categoryId,
                                    @RequestParam(required = false) String tag,
                                    @RequestParam(required = false, name = "q") String query) {
        return courses.list(categoryId, tag, query);
    }

    @GetMapping("/{id}")
    public CourseDetails get(@PathVariable Long id) {
        return courses.get(id);
    }

    @PutMapping("/{id}")
    public CourseDetails update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return courses.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courses.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-teacher/{teacherId}")
    public List<CourseSummary> byTeacher(@PathVariable Long teacherId) {
        return courses.listByTeacher(teacherId);
    }

    @GetMapping("/{id}/modules")
    public List<ModuleResponse> modules(@PathVariable Long id) {
        return content.listModules(id);
    }

    @PostMapping("/{id}/modules")
    public ResponseEntity<ModuleResponse> addModule(@PathVariable Long id, @Valid @RequestBody ModuleRequest request) {
        ModuleResponse created = content.addModule(id, request);
        return Responses.created("/api/modules", created.id(), created);
    }

    @GetMapping("/{id}/assignments")
    public List<AssignmentResponse> assignments(@PathVariable Long id) {
        return assignments.listByCourse(id);
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<EnrollmentResponse> enroll(@PathVariable Long id, @RequestParam("user") Long userId) {
        EnrollmentResponse created = enrollments.enrollStudent(id, userId);
        return ResponseEntity.status(201).body(created);
    }

    @DeleteMapping("/{id}/enroll")
    public ResponseEntity<Void> unenroll(@PathVariable Long id, @RequestParam("user") Long userId) {
        enrollments.unenrollStudent(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    public EnrollmentResponse complete(@PathVariable Long id, @RequestParam("user") Long userId) {
        return enrollments.complete(id, userId);
    }

    @GetMapping("/{id}/students")
    public List<EnrollmentResponse> students(@PathVariable Long id) {
        return enrollments.listByCourse(id);
    }

    @GetMapping("/{id}/progress")
    public CourseProgressResponse progress(@PathVariable Long id, @RequestParam("user") Long userId) {
        return progress.courseProgress(id, userId);
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewResponse> addReview(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(201).body(reviews.add(id, request));
    }

    @GetMapping("/{id}/reviews")
    public List<ReviewResponse> reviews(@PathVariable Long id) {
        return reviews.listByCourse(id);
    }

    @GetMapping("/{id}/rating")
    public Map<String, Double> rating(@PathVariable Long id) {
        Double average = reviews.averageRating(id);
        return Map.of("average", average == null ? 0.0 : average);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviews.delete(reviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/quiz-results")
    public List<QuizResultResponse> quizResults(@PathVariable Long id) {
        return quizzes.resultsByCourse(id);
    }
}
