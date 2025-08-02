package com.interview.quizsystem.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.quizsystem.model.Progress;
import com.interview.quizsystem.model.QuizSession;
import com.interview.quizsystem.model.TopicProgress;
import com.interview.quizsystem.model.DifficultyProgress;
import com.interview.quizsystem.service.ProgressTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressTrackingServiceImpl implements ProgressTrackingService {

    private final ObjectMapper objectMapper;

    @Value("${progress.storage.path}")
    private String progressFilePath;

    private Progress currentProgress;

    @Override
    public Progress getProgress() {
        if (currentProgress == null) {
            currentProgress = loadProgress();
        }
        return currentProgress;
    }

    @Override
    public Progress updateProgress(QuizSession completedSession) {
        Progress progress = getProgress();
        String topic = completedSession.getTopic();

        TopicProgress topicProgress = progress.getTopicProgressMap()
                .computeIfAbsent(topic, k -> TopicProgress.builder()
                        .topic(topic)
                        .difficultyProgressMap(new HashMap<>())
                        .build());

        // Update topic-level progress
        topicProgress.setQuestionsAnswered(topicProgress.getQuestionsAnswered() + completedSession.getQuestions().size());
        topicProgress.setCorrectAnswers(topicProgress.getCorrectAnswers() + (int) completedSession.getScore());
        topicProgress.setAccuracy((double) topicProgress.getCorrectAnswers() / topicProgress.getQuestionsAnswered());
        topicProgress.setLastStudied(LocalDateTime.now());
        topicProgress.setCurrentDifficulty(completedSession.getDifficulty());

        // Update difficulty-level progress
        DifficultyProgress difficultyProgress = topicProgress.getDifficultyProgressMap()
                .computeIfAbsent(completedSession.getDifficulty(), k -> DifficultyProgress.builder().build());

        difficultyProgress.setQuestionsAnswered(difficultyProgress.getQuestionsAnswered() + completedSession.getQuestions().size());
        difficultyProgress.setCorrectAnswers(difficultyProgress.getCorrectAnswers() + (int) completedSession.getScore());
        difficultyProgress.setAccuracy((double) difficultyProgress.getCorrectAnswers() / difficultyProgress.getQuestionsAnswered());

        // Update overall progress
        progress.setTotalQuestionsAnswered(progress.getTotalQuestionsAnswered() + completedSession.getQuestions().size());
        progress.setTotalCorrectAnswers(progress.getTotalCorrectAnswers() + (int) completedSession.getScore());
        progress.setLastUpdated(LocalDateTime.now());

        saveProgress(progress);
        return progress;
    }

    @Override
    public void saveProgress(Progress progress) {
        try {
            File file = new File(progressFilePath);
            file.getParentFile().mkdirs();
            objectMapper.writeValue(file, progress);
            currentProgress = progress;
        } catch (IOException e) {
            log.error("Error saving progress to file: {}", progressFilePath, e);
            throw new RuntimeException("Failed to save progress", e);
        }
    }

    @Override
    public Progress loadProgress() {
        File file = new File(progressFilePath);
        if (!file.exists()) {
            return createInitialProgress();
        }

        try {
            return objectMapper.readValue(file, Progress.class);
        } catch (IOException e) {
            log.error("Error loading progress from file: {}", progressFilePath, e);
            return createInitialProgress();
        }
    }

    private Progress createInitialProgress() {
        return Progress.builder()
                .topicProgressMap(new HashMap<>())
                .totalQuestionsAnswered(0)
                .totalCorrectAnswers(0)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
} 