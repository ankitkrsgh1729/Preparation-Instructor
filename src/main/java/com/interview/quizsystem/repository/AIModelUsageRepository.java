package com.interview.quizsystem.repository;

import com.interview.quizsystem.model.AIOperationType;
import com.interview.quizsystem.model.entity.AIModelUsage;
import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AIModelUsageRepository extends JpaRepository<AIModelUsage, Long> {
    List<AIModelUsage> findByTopicAndCreatedAtBetween(Topic topic, LocalDateTime start, LocalDateTime end);
    List<AIModelUsage> findByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT u FROM AIModelUsage u WHERE u.topic = :topic AND u.operationType = :operationType ORDER BY u.createdAt DESC")
    List<AIModelUsage> findLatestByTopicAndOperation(Topic topic, AIOperationType operationType);
    
    @Query("SELECT COUNT(u) FROM AIModelUsage u WHERE u.topic = :topic AND u.createdAt >= :since")
    long countUsageByTopicSince(Topic topic, LocalDateTime since);
} 