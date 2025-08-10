package com.interview.quizsystem.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "topic_processing_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicProcessingHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "content_hash", length = 128, nullable = false)
    private String contentHash;

    @Column(name = "questions_generated")
    private Integer questionsGenerated;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        processedAt = LocalDateTime.now();
    }
}


