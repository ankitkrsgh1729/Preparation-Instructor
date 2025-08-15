package com.interview.quizsystem.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_momentum")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMomentum {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "momentum_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal momentumScore = BigDecimal.ZERO;

    @Column(name = "response_time_trend", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal responseTimeTrend = BigDecimal.ZERO;

    @Column(name = "accuracy_trend", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal accuracyTrend = BigDecimal.ZERO;

    @Column(name = "questions_answered")
    @Builder.Default
    private Integer questionsAnswered = 0;

    @Column(name = "correct_answers")
    @Builder.Default
    private Integer correctAnswers = 0;

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
        if (questionsAnswered == 0) return 0.0;
        return (double) correctAnswers / questionsAnswered * 100.0;
    }

    public void recordAnswer(boolean isCorrect, long responseTimeMs) {
        questionsAnswered++;
        if (isCorrect) {
            correctAnswers++;
        }
        
        // Update average response time
        if (averageResponseTimeMs == 0) {
            averageResponseTimeMs = responseTimeMs;
        } else {
            averageResponseTimeMs = (averageResponseTimeMs + responseTimeMs) / 2;
        }
    }

    public void calculateMomentumScore() {
        // Momentum score combines accuracy and response time trends
        // Higher accuracy and faster response times = higher momentum
        double accuracyWeight = 0.7;
        double responseTimeWeight = 0.3;
        
        double accuracyComponent = getAccuracyRate() * accuracyWeight;
        double responseTimeComponent = Math.max(0, 100 - (averageResponseTimeMs / 1000.0)) * responseTimeWeight;
        
        momentumScore = BigDecimal.valueOf(accuracyComponent + responseTimeComponent);
    }

    public boolean isInFlow() {
        return momentumScore.compareTo(BigDecimal.valueOf(70)) >= 0;
    }

    public boolean isStruggling() {
        return momentumScore.compareTo(BigDecimal.valueOf(30)) <= 0;
    }
}

