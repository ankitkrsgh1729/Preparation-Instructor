package com.interview.quizsystem.service.impl;

import com.interview.quizsystem.model.entity.SpacedRepetitionData;
import com.interview.quizsystem.model.entity.User;
import com.interview.quizsystem.repository.SpacedRepetitionDataRepository;
import com.interview.quizsystem.service.SpacedRepetitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpacedRepetitionServiceImpl implements SpacedRepetitionService {

    private final SpacedRepetitionDataRepository spacedRepetitionDataRepository;

    @Override
    @Transactional
    public SpacedRepetitionData initializeQuestion(User user, String questionId) {
        log.debug("Initializing spaced repetition data for user {} and question {}", user.getId(), questionId);
        
        // Check if data already exists
        SpacedRepetitionData existingData = spacedRepetitionDataRepository
            .findByUserIdAndQuestionId(user.getId(), questionId)
            .orElse(null);
            
        if (existingData != null) {
            log.debug("Spaced repetition data already exists for user {} and question {}", user.getId(), questionId);
            return existingData;
        }

        // Create new spaced repetition data with initial values
        SpacedRepetitionData newData = SpacedRepetitionData.builder()
            .user(user)
            .questionId(questionId)
            .repetitionNumber(0)
            .easeFactor(new java.math.BigDecimal("2.5"))
            .intervalDays(0)
            .nextReviewDate(LocalDateTime.now()) // Due immediately for first review
            .consecutiveCorrect(0)
            .consecutiveIncorrect(0)
            .build();

        SpacedRepetitionData savedData = spacedRepetitionDataRepository.save(newData);
        log.debug("Initialized spaced repetition data: {}", savedData.getId());
        
        return savedData;
    }

    @Override
    @Transactional
    public SpacedRepetitionData processAnswer(User user, String questionId, boolean isCorrect) {
        log.debug("Processing answer for user {} and question {}: correct={}", user.getId(), questionId, isCorrect);
        
        SpacedRepetitionData data = spacedRepetitionDataRepository
            .findByUserIdAndQuestionId(user.getId(), questionId)
            .orElseGet(() -> initializeQuestion(user, questionId));

        // Update ease factor based on answer
        data.updateEaseFactor(isCorrect);
        
        // Calculate next interval
        data.calculateNextInterval();
        
        SpacedRepetitionData savedData = spacedRepetitionDataRepository.save(data);
        log.debug("Updated spaced repetition data: repetition={}, easeFactor={}, interval={}, nextReview={}", 
            savedData.getRepetitionNumber(), savedData.getEaseFactor(), savedData.getIntervalDays(), savedData.getNextReviewDate());
        
        return savedData;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpacedRepetitionData> getDueQuestions(User user) {
        log.debug("Getting due questions for user {}", user.getId());
        return spacedRepetitionDataRepository.findDueQuestionsByUserIdOrdered(user.getId(), LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpacedRepetitionData> getDueQuestions(User user, int limit) {
        log.debug("Getting up to {} due questions for user {}", limit, user.getId());
        List<SpacedRepetitionData> dueQuestions = getDueQuestions(user);
        return dueQuestions.stream().limit(limit).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countDueQuestions(User user) {
        log.debug("Counting due questions for user {}", user.getId());
        return spacedRepetitionDataRepository.countDueQuestionsByUserId(user.getId(), LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpacedRepetitionData> getSpacedRepetitionData(User user, List<String> questionIds) {
        log.debug("Getting spaced repetition data for user {} and {} questions", user.getId(), questionIds.size());
        return spacedRepetitionDataRepository.findByUserIdAndQuestionIds(user.getId(), questionIds);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isQuestionDue(User user, String questionId) {
        log.debug("Checking if question {} is due for user {}", questionId, user.getId());
        
        SpacedRepetitionData data = spacedRepetitionDataRepository
            .findByUserIdAndQuestionId(user.getId(), questionId)
            .orElse(null);
            
        if (data == null) {
            // New question is always due
            return true;
        }
        
        return data.isDueForReview();
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime getNextReviewDate(User user, String questionId) {
        log.debug("Getting next review date for question {} and user {}", questionId, user.getId());
        
        SpacedRepetitionData data = spacedRepetitionDataRepository
            .findByUserIdAndQuestionId(user.getId(), questionId)
            .orElse(null);
            
        if (data == null) {
            // New question is due immediately
            return LocalDateTime.now();
        }
        
        return data.getNextReviewDate();
    }
}

