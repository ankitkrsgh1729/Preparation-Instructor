package com.interview.quizsystem.controller;

import com.interview.quizsystem.dto.StartQuizRequest;
import com.interview.quizsystem.dto.SubmitAnswerRequest;
import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.QuizSession;
import com.interview.quizsystem.service.GitHubParserService;
import com.interview.quizsystem.service.QuizSessionService;
import com.interview.quizsystem.service.QuestionSelectionService;
import com.interview.quizsystem.service.SpacedRepetitionService;
import com.interview.quizsystem.service.UserQuestionPerformanceService;
import com.interview.quizsystem.service.SessionMomentumService;
import com.interview.quizsystem.service.UserService;
import com.interview.quizsystem.service.TopicService;
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
public class QuizSessionController {

    private final QuizSessionService quizSessionService;
    private final GitHubParserService gitHubParserService;
    private final QuestionSelectionService questionSelectionService;
    private final SpacedRepetitionService spacedRepetitionService;
    private final UserQuestionPerformanceService userQuestionPerformanceService;
    private final SessionMomentumService sessionMomentumService;
    private final UserService userService;
    private final TopicService topicService;
    private final Bucket rateLimitBucket;

    @PostMapping("/start")
    public ResponseEntity<?> startSession(@Valid @RequestBody StartQuizRequest request) {
        log.info("Received request to start quiz session for topic: {}, difficulty: {}, questionCount: {}", 
            request.getTopic(), request.getDifficulty(), request.getQuestionCount());

        // Check rate limit
        if (!rateLimitBucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for quiz session request");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Rate limit exceeded", "message", "Too many requests. Please try again later."));
        }

        try {
            // Validate topic exists
            List<String> availableTopics = gitHubParserService.getAvailableTopics();
            if (!availableTopics.contains(request.getTopic())) {
                log.warn("Requested topic not found: {}", request.getTopic());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Not found", "message", "Topic does not exist"));
            }

            // Check if content is available for the topic
            Map<String, String> topicContent = gitHubParserService.getContentByTopic(request.getTopic());
            if (topicContent.isEmpty()) {
                log.warn("No content available for topic: {}", request.getTopic());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Not found", "message", "No content available for the selected topic"));
            }

            // Start session (now uses question bank internally)
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
                        .body(Map.of(
                            "error", "Not found",
                            "message", String.format("Not enough questions available. Requested: %d, Available: %d",
                                request.getQuestionCount(), session.getQuestions().size())
                        ));
            }

            // Remove correct answers from questions before sending to frontend
            session.getQuestions().forEach(q -> q.setCorrectAnswer(null));

            log.info("Successfully created quiz session with ID: {}", session.getId());
            return ResponseEntity.ok(session);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid request", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to start quiz session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error", "message", "Failed to start quiz session"));
        }
    }

    @PostMapping("/{sessionId}/submit")
    public ResponseEntity<?> submitAnswer(
            @PathVariable String sessionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        
        log.info("Received answer submission for session: {}, question: {}", sessionId, request.getQuestionId());

        try {
            QuizSession session = quizSessionService.submitAnswer(
                    sessionId,
                    request.getQuestionId(),
                    request.getAnswer()
            );

            return ResponseEntity.ok(session);
        } catch (IllegalArgumentException e) {
            log.error("Invalid submission parameters", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid request", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to submit answer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error", "message", "Failed to submit answer"));
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
                    .body(Map.of("error", "Not found", "message", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Invalid session state: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid request", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to end session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error", "message", "Failed to end session"));
        }
    }

    @GetMapping("/{sessionId}/momentum")
    public ResponseEntity<?> getSessionMomentum(@PathVariable String sessionId) {
        log.info("Getting session momentum for session: {}", sessionId);

        try {
            var user = userService.getCurrentUser();
            var momentum = sessionMomentumService.getSessionMomentum(sessionId);
            
            if (momentum.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not found", "message", "Session momentum not found"));
            }

            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "momentumScore", momentum.get().getMomentumScore(),
                "accuracyRate", momentum.get().getAccuracyRate(),
                "averageResponseTime", momentum.get().getAverageResponseTimeMs(),
                "questionsAnswered", momentum.get().getQuestionsAnswered(),
                "correctAnswers", momentum.get().getCorrectAnswers(),
                "isInFlow", momentum.get().isInFlow(),
                "isStruggling", momentum.get().isStruggling()
            ));
        } catch (Exception e) {
            log.error("Failed to get session momentum", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Server error", "message", "Failed to get session momentum"));
        }
    }

    @GetMapping("/due-questions/{topic}")
    public ResponseEntity<?> getDueQuestions(@PathVariable String topic, @RequestParam(defaultValue = "10") int limit) {
        log.info("Getting due questions for topic: {}, limit: {}", topic, limit);

        try {
            var user = userService.getCurrentUser();
            var topicEntity = topicService.getTopicByName(topic);
            
            var dueQuestions = questionSelectionService.getReviewQuestions(user, topicEntity, limit);
            
            // Remove correct answers from questions before sending to frontend
            dueQuestions.forEach(q -> q.setCorrectAnswer(null));

            return ResponseEntity.ok(Map.of(
                "topic", topic,
                "dueQuestions", dueQuestions,
                "count", dueQuestions.size()
            ));
        } catch (Exception e) {
            log.error("Failed to get due questions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Server error", "message", "Failed to get due questions"));
        }
    }

    @GetMapping("/recommended-difficulty/{topic}")
    public ResponseEntity<?> getRecommendedDifficulty(@PathVariable String topic) {
        log.info("Getting recommended difficulty for topic: {}", topic);

        try {
            var user = userService.getCurrentUser();
            var topicEntity = topicService.getTopicByName(topic);
            
            Difficulty recommendedDifficulty = questionSelectionService.getRecommendedDifficulty(user, topicEntity);
            
            return ResponseEntity.ok(Map.of(
                "topic", topic,
                "recommendedDifficulty", recommendedDifficulty,
                "shouldAdvanceEasy", questionSelectionService.shouldAdvanceDifficulty(user, topicEntity, Difficulty.EASY),
                "shouldAdvanceMedium", questionSelectionService.shouldAdvanceDifficulty(user, topicEntity, Difficulty.MEDIUM),
                "shouldAdvanceHard", questionSelectionService.shouldAdvanceDifficulty(user, topicEntity, Difficulty.HARD)
            ));
        } catch (Exception e) {
            log.error("Failed to get recommended difficulty", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Server error", "message", "Failed to get recommended difficulty"));
        }
    }

    @GetMapping("/performance/{topic}")
    public ResponseEntity<?> getTopicPerformance(@PathVariable String topic) {
        log.info("Getting performance for topic: {}", topic);

        try {
            var user = userService.getCurrentUser();
            var topicEntity = topicService.getTopicByName(topic);
            
            Double averageAccuracy = userQuestionPerformanceService.getAverageAccuracy(user, topicEntity.getId());
            Double averageResponseTime = userQuestionPerformanceService.getAverageResponseTime(user, topicEntity.getId());
            
            return ResponseEntity.ok(Map.of(
                "topic", topic,
                "averageAccuracy", averageAccuracy != null ? averageAccuracy : 0.0,
                "averageResponseTime", averageResponseTime != null ? averageResponseTime : 0.0,
                "dueQuestionsCount", spacedRepetitionService.countDueQuestions(user)
            ));
        } catch (Exception e) {
            log.error("Failed to get topic performance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Server error", "message", "Failed to get topic performance"));
        }
    }
} 