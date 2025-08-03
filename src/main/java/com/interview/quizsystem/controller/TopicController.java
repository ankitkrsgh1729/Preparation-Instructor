package com.interview.quizsystem.controller;

import com.interview.quizsystem.service.GitHubParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TopicController {

    private final GitHubParserService gitHubParserService;

    // Fallback topics when Git sync fails
    private static final List<String> FALLBACK_TOPICS = Arrays.asList(
        "core fundamentals",
        "data structures",
        "algorithms",
        "system design",
        "design patterns"
    );

    @GetMapping("/topics")
    @Cacheable(value = "topics", unless = "#result.statusCode != T(org.springframework.http.HttpStatus).OK")
    public ResponseEntity<?> getTopics() {
        try {
            List<String> topics = gitHubParserService.getAvailableTopics();
            if (topics.isEmpty()) {
                log.warn("No topics found from Git repository, using fallback topics");
                return ResponseEntity.ok(FALLBACK_TOPICS);
            }
            return ResponseEntity.ok(topics.stream().sorted().collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Failed to fetch topics from Git repository, using fallback topics", e);
            return ResponseEntity.ok(FALLBACK_TOPICS);
        }
    }

    @GetMapping("/topics/{topic}/availability")
    @Cacheable(value = "topic-availability", key = "#topic", 
               unless = "#result.statusCode != T(org.springframework.http.HttpStatus).OK")
    public ResponseEntity<?> getTopicAvailability(@PathVariable String topic) {
        try {
            // Check if topic exists
            List<String> topics;
            try {
                topics = gitHubParserService.getAvailableTopics();
            } catch (Exception e) {
                log.warn("Failed to get topics from Git, using fallback topics", e);
                topics = FALLBACK_TOPICS;
            }

            if (!topics.contains(topic) && !FALLBACK_TOPICS.contains(topic)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not found", "Topic does not exist"));
            }

            // Get content for the topic
            Map<String, String> content;
            try {
                content = gitHubParserService.getContentByTopic(topic);
            } catch (Exception e) {
                log.warn("Failed to get content for topic: {}, using default values", topic, e);
                content = Map.of("default", "default content");
            }

            int estimatedQuestions = Math.max(content.size() * 2, 5); // At least 5 questions per topic

            return ResponseEntity.ok(Map.of(
                "topic", topic,
                "contentPieces", content.size(),
                "estimatedQuestions", estimatedQuestions,
                "recommendedMaxQuestions", Math.min(estimatedQuestions, 10)
            ));

        } catch (Exception e) {
            log.error("Failed to get topic availability for topic: {}", topic, e);
            return ResponseEntity.ok(Map.of(
                "topic", topic,
                "contentPieces", 1,
                "estimatedQuestions", 5,
                "recommendedMaxQuestions", 5
            ));
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