package com.interview.quizsystem.repository;

import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.model.entity.TopicProcessingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TopicProcessingHistoryRepository extends JpaRepository<TopicProcessingHistory, Long> {
    Optional<TopicProcessingHistory> findTopByTopicOrderByProcessedAtDesc(Topic topic);
}


