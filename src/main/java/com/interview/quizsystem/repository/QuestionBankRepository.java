package com.interview.quizsystem.repository;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.entity.QuestionBank;
import com.interview.quizsystem.model.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {
    List<QuestionBank> findByTopicAndDifficulty(Topic topic, Difficulty difficulty);
    long countByTopicAndDifficulty(Topic topic, Difficulty difficulty);
    void deleteByTopic(Topic topic);
}


