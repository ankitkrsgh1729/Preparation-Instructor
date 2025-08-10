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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final GitHubParserService gitHubParserService;

    @GetMapping
    @Cacheable(value = "topics", unless = "#result.statusCode != T(org.springframework.http.HttpStatus).OK")
    public ResponseEntity<?> getTopics() {
        try {
            List<String> topics = gitHubParserService.getAvailableTopics();
            if (topics.isEmpty()) {
                log.warn("No topics found in the repository");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No topics found"));
            }
            return ResponseEntity.ok(topics.stream().sorted().collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Failed to fetch topics from repository", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch topics"));
        }
    }

    @GetMapping("/availability")
    @Cacheable(value = "topic-availability", unless = "#result.statusCode != T(org.springframework.http.HttpStatus).OK")
    public ResponseEntity<?> getTopicAvailability(@RequestParam String topic) {
        log.info("Received topic availability request for topic: '{}'", topic);
        try {
            // Check if topic exists
            List<String> topics = gitHubParserService.getAvailableTopics();
            if (!topics.contains(topic)) {
                log.warn("Topic not found: '{}'", topic);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Topic does not exist: " + topic));
            }

            // Get content for the topic
            Map<String, String> content = gitHubParserService.getContentByTopic(topic);
            if (content.isEmpty()) {
                log.warn("No content found for topic: '{}'", topic);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No content found for topic: " + topic));
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get topic availability"));
        }
    }
} 