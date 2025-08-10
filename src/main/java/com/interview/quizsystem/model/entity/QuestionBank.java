package com.interview.quizsystem.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.QuestionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "question_bank")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBank {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private Difficulty difficulty;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @ElementCollection
    @CollectionTable(name = "question_bank_options", joinColumns = @JoinColumn(name = "question_bank_id"))
    @Column(name = "option_value")
    private List<String> options;

    @Column(name = "expected_answer", columnDefinition = "TEXT")
    private String expectedAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "source_file")
    private String sourceFile;

    @Column(name = "source_content", columnDefinition = "TEXT")
    @JsonIgnore
    private String sourceContent;

    @Column(name = "content_hash", length = 128)
    private String contentHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}


