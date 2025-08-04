package com.interview.quizsystem.repository;

import com.interview.quizsystem.model.entity.AIModelError;
import com.interview.quizsystem.model.entity.AIModelUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIModelErrorRepository extends JpaRepository<AIModelError, Long> {
    List<AIModelError> findByUsage(AIModelUsage usage);
} 