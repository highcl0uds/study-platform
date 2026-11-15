package com.example.studyplatform.web;

import com.example.studyplatform.dto.AssignmentRequest;
import com.example.studyplatform.dto.AssignmentResponse;
import com.example.studyplatform.dto.LessonRequest;
import com.example.studyplatform.dto.LessonResponse;
import com.example.studyplatform.dto.ModuleRequest;
import com.example.studyplatform.dto.ModuleResponse;
import com.example.studyplatform.dto.QuizRequest;
import com.example.studyplatform.dto.QuizResponse;
import com.example.studyplatform.service.AssignmentService;
import com.example.studyplatform.service.CourseContentService;
import com.example.studyplatform.service.ProgressService;
import com.example.studyplatform.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContentController {

    private final CourseContentService content;
    private final AssignmentService assignments;
    private final QuizService quizzes;
    private final ProgressService progress;

    @GetMapping("/modules/{id}")
    public ModuleResponse getModule(@PathVariable Long id) {
        return content.getModule(id);
    }

    @PutMapping("/modules/{id}")
    public ModuleResponse updateModule(@PathVariable Long id, @Valid @RequestBody ModuleRequest request) {
        return content.updateModule(id, request);
    }

    @DeleteMapping("/modules/{id}")
    public ResponseEntity<Void> deleteModule(@PathVariable Long id) {
        content.deleteModule(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/modules/{id}/lessons")
    public ResponseEntity<LessonResponse> addLesson(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        LessonResponse created = content.addLesson(id, request);
        return Responses.created("/api/lessons", created.id(), created);
    }

    @PostMapping("/modules/{id}/quiz")
    public ResponseEntity<QuizResponse> createQuiz(@PathVariable Long id, @Valid @RequestBody QuizRequest request) {
        QuizResponse created = quizzes.create(id, request);
        return Responses.created("/api/quizzes", created.id(), created);
    }

    @GetMapping("/modules/{id}/quiz")
    public QuizResponse moduleQuiz(@PathVariable Long id) {
        return quizzes.getByModule(id);
    }

    @GetMapping("/lessons/{id}")
    public LessonResponse getLesson(@PathVariable Long id) {
        return content.getLesson(id);
    }

    @PutMapping("/lessons/{id}")
    public LessonResponse updateLesson(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        return content.updateLesson(id, request);
    }

    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id) {
        content.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lessons/{id}/complete")
    public ResponseEntity<Void> completeLesson(@PathVariable Long id, @RequestParam("user") Long userId) {
        progress.completeLesson(id, userId);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/lessons/{id}/assignments")
    public ResponseEntity<AssignmentResponse> createAssignment(@PathVariable Long id,
                                                               @Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse created = assignments.create(id, request);
        return Responses.created("/api/assignments", created.id(), created);
    }

    @GetMapping("/lessons/{id}/assignments")
    public List<AssignmentResponse> lessonAssignments(@PathVariable Long id) {
        return assignments.listByLesson(id);
    }
}
