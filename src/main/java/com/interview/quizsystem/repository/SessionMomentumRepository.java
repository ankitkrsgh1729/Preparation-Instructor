package com.interview.quizsystem.repository;

import com.interview.quizsystem.model.entity.SessionMomentum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionMomentumRepository extends JpaRepository<SessionMomentum, Long> {
    
    Optional<SessionMomentum> findBySessionId(String sessionId);
    
    Optional<SessionMomentum> findBySessionIdAndUserId(String sessionId, Long userId);
}

