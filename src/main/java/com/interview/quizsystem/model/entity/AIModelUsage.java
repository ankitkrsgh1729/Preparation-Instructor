package com.interview.quizsystem.model.entity;

import com.interview.quizsystem.model.AIOperationType;
import com.interview.quizsystem.model.AIUsageStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_model_usage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIModelUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "operation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AIOperationType operationType;

    @Column(name = "model_provider", nullable = false, length = 50)
    private String modelProvider;

    @Column(name = "model_name", nullable = false, length = 50)
    private String modelName;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "cost_in_usd", precision = 10, scale = 6)
    private BigDecimal costInUsd;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AIUsageStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
} 