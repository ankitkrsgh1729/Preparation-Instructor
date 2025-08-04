package com.interview.quizsystem.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_answers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_session_id", nullable = false)
    private QuizSession quizSession;
} 