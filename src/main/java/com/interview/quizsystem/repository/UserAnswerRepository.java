package com.interview.quizsystem.repository;

import com.interview.quizsystem.model.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, String> {
    List<UserAnswer> findByQuizSessionId(String quizSessionId);
} 