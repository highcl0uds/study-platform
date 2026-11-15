package com.example.studyplatform.web;

import com.example.studyplatform.dto.NameRequest;
import com.example.studyplatform.dto.NameResponse;
import com.example.studyplatform.service.CategoryService;
import com.example.studyplatform.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final CategoryService categories;
    private final TagService tags;

    @PostMapping("/categories")
    public ResponseEntity<NameResponse> createCategory(@Valid @RequestBody NameRequest request) {
        NameResponse created = categories.create(request);
        return Responses.created("/api/categories", created.id(), created);
    }

    @GetMapping("/categories")
    public List<NameResponse> listCategories() {
        return categories.list();
    }

    @GetMapping("/categories/{id}")
    public NameResponse getCategory(@PathVariable Long id) {
        return categories.get(id);
    }

    @PutMapping("/categories/{id}")
    public NameResponse updateCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return categories.update(id, request);
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categories.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tags")
    public ResponseEntity<NameResponse> createTag(@Valid @RequestBody NameRequest request) {
        NameResponse created = tags.create(request);
        return Responses.created("/api/tags", created.id(), created);
    }

    @GetMapping("/tags")
    public List<NameResponse> listTags() {
        return tags.list();
    }

    @DeleteMapping("/tags/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        tags.delete(id);
        return ResponseEntity.noContent().build();
    }
}
