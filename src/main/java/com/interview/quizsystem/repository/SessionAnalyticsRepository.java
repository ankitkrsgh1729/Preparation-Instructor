package com.interview.quizsystem.repository;

import com.interview.quizsystem.model.entity.SessionAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionAnalyticsRepository extends JpaRepository<SessionAnalytics, Long> {
    
    List<SessionAnalytics> findBySessionId(String sessionId);
    
    List<SessionAnalytics> findBySessionIdOrderByAnsweredAtAsc(String sessionId);
    
    @Query("SELECT sa FROM SessionAnalytics sa WHERE sa.sessionId = :sessionId ORDER BY sa.answeredAt ASC")
    List<SessionAnalytics> findSessionAnalyticsOrdered(@Param("sessionId") String sessionId);
    
    @Query("SELECT AVG(sa.responseTimeMs) FROM SessionAnalytics sa WHERE sa.sessionId = :sessionId")
    Double getAverageResponseTimeBySession(@Param("sessionId") String sessionId);
    
    @Query("SELECT COUNT(sa) FROM SessionAnalytics sa WHERE sa.sessionId = :sessionId AND sa.isCorrect = true")
    Long getCorrectAnswersCountBySession(@Param("sessionId") String sessionId);
    
    @Query("SELECT COUNT(sa) FROM SessionAnalytics sa WHERE sa.sessionId = :sessionId")
    Long getTotalAnswersCountBySession(@Param("sessionId") String sessionId);
}

