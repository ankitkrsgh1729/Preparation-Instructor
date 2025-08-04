package com.interview.quizsystem.model.entity;

import com.interview.quizsystem.model.Difficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "difficulty_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"topic_progress_id", "difficulty"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifficultyProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_progress_id", nullable = false)
    private TopicProgress topicProgress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(name = "score")
    private Double score;

    @Column(name = "questions_attempted")
    private Integer questionsAttempted;

    @Column(name = "questions_correct")
    private Integer questionsCorrect;

    @Column(name = "last_attempt_date")
    private LocalDateTime lastAttemptDate;

    @PrePersist
    protected void onCreate() {
        questionsAttempted = 0;
        questionsCorrect = 0;
        score = 0.0;
        lastAttemptDate = LocalDateTime.now();
    }
} 