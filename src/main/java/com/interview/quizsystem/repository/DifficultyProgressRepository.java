package com.interview.quizsystem.repository;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.entity.DifficultyProgress;
import com.interview.quizsystem.model.entity.TopicProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DifficultyProgressRepository extends JpaRepository<DifficultyProgress, Long> {
    Optional<DifficultyProgress> findByTopicProgressAndDifficulty(TopicProgress topicProgress, Difficulty difficulty);
} 