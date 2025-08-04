package com.interview.quizsystem.controller;

import com.interview.quizsystem.config.LearningCycleConfig;
import com.interview.quizsystem.dto.DifficultyProgressDTO;
import com.interview.quizsystem.dto.TopicProgressDTO;
import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.entity.DifficultyProgress;
import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.model.entity.TopicProgress;
import com.interview.quizsystem.model.entity.User;
import com.interview.quizsystem.service.ProgressService;
import com.interview.quizsystem.service.TopicService;
import com.interview.quizsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ProgressController {

    private final ProgressService progressService;
    private final TopicService topicService;
    private final UserService userService;
    private final LearningCycleConfig learningCycleConfig;

    @GetMapping
    public ResponseEntity<List<TopicProgressDTO>> getUserProgress() {
        User user = userService.getCurrentUser();
        
        // Get all topics and ensure progress exists for each
        List<Topic> allTopics = topicService.getAllTopics();
        allTopics.forEach(topic -> progressService.getOrCreateTopicProgress(user, topic));
        
        // Now get all progress
        List<TopicProgress> progress = progressService.getUserProgress(user);
        
        List<TopicProgressDTO> dtos = progress.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/active")
    public ResponseEntity<List<TopicProgressDTO>> getActiveProgress() {
        User user = userService.getCurrentUser();
        List<TopicProgress> progress = progressService.getActiveProgressByUser(user);
        
        List<TopicProgressDTO> dtos = progress.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/topics/{topicName}")
    public ResponseEntity<TopicProgressDTO> getTopicProgress(@PathVariable String topicName) {
        User user = userService.getCurrentUser();
        Topic topic = topicService.getTopicByName(topicName);
        
        if (topic == null) {
            return ResponseEntity.notFound().build();
        }
        
        TopicProgress progress = progressService.getOrCreateTopicProgress(user, topic);
        return ResponseEntity.ok(convertToDTO(progress));
    }

    @GetMapping("/topics/{topicName}/difficulty/{difficulty}")
    public ResponseEntity<DifficultyProgressDTO> getDifficultyProgress(
            @PathVariable String topicName,
            @PathVariable Difficulty difficulty) {
        User user = userService.getCurrentUser();
        Topic topic = topicService.getTopicByName(topicName);
        
        if (topic == null) {
            return ResponseEntity.notFound().build();
        }
        
        TopicProgress topicProgress = progressService.getOrCreateTopicProgress(user, topic);
        DifficultyProgress progress = progressService.getDifficultyProgress(topicProgress, difficulty);
        
        return ResponseEntity.ok(convertToDTO(progress, topicProgress));
    }

    @PostMapping("/topics/{topicName}/reset")
    public ResponseEntity<TopicProgressDTO> resetTopicProgress(@PathVariable String topicName) {
        User user = userService.getCurrentUser();
        Topic topic = topicService.getTopicByName(topicName);
        
        if (topic == null) {
            return ResponseEntity.notFound().build();
        }
        
        TopicProgress progress = progressService.getOrCreateTopicProgress(user, topic);
        progressService.resetProgress(progress);
        
        // Get the new progress after reset
        TopicProgress newProgress = progressService.getOrCreateTopicProgress(user, topic);
        return ResponseEntity.ok(convertToDTO(newProgress));
    }

    private TopicProgressDTO convertToDTO(TopicProgress progress) {
        List<DifficultyProgressDTO> difficultyDTOs = progress.getDifficultyProgress().stream()
                .map(dp -> convertToDTO(dp, progress))
                .collect(Collectors.toList());

        int daysRemaining = learningCycleConfig.getDays();
        if (progress.getLastAttemptDate() != null) {
            long daysSinceLastAttempt = ChronoUnit.DAYS.between(progress.getLastAttemptDate(), LocalDateTime.now());
            daysRemaining = (int) Math.max(0, learningCycleConfig.getDays() - daysSinceLastAttempt);
        }

        return TopicProgressDTO.builder()
                .topicName(progress.getTopic().getName())
                .overallScore(progress.getOverallScore())
                .questionsAttempted(progress.getQuestionsAttempted())
                .questionsCorrect(progress.getQuestionsCorrect())
                .startDate(progress.getStartDate())
                .lastAttemptDate(progress.getLastAttemptDate())
                .active(progress.isActive())
                .daysRemaining(daysRemaining)
                .difficultyProgress(difficultyDTOs)
                .build();
    }

    private DifficultyProgressDTO convertToDTO(DifficultyProgress progress, TopicProgress topicProgress) {
        Difficulty currentDifficulty = getCurrentDifficulty(topicProgress);
        boolean canProgress = progress.getQuestionsAttempted() >= learningCycleConfig.getMinQuestions() &&
                            progress.getScore() >= learningCycleConfig.getDifficultyProgressionThreshold();

        return DifficultyProgressDTO.builder()
                .difficulty(progress.getDifficulty())
                .score(progress.getScore())
                .questionsAttempted(progress.getQuestionsAttempted())
                .questionsCorrect(progress.getQuestionsCorrect())
                .lastAttemptDate(progress.getLastAttemptDate())
                .isCurrentDifficulty(progress.getDifficulty() == currentDifficulty)
                .canProgress(canProgress)
                .build();
    }

    private Difficulty getCurrentDifficulty(TopicProgress progress) {
        for (Difficulty difficulty : Difficulty.values()) {
            DifficultyProgress difficultyProgress = progressService.getDifficultyProgress(progress, difficulty);
            if (difficultyProgress.getQuestionsAttempted() < learningCycleConfig.getMinQuestions() ||
                difficultyProgress.getScore() < learningCycleConfig.getDifficultyProgressionThreshold()) {
                return difficulty;
            }
        }
        return Difficulty.HARD;
    }
} 