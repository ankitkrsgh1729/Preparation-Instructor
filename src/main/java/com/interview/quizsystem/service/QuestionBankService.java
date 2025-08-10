package com.interview.quizsystem.service;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.QuestionDTO;
import com.interview.quizsystem.model.entity.Topic;

import java.util.List;

public interface QuestionBankService {
    void regenerateForTopic(Topic topic, int perDifficultyTarget);
    List<QuestionDTO> getQuestionsByTopicAndDifficulty(Topic topic, Difficulty difficulty, int limit);
    long countByTopicAndDifficulty(Topic topic, Difficulty difficulty);
}


