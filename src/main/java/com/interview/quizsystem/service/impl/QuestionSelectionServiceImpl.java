package com.interview.quizsystem.service.impl;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.QuestionDTO;
import com.interview.quizsystem.model.entity.SpacedRepetitionData;
import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.model.entity.User;
import com.interview.quizsystem.model.entity.UserQuestionPerformance;
import com.interview.quizsystem.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionSelectionServiceImpl implements QuestionSelectionService {

    private final QuestionBankService questionBankService;
    private final SpacedRepetitionService spacedRepetitionService;
    private final UserQuestionPerformanceService userQuestionPerformanceService;
    private final SessionMomentumService sessionMomentumService;

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDTO> selectQuestionsForSession(User user, Topic topic, Difficulty difficulty, int questionCount) {
        log.debug("Selecting {} questions for user {} topic {} difficulty {}", questionCount, user.getId(), topic.getName(), difficulty);
        
        // First, try to get due spaced repetition questions
        List<QuestionDTO> dueQuestions = selectDueQuestions(user, topic, questionCount);
        
        if (dueQuestions.size() >= questionCount) {
            log.debug("Selected {} due questions for session", dueQuestions.size());
            return dueQuestions.subList(0, questionCount);
        }
        
        // Fill remaining slots with new questions at appropriate difficulty
        int remainingCount = questionCount - dueQuestions.size();
        List<QuestionDTO> newQuestions = selectQuestionsByDifficultyProgression(user, topic, remainingCount);
        
        // Combine due questions and new questions
        List<QuestionDTO> allQuestions = new ArrayList<>(dueQuestions);
        allQuestions.addAll(newQuestions);
        
        log.debug("Selected {} questions total: {} due + {} new", allQuestions.size(), dueQuestions.size(), newQuestions.size());
        return allQuestions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDTO> selectDueQuestions(User user, Topic topic, int questionCount) {
        log.debug("Selecting due questions for user {} topic {}", user.getId(), topic.getName());
        
        List<SpacedRepetitionData> dueData = spacedRepetitionService.getDueQuestions(user, questionCount * 2); // Get more to filter by topic
        
        // Filter by topic and get question IDs
        List<String> dueQuestionIds = dueData.stream()
            .filter(data -> {
                // We need to check if the question belongs to this topic
                // This would require a join or additional query to get question details
                // For now, we'll get all due questions and let the question bank service filter
                return true;
            })
            .map(SpacedRepetitionData::getQuestionId)
            .limit(questionCount)
            .collect(Collectors.toList());
        
        if (dueQuestionIds.isEmpty()) {
            log.debug("No due questions found for user {} topic {}", user.getId(), topic.getName());
            return new ArrayList<>();
        }
        
        // Get questions from question bank
        List<QuestionDTO> dueQuestions = questionBankService.getQuestionsByIds(dueQuestionIds);
        
        // Filter by topic
        dueQuestions = dueQuestions.stream()
            .filter(q -> topic.getName().equals(q.getTopic()))
            .collect(Collectors.toList());
        
        log.debug("Selected {} due questions for topic {}", dueQuestions.size(), topic.getName());
        return dueQuestions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDTO> selectQuestionsByDifficultyProgression(User user, Topic topic, int questionCount) {
        log.debug("Selecting questions by difficulty progression for user {} topic {}", user.getId(), topic.getName());
        
        Difficulty recommendedDifficulty = getRecommendedDifficulty(user, topic);
        log.debug("Recommended difficulty for user {} topic {}: {}", user.getId(), topic.getName(), recommendedDifficulty);
        
        // Get questions at recommended difficulty
        List<QuestionDTO> questions = questionBankService.getQuestionsByTopicAndDifficulty(topic, recommendedDifficulty, questionCount * 2);
        
        // Filter out questions the user has already attempted
        List<UserQuestionPerformance> userPerformance = userQuestionPerformanceService.getPerformanceByTopic(user, topic.getId());
        Set<String> attemptedQuestionIds = userPerformance.stream()
            .map(UserQuestionPerformance::getQuestionId)
            .collect(Collectors.toSet());
        
        questions = questions.stream()
            .filter(q -> !attemptedQuestionIds.contains(q.getId()))
            .limit(questionCount)
            .collect(Collectors.toList());
        
        log.debug("Selected {} new questions at difficulty {}", questions.size(), recommendedDifficulty);
        return questions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDTO> selectQuestionsByMomentum(User user, Topic topic, String sessionId, int questionCount) {
        log.debug("Selecting questions by momentum for user {} topic {} session {}", user.getId(), topic.getName(), sessionId);
        
        boolean isStruggling = sessionMomentumService.isStruggling(sessionId);
        boolean isInFlow = sessionMomentumService.isInFlow(sessionId);
        
        if (isStruggling) {
            log.debug("User is struggling, selecting easier questions");
            // Select easier questions for struggling users
            return selectEasierQuestions(user, topic, questionCount);
        } else if (isInFlow) {
            log.debug("User is in flow, selecting harder questions");
            // Select harder questions for users in flow
            return selectHarderQuestions(user, topic, questionCount);
        } else {
            log.debug("User is in normal state, selecting standard questions");
            // Select standard questions
            return selectQuestionsByDifficultyProgression(user, topic, questionCount);
        }
    }

    private List<QuestionDTO> selectEasierQuestions(User user, Topic topic, int questionCount) {
        Difficulty currentDifficulty = getRecommendedDifficulty(user, topic);
        Difficulty easierDifficulty = getEasierDifficulty(currentDifficulty);
        
        if (easierDifficulty == null) {
            // Already at easiest level, return current difficulty
            return questionBankService.getQuestionsByTopicAndDifficulty(topic, currentDifficulty, questionCount);
        }
        
        return questionBankService.getQuestionsByTopicAndDifficulty(topic, easierDifficulty, questionCount);
    }

    private List<QuestionDTO> selectHarderQuestions(User user, Topic topic, int questionCount) {
        Difficulty currentDifficulty = getRecommendedDifficulty(user, topic);
        Difficulty harderDifficulty = getHarderDifficulty(currentDifficulty);
        
        if (harderDifficulty == null) {
            // Already at hardest level, return current difficulty
            return questionBankService.getQuestionsByTopicAndDifficulty(topic, currentDifficulty, questionCount);
        }
        
        return questionBankService.getQuestionsByTopicAndDifficulty(topic, harderDifficulty, questionCount);
    }

    private Difficulty getEasierDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case HARD -> Difficulty.MEDIUM;
            case MEDIUM -> Difficulty.EASY;
            case EASY -> null; // Already at easiest
        };
    }

    private Difficulty getHarderDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> Difficulty.MEDIUM;
            case MEDIUM -> Difficulty.HARD;
            case HARD -> null; // Already at hardest
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Difficulty getRecommendedDifficulty(User user, Topic topic) {
        log.debug("Getting recommended difficulty for user {} topic {}", user.getId(), topic.getName());
        
        // Check if user has achieved proficiency at EASY level
        if (!userQuestionPerformanceService.hasAchievedProficiencyLevel(user, topic.getId(), Difficulty.EASY)) {
            log.debug("User has not achieved EASY proficiency, recommending EASY");
            return Difficulty.EASY;
        }
        
        // Check if user has achieved proficiency at MEDIUM level
        if (!userQuestionPerformanceService.hasAchievedProficiencyLevel(user, topic.getId(), Difficulty.MEDIUM)) {
            log.debug("User has achieved EASY proficiency but not MEDIUM, recommending MEDIUM");
            return Difficulty.MEDIUM;
        }
        
        // Check if user has achieved proficiency at HARD level
        if (!userQuestionPerformanceService.hasAchievedProficiencyLevel(user, topic.getId(), Difficulty.HARD)) {
            log.debug("User has achieved MEDIUM proficiency but not HARD, recommending HARD");
            return Difficulty.HARD;
        }
        
        log.debug("User has achieved all difficulty levels, recommending HARD");
        return Difficulty.HARD;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldAdvanceDifficulty(User user, Topic topic, Difficulty currentDifficulty) {
        log.debug("Checking if user {} should advance from difficulty {} in topic {}", user.getId(), currentDifficulty, topic.getName());
        
        return userQuestionPerformanceService.hasAchievedProficiencyLevel(user, topic.getId(), currentDifficulty);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDTO> getReviewQuestions(User user, Topic topic, int questionCount) {
        log.debug("Getting review questions for user {} topic {}", user.getId(), topic.getName());
        return selectDueQuestions(user, topic, questionCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDTO> getLearningQuestions(User user, Topic topic, int questionCount) {
        log.debug("Getting learning questions for user {} topic {}", user.getId(), topic.getName());
        return selectQuestionsByDifficultyProgression(user, topic, questionCount);
    }
}

