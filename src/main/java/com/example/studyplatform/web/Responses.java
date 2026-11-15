package com.example.studyplatform.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

final class Responses {

    private Responses() {
    }

    static <T> ResponseEntity<T> created(String basePath, Long id, T body) {
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path(basePath + "/{id}")
                .buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(body);
    }
}
