package com.example.studyplatform.web;

import com.example.studyplatform.dto.EnrollmentResponse;
import com.example.studyplatform.dto.QuizResultResponse;
import com.example.studyplatform.dto.SubmissionResponse;
import com.example.studyplatform.dto.UserRequest;
import com.example.studyplatform.dto.UserResponse;
import com.example.studyplatform.entity.Role;
import com.example.studyplatform.service.EnrollmentService;
import com.example.studyplatform.service.QuizService;
import com.example.studyplatform.service.SubmissionService;
import com.example.studyplatform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService users;
    private final EnrollmentService enrollments;
    private final SubmissionService submissions;
    private final QuizService quizzes;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        UserResponse created = users.create(request);
        return Responses.created("/api/users", created.id(), created);
    }

    @GetMapping
    public List<UserResponse> list(@RequestParam(required = false) Role role) {
        return users.list(role);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return users.get(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return users.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        users.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/enrollments")
    public List<EnrollmentResponse> enrollments(@PathVariable Long id) {
        return enrollments.listByStudent(id);
    }

    @GetMapping("/{id}/assignments")
    public List<SubmissionResponse> assignments(@PathVariable Long id) {
        return submissions.listByStudent(id);
    }

    @GetMapping("/{id}/quiz-results")
    public List<QuizResultResponse> quizResults(@PathVariable Long id) {
        return quizzes.resultsByStudent(id);
    }
}
