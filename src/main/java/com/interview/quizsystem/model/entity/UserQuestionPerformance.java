package com.interview.quizsystem.model.entity;

import com.interview.quizsystem.model.Difficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_question_performance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQuestionPerformance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(name = "total_attempts")
    @Builder.Default
    private Integer totalAttempts = 0;

    @Column(name = "correct_attempts")
    @Builder.Default
    private Integer correctAttempts = 0;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @Column(name = "average_response_time_ms")
    @Builder.Default
    private Long averageResponseTimeMs = 0L;

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

    public double getAccuracyRate() {
        if (totalAttempts == 0) return 0.0;
        return (double) correctAttempts / totalAttempts * 100.0;
    }

    public void recordAttempt(boolean isCorrect, long responseTimeMs) {
        totalAttempts++;
        if (isCorrect) {
            correctAttempts++;
        }
        lastAttemptedAt = LocalDateTime.now();
        
        // Update average response time
        if (averageResponseTimeMs == 0) {
            averageResponseTimeMs = responseTimeMs;
        } else {
            averageResponseTimeMs = (averageResponseTimeMs + responseTimeMs) / 2;
        }
    }
}

