package com.interview.quizsystem.service.impl;

import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.repository.TopicRepository;
import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.entity.TopicProcessingHistory;
import com.interview.quizsystem.repository.TopicProcessingHistoryRepository;
import com.interview.quizsystem.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubSyncServiceImpl implements GitHubSyncService {

    private final GitHubParserService gitHubParserService;
    private final QuestionBankService questionBankService;
    private final TopicRepository topicRepository;
    private final TopicProcessingHistoryRepository topicProcessingHistoryRepository;
    private final ContentHashService contentHashService;

    @Override
    @Transactional
    public void syncAndPreGenerateQuestions(int perDifficultyTarget) {
        log.info("Starting GitHub sync and pre-generation. Target per difficulty: {}", perDifficultyTarget);
        gitHubParserService.syncRepository();
        List<String> topics = gitHubParserService.getAvailableTopics();
        for (String topicName : topics) {
            Topic topic = topicRepository.findByNameIgnoreCase(topicName)
                    .orElseThrow(() -> new IllegalStateException("Topic not found after sync: " + topicName));
            // Calculate current content hash for change detection
            String combined = String.join("\n\n", gitHubParserService.getContentByTopic(topicName).values());
            String newHash = contentHashService.calculateHash(combined);

            String lastHash = topicProcessingHistoryRepository.findTopByTopicOrderByProcessedAtDesc(topic)
                    .map(TopicProcessingHistory::getContentHash)
                    .orElse(null);

            if (lastHash != null && lastHash.equals(newHash)) {
                log.info("No changes detected for topic: {}. Skipping regeneration.", topicName);
                continue;
            }

            log.info("Changes detected for topic: {}. Regenerating question bank...", topicName);
            questionBankService.regenerateForTopic(topic, perDifficultyTarget);

            // Store processing history
            int totalGenerated = 0;
            for (Difficulty difficulty : Difficulty.values()) {
                totalGenerated += (int) questionBankService.countByTopicAndDifficulty(topic, difficulty);
            }
            TopicProcessingHistory history = TopicProcessingHistory.builder()
                    .topic(topic)
                    .contentHash(newHash)
                    .questionsGenerated(totalGenerated)
                    .status("SUCCESS")
                    .build();
            topicProcessingHistoryRepository.save(history);
        }
        log.info("Completed pre-generation for {} topics", topics.size());
    }
}


