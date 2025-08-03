package com.interview.quizsystem.controller;

import com.interview.quizsystem.dto.StartQuizRequest;
import com.interview.quizsystem.dto.SubmitAnswerRequest;
import com.interview.quizsystem.model.QuizSession;
import com.interview.quizsystem.service.GitHubParserService;
import com.interview.quizsystem.service.QuizSessionService;
import io.github.bucket4j.Bucket;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class QuizSessionController {

    private final QuizSessionService quizSessionService;
    private final GitHubParserService gitHubParserService;
    private final Bucket rateLimitBucket;

    @PostMapping("/start")
    public ResponseEntity<?> startSession(@Valid @RequestBody StartQuizRequest request) {
        log.info("Received request to start quiz session for topic: {}, difficulty: {}, questionCount: {}", 
            request.getTopic(), request.getDifficulty(), request.getQuestionCount());

        // Check rate limit
        if (!rateLimitBucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for quiz session request");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("Rate limit exceeded", "Too many requests. Please try again later."));
        }

        try {
            // Validate topic exists
            List<String> availableTopics = gitHubParserService.getAvailableTopics();
            if (!availableTopics.contains(request.getTopic())) {
                log.warn("Requested topic not found: {}", request.getTopic());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Not found", "Topic does not exist"));
            }

            // Check if content is available for the topic
            Map<String, String> topicContent = gitHubParserService.getContentByTopic(request.getTopic());
            if (topicContent.isEmpty()) {
                log.warn("No content available for topic: {}", request.getTopic());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Not found", "No content available for the selected topic"));
            }

            // Start session
            QuizSession session = quizSessionService.startSession(
                    request.getTopic(),
                    request.getDifficulty(),
                    request.getQuestionCount()
            );

            // Check if enough questions were found
            if (session.getQuestions().size() < request.getQuestionCount()) {
                log.warn("Not enough questions available for topic: {}, difficulty: {}, requested: {}, available: {}", 
                    request.getTopic(), request.getDifficulty(), request.getQuestionCount(), session.getQuestions().size());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Not found", 
                            String.format("Not enough questions available. Requested: %d, Available: %d", 
                                request.getQuestionCount(), session.getQuestions().size())));
            }

            // Remove correct answers from questions before sending to frontend
            session.getQuestions().forEach(q -> q.setCorrectAnswer(null));

            log.info("Successfully created quiz session with ID: {}", session.getId());
            return ResponseEntity.ok(session);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters", e);
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid request", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to start quiz session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Server error", "Failed to start quiz session"));
        }
    }

    @PostMapping("/{sessionId}/submit")
    public ResponseEntity<?> submitAnswer(
            @PathVariable String sessionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        
        log.info("Received answer submission for session: {}, question: {}", 
            sessionId, request.getQuestionId());

        try {
            QuizSession session = quizSessionService.submitAnswer(
                sessionId, 
                request.getQuestionId(), 
                request.getAnswer()
            );

            if (session == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not found", "Session not found or expired"));
            }

            // Ensure answer feedback is included for the submitted question
            session.getQuestions().stream()
                .filter(q -> q.getId().equals(request.getQuestionId()))
                .findFirst()
                .ifPresent(q -> q.setCorrectAnswer(q.getCorrectAnswer())); // This ensures the correct answer is included with feedback

            return ResponseEntity.ok(session);

        } catch (IllegalStateException e) {
            log.warn("Invalid session state: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Invalid request", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to submit answer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Server error", "Failed to submit answer"));
        }
    }

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<?> endSession(@PathVariable String sessionId) {
        log.info("Received request to end session: {}", sessionId);

        try {
            QuizSession session = quizSessionService.endSession(sessionId);
            return ResponseEntity.ok(session);
        } catch (IllegalArgumentException e) {
            log.warn("Session not found: {}", sessionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Not found", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Invalid session state: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Invalid request", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to end session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Server error", "Failed to end session"));
        }
    }

    private static class ErrorResponse {
        private final String error;
        private final String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }
    }
} 