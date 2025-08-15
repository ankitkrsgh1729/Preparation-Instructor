package com.interview.quizsystem.service.impl;

import com.interview.quizsystem.model.entity.SessionMomentum;
import com.interview.quizsystem.model.entity.User;
import com.interview.quizsystem.repository.SessionMomentumRepository;
import com.interview.quizsystem.service.SessionMomentumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionMomentumServiceImpl implements SessionMomentumService {

    private final SessionMomentumRepository sessionMomentumRepository;

    @Override
    @Transactional
    public SessionMomentum initializeSession(String sessionId, User user) {
        log.debug("Initializing session momentum for session {} user {}", sessionId, user.getId());
        
        // Check if momentum tracking already exists
        Optional<SessionMomentum> existingMomentum = sessionMomentumRepository.findBySessionId(sessionId);
        if (existingMomentum.isPresent()) {
            log.debug("Session momentum already exists for session {}", sessionId);
            return existingMomentum.get();
        }

        SessionMomentum momentum = SessionMomentum.builder()
            .sessionId(sessionId)
            .user(user)
            .momentumScore(java.math.BigDecimal.ZERO)
            .responseTimeTrend(java.math.BigDecimal.ZERO)
            .accuracyTrend(java.math.BigDecimal.ZERO)
            .questionsAnswered(0)
            .correctAnswers(0)
            .averageResponseTimeMs(0L)
            .build();

        SessionMomentum savedMomentum = sessionMomentumRepository.save(momentum);
        log.debug("Initialized session momentum: {}", savedMomentum.getId());
        
        return savedMomentum;
    }

    @Override
    @Transactional
    public SessionMomentum recordAnswer(String sessionId, User user, boolean isCorrect, long responseTimeMs) {
        log.debug("Recording answer for session {}: correct={}, responseTime={}ms", sessionId, isCorrect, responseTimeMs);
        
        SessionMomentum momentum = sessionMomentumRepository
            .findBySessionId(sessionId)
            .orElseGet(() -> initializeSession(sessionId, user));
        
        momentum.recordAnswer(isCorrect, responseTimeMs);
        momentum.calculateMomentumScore();
        
        SessionMomentum savedMomentum = sessionMomentumRepository.save(momentum);
        log.debug("Recorded answer: questions={}, correct={}, accuracy={}%, momentum={}", 
            savedMomentum.getQuestionsAnswered(), savedMomentum.getCorrectAnswers(), 
            savedMomentum.getAccuracyRate(), savedMomentum.getMomentumScore());
        
        return savedMomentum;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SessionMomentum> getSessionMomentum(String sessionId) {
        log.debug("Getting session momentum for session {}", sessionId);
        return sessionMomentumRepository.findBySessionId(sessionId);
    }

    @Override
    @Transactional
    public SessionMomentum calculateMomentum(String sessionId) {
        log.debug("Calculating momentum for session {}", sessionId);
        
        SessionMomentum momentum = sessionMomentumRepository
            .findBySessionId(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session momentum not found: " + sessionId));
        
        momentum.calculateMomentumScore();
        
        SessionMomentum savedMomentum = sessionMomentumRepository.save(momentum);
        log.debug("Calculated momentum score: {}", savedMomentum.getMomentumScore());
        
        return savedMomentum;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInFlow(String sessionId) {
        log.debug("Checking if session {} is in flow", sessionId);
        
        SessionMomentum momentum = sessionMomentumRepository
            .findBySessionId(sessionId)
            .orElse(null);
            
        if (momentum == null) {
            return false;
        }
        
        boolean inFlow = momentum.isInFlow();
        log.debug("Flow check: momentum={}, in flow={}", momentum.getMomentumScore(), inFlow);
        
        return inFlow;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStruggling(String sessionId) {
        log.debug("Checking if session {} is struggling", sessionId);
        
        SessionMomentum momentum = sessionMomentumRepository
            .findBySessionId(sessionId)
            .orElse(null);
            
        if (momentum == null) {
            return false;
        }
        
        boolean struggling = momentum.isStruggling();
        log.debug("Struggling check: momentum={}, struggling={}", momentum.getMomentumScore(), struggling);
        
        return struggling;
    }

    @Override
    @Transactional(readOnly = true)
    public double getMomentumScore(String sessionId) {
        log.debug("Getting momentum score for session {}", sessionId);
        
        SessionMomentum momentum = sessionMomentumRepository
            .findBySessionId(sessionId)
            .orElse(null);
            
        if (momentum == null) {
            return 0.0;
        }
        
        return momentum.getMomentumScore().doubleValue();
    }

    @Override
    @Transactional(readOnly = true)
    public double getSessionAccuracy(String sessionId) {
        log.debug("Getting session accuracy for session {}", sessionId);
        
        SessionMomentum momentum = sessionMomentumRepository
            .findBySessionId(sessionId)
            .orElse(null);
            
        if (momentum == null) {
            return 0.0;
        }
        
        return momentum.getAccuracyRate();
    }

    @Override
    @Transactional(readOnly = true)
    public long getAverageResponseTime(String sessionId) {
        log.debug("Getting average response time for session {}", sessionId);
        
        SessionMomentum momentum = sessionMomentumRepository
            .findBySessionId(sessionId)
            .orElse(null);
            
        if (momentum == null) {
            return 0L;
        }
        
        return momentum.getAverageResponseTimeMs();
    }
}

