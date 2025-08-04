package com.interview.quizsystem.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "topic_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "topic_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "last_attempt_date")
    private LocalDateTime lastAttemptDate;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "overall_score")
    private Double overallScore;

    @Column(name = "questions_attempted")
    private Integer questionsAttempted;

    @Column(name = "questions_correct")
    private Integer questionsCorrect;

    @OneToMany(mappedBy = "topicProgress", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DifficultyProgress> difficultyProgress = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        startDate = LocalDateTime.now();
        active = true;
        questionsAttempted = 0;
        questionsCorrect = 0;
        overallScore = 0.0;
    }
} 