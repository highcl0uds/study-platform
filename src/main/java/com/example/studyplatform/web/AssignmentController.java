package com.example.studyplatform.web;

import com.example.studyplatform.dto.AssignmentRequest;
import com.example.studyplatform.dto.AssignmentResponse;
import com.example.studyplatform.dto.GradeRequest;
import com.example.studyplatform.dto.SubmissionRequest;
import com.example.studyplatform.dto.SubmissionResponse;
import com.example.studyplatform.service.AssignmentService;
import com.example.studyplatform.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignments;
    private final SubmissionService submissions;

    @GetMapping("/assignments/{id}")
    public AssignmentResponse get(@PathVariable Long id) {
        return assignments.get(id);
    }

    @PutMapping("/assignments/{id}")
    public AssignmentResponse update(@PathVariable Long id, @Valid @RequestBody AssignmentRequest request) {
        return assignments.update(id, request);
    }

    @DeleteMapping("/assignments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assignments.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assignments/{id}/submit")
    public ResponseEntity<SubmissionResponse> submit(@PathVariable Long id,
                                                     @Valid @RequestBody SubmissionRequest request) {
        SubmissionResponse created = submissions.submit(id, request);
        return Responses.created("/api/submissions", created.id(), created);
    }

    @GetMapping("/assignments/{id}/submissions")
    public List<SubmissionResponse> submissions(@PathVariable Long id) {
        return submissions.listByAssignment(id);
    }

    @GetMapping("/submissions/{id}")
    public SubmissionResponse getSubmission(@PathVariable Long id) {
        return submissions.get(id);
    }

    @PutMapping("/submissions/{id}/grade")
    public SubmissionResponse grade(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
        return submissions.grade(id, request);
    }
}
