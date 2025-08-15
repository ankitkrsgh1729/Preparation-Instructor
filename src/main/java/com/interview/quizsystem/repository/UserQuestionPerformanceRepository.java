package com.interview.quizsystem.repository;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.entity.UserQuestionPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserQuestionPerformanceRepository extends JpaRepository<UserQuestionPerformance, Long> {
    
    Optional<UserQuestionPerformance> findByUserIdAndQuestionId(Long userId, String questionId);
    
    List<UserQuestionPerformance> findByUserIdAndTopicId(Long userId, Long topicId);
    
    List<UserQuestionPerformance> findByUserIdAndDifficulty(Long userId, Difficulty difficulty);
    
    @Query("SELECT uqp FROM UserQuestionPerformance uqp WHERE uqp.user.id = :userId AND uqp.topic.id = :topicId AND uqp.difficulty = :difficulty")
    List<UserQuestionPerformance> findByUserIdAndTopicIdAndDifficulty(
        @Param("userId") Long userId, 
        @Param("topicId") Long topicId, 
        @Param("difficulty") Difficulty difficulty
    );
    
    @Query("SELECT AVG(uqp.averageResponseTimeMs) FROM UserQuestionPerformance uqp WHERE uqp.user.id = :userId AND uqp.topic.id = :topicId")
    Double getAverageResponseTimeByUserAndTopic(@Param("userId") Long userId, @Param("topicId") Long topicId);
    
    @Query("SELECT AVG(uqp.correctAttempts * 100.0 / uqp.totalAttempts) FROM UserQuestionPerformance uqp WHERE uqp.user.id = :userId AND uqp.topic.id = :topicId")
    Double getAverageAccuracyByUserAndTopic(@Param("userId") Long userId, @Param("topicId") Long topicId);
}

