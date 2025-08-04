package com.interview.quizsystem.repository;

import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.model.entity.TopicProgress;
import com.interview.quizsystem.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TopicProgressRepository extends JpaRepository<TopicProgress, Long> {
    Optional<TopicProgress> findByUserAndTopic(User user, Topic topic);
    
    List<TopicProgress> findByUser(User user);
    
    @Query("SELECT tp FROM TopicProgress tp WHERE tp.active = true AND tp.lastAttemptDate < :expiryDate")
    List<TopicProgress> findExpiredProgress(LocalDateTime expiryDate);
    
    @Query("SELECT tp FROM TopicProgress tp WHERE tp.user = :user AND tp.active = true")
    List<TopicProgress> findActiveProgressByUser(User user);
} 