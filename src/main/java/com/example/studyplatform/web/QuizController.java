package com.example.studyplatform.web;

import com.example.studyplatform.dto.QuizResponse;
import com.example.studyplatform.dto.QuizResultResponse;
import com.example.studyplatform.dto.TakeQuizRequest;
import com.example.studyplatform.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizzes;

    @GetMapping("/{id}")
    public QuizResponse get(@PathVariable Long id) {
        return quizzes.get(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        quizzes.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/take")
    public ResponseEntity<QuizResultResponse> take(@PathVariable Long id, @Valid @RequestBody TakeQuizRequest request) {
        return ResponseEntity.status(201).body(quizzes.takeQuiz(id, request));
    }

    @GetMapping("/{id}/results")
    public List<QuizResultResponse> results(@PathVariable Long id) {
        return quizzes.resultsByQuiz(id);
    }
}
