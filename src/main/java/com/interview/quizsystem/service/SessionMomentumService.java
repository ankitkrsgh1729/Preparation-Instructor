package com.interview.quizsystem.service;

import com.interview.quizsystem.model.entity.SessionMomentum;
import com.interview.quizsystem.model.entity.User;

import java.util.Optional;

public interface SessionMomentumService {
    
    /**
     * Initialize session momentum tracking
     */
    SessionMomentum initializeSession(String sessionId, User user);
    
    /**
     * Record an answer and update momentum
     */
    SessionMomentum recordAnswer(String sessionId, User user, boolean isCorrect, long responseTimeMs);
    
    /**
     * Get current session momentum
     */
    Optional<SessionMomentum> getSessionMomentum(String sessionId);
    
    /**
     * Calculate and update momentum score
     */
    SessionMomentum calculateMomentum(String sessionId);
    
    /**
     * Check if user is in flow state
     */
    boolean isInFlow(String sessionId);
    
    /**
     * Check if user is struggling
     */
    boolean isStruggling(String sessionId);
    
    /**
     * Get momentum score (0-100)
     */
    double getMomentumScore(String sessionId);
    
    /**
     * Get current accuracy rate for the session
     */
    double getSessionAccuracy(String sessionId);
    
    /**
     * Get average response time for the session
     */
    long getAverageResponseTime(String sessionId);
}

