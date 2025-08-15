package com.interview.quizsystem.repository;

import com.interview.quizsystem.model.entity.SpacedRepetitionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpacedRepetitionDataRepository extends JpaRepository<SpacedRepetitionData, Long> {
    
    Optional<SpacedRepetitionData> findByUserIdAndQuestionId(Long userId, String questionId);
    
    @Query("SELECT srd FROM SpacedRepetitionData srd WHERE srd.user.id = :userId AND srd.nextReviewDate <= :now")
    List<SpacedRepetitionData> findDueQuestionsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT srd FROM SpacedRepetitionData srd WHERE srd.user.id = :userId AND srd.nextReviewDate <= :now ORDER BY srd.nextReviewDate ASC")
    List<SpacedRepetitionData> findDueQuestionsByUserIdOrdered(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(srd) FROM SpacedRepetitionData srd WHERE srd.user.id = :userId AND srd.nextReviewDate <= :now")
    Long countDueQuestionsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT srd FROM SpacedRepetitionData srd WHERE srd.user.id = :userId AND srd.questionId IN :questionIds")
    List<SpacedRepetitionData> findByUserIdAndQuestionIds(@Param("userId") Long userId, @Param("questionIds") List<String> questionIds);
}

