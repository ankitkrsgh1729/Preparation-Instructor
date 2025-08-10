package com.interview.quizsystem.controller;

import com.interview.quizsystem.service.GitHubSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final GitHubSyncService gitHubSyncService;

    @PostMapping("/regenerate")
    public ResponseEntity<?> regenerate(@RequestParam(defaultValue = "12") int perDifficultyTarget) {
        log.info("Manual trigger: regenerate question bank with target {} per difficulty", perDifficultyTarget);
        gitHubSyncService.syncAndPreGenerateQuestions(perDifficultyTarget);
        return ResponseEntity.ok().build();
    }

    // Future: add single-topic regeneration if needed
}


