package com.interview.quizsystem.repository;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.QuestionType;
import com.interview.quizsystem.model.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTopicId(Long topicId);
    List<Question> findByTopicIdAndDifficulty(Long topicId, Difficulty difficulty);
    List<Question> findByTopicIdAndQuestionType(Long topicId, QuestionType questionType);
    List<Question> findByDifficulty(Difficulty difficulty);
} 