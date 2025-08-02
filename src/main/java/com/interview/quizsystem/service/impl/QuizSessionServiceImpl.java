package com.interview.quizsystem.service.impl;

import com.interview.quizsystem.model.QuizSession;
import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.Question;
import com.interview.quizsystem.model.SessionStatus;
import com.interview.quizsystem.model.UserAnswer;
import com.interview.quizsystem.service.QuizSessionService;
import com.interview.quizsystem.service.QuestionGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizSessionServiceImpl implements QuizSessionService {

    private final QuestionGeneratorService questionGeneratorService;
    private final Map<String, QuizSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public QuizSession startSession(String topic, Difficulty difficulty, int questionCount) {
        List<Question> questions = questionGeneratorService.generateQuestions(topic, questionCount, difficulty);
        
        QuizSession session = QuizSession.builder()
                .id(UUID.randomUUID().toString())
                .questions(questions)
                .answers(new ArrayList<>())
                .topic(topic)
                .difficulty(difficulty)
                .startTime(LocalDateTime.now())
                .status(SessionStatus.IN_PROGRESS)
                .build();

        activeSessions.put(session.getId(), session);
        return session;
    }

    @Override
    public QuizSession submitAnswer(String sessionId, String questionId, String answer) {
        QuizSession session = getSession(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new RuntimeException("Session is not in progress");
        }

        Question question = session.getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Question not found: " + questionId));

        UserAnswer userAnswer = UserAnswer.builder()
                .questionId(questionId)
                .answer(answer)
                .correct(answer.equalsIgnoreCase(question.getCorrectAnswer()))
                .answeredAt(LocalDateTime.now())
                .build();

        session.getAnswers().add(userAnswer);

        // Check if all questions are answered
        if (session.getAnswers().size() == session.getQuestions().size()) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setEndTime(LocalDateTime.now());
            session.setScore(calculateScore(session));
        }

        return session;
    }

    @Override
    public QuizSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    @Override
    public void endSession(String sessionId) {
        QuizSession session = getSession(sessionId);
        if (session != null) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setEndTime(LocalDateTime.now());
            session.setScore(calculateScore(session));
        }
    }

    private double calculateScore(QuizSession session) {
        if (session.getAnswers().isEmpty()) {
            return 0.0;
        }

        long correctAnswers = session.getAnswers().stream()
                .filter(UserAnswer::isCorrect)
                .count();

        return (double) correctAnswers / session.getQuestions().size() * 100;
    }
} 