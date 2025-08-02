package com.interview.quizsystem.service;

import com.interview.quizsystem.model.Question;
import com.interview.quizsystem.model.Difficulty;

import java.util.List;

public interface QuestionGeneratorService {
    List<Question> generateQuestions(String topic, int count, Difficulty difficulty);
    Question generateQuestion(String content, String topic, Difficulty difficulty);
} 