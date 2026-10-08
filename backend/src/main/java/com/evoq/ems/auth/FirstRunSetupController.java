package com.evoq.ems.auth;

import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/setup")
public class FirstRunSetupController {
    private final FirstRunSetupService setup;
    public FirstRunSetupController(FirstRunSetupService setup) { this.setup = setup; }

    @GetMapping
    public Map<String, Boolean> status() { return Map.of("required", setup.required()); }

    @PostMapping
    public ResponseEntity<Map<String, Boolean>> create(@Valid @RequestBody FirstRunSetupRequest request) {
        setup.create(request);
        return ResponseEntity.status(201).body(Map.of("required", false));
    }
}
