package com.interview.quizsystem.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "spaced_repetition_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpacedRepetitionData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @Column(name = "repetition_number")
    @Builder.Default
    private Integer repetitionNumber = 0;

    @Column(name = "ease_factor", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal easeFactor = new BigDecimal("2.5");

    @Column(name = "interval_days")
    @Builder.Default
    private Integer intervalDays = 0;

    @Column(name = "next_review_date", nullable = false)
    private LocalDateTime nextReviewDate;

    @Column(name = "last_review_date")
    private LocalDateTime lastReviewDate;

    @Column(name = "consecutive_correct")
    @Builder.Default
    private Integer consecutiveCorrect = 0;

    @Column(name = "consecutive_incorrect")
    @Builder.Default
    private Integer consecutiveIncorrect = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isDueForReview() {
        return LocalDateTime.now().isAfter(nextReviewDate) || LocalDateTime.now().isEqual(nextReviewDate);
    }

    public void updateEaseFactor(boolean isCorrect) {
        if (isCorrect) {
            // Increase ease factor by 0.1 for correct answers
            easeFactor = easeFactor.add(new BigDecimal("0.1"));
            consecutiveCorrect++;
            consecutiveIncorrect = 0;
        } else {
            // Decrease ease factor by 0.2 for incorrect answers (minimum 1.3)
            easeFactor = easeFactor.subtract(new BigDecimal("0.2"));
            if (easeFactor.compareTo(new BigDecimal("1.3")) < 0) {
                easeFactor = new BigDecimal("1.3");
            }
            consecutiveIncorrect++;
            consecutiveCorrect = 0;
        }
    }

    public void calculateNextInterval() {
        repetitionNumber++;
        
        if (repetitionNumber == 1) {
            // First repetition: 1 day
            intervalDays = 1;
        } else if (repetitionNumber == 2) {
            // Second repetition: 6 days
            intervalDays = 6;
        } else {
            // Subsequent repetitions: interval * ease factor
            intervalDays = (int) Math.round(intervalDays * easeFactor.doubleValue());
        }
        
        nextReviewDate = LocalDateTime.now().plusDays(intervalDays);
        lastReviewDate = LocalDateTime.now();
    }
}

