package com.interview.quizsystem.service;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.QuestionDTO;
import java.util.List;

public interface QuestionGeneratorService {
    List<QuestionDTO> generateQuestions(String topic, int count, Difficulty difficulty);
    QuestionDTO generateQuestion(String content, String topic, Difficulty difficulty);
} 