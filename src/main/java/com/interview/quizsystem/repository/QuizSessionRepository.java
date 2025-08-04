package com.interview.quizsystem.repository;

import com.interview.quizsystem.model.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, String> {
} 