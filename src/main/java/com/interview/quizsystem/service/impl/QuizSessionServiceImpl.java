package com.interview.quizsystem.service.impl;

import com.interview.quizsystem.model.*;
import com.interview.quizsystem.service.QuizSessionService;
import com.interview.quizsystem.service.QuestionGeneratorService;
import com.interview.quizsystem.service.AnswerEvaluationService;
import com.interview.quizsystem.dto.AnswerFeedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizSessionServiceImpl implements QuizSessionService {

    private final QuestionGeneratorService questionGeneratorService;
    private final AnswerEvaluationService answerEvaluationService;
    private final Map<String, QuizSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public QuizSession startSession(String topic, Difficulty difficulty, int questionCount) {
        log.debug("Generating {} questions for topic: {}, difficulty: {}", questionCount, topic, difficulty);
        List<Question> questions = questionGeneratorService.generateQuestions(topic, questionCount, difficulty);
        
        // Validate questions have correct answers
        questions.forEach(q -> {
            if (q.getCorrectAnswer() == null) {
                log.error("Question generated without correct answer. Content: {}", q.getContent());
                throw new IllegalStateException("Question generated without correct answer: " + q.getContent());
            }
            log.debug("Question generated - ID: {}, Type: {}, Has correct answer: {}", 
                q.getId(), q.getType(), q.getCorrectAnswer() != null);
        });

        // Create a deep copy of questions for the response
        List<Question> questionsCopy = questions.stream()
            .map(q -> {
                Question copy = Question.builder()
                    .id(q.getId())
                    .content(q.getContent())
                    .type(q.getType())
                    .options(q.getType() == QuestionType.MULTIPLE_CHOICE ? 
                        new ArrayList<>(q.getOptions()) : q.getOptions())
                    .correctAnswer(null) // Hide correct answer initially
                    .explanation(q.getExplanation())
                    .topic(q.getTopic())
                    .difficulty(q.getDifficulty())
                    .sourceFile(q.getSourceFile())
                    .sourceContent(q.getSourceContent())
                    .build();
                log.debug("Created visible copy of question - ID: {}, Type: {}", copy.getId(), copy.getType());
                return copy;
            })
            .collect(Collectors.toList());

        // Store original questions with answers
        List<Question> originalQuestions = questions.stream()
            .map(q -> Question.builder()
                .id(q.getId())
                .content(q.getContent())
                .type(q.getType())
                .options(q.getType() == QuestionType.MULTIPLE_CHOICE ? 
                    new ArrayList<>(q.getOptions()) : q.getOptions())
                .correctAnswer(q.getCorrectAnswer()) // Keep correct answer
                .explanation(q.getExplanation())
                .topic(q.getTopic())
                .difficulty(q.getDifficulty())
                .sourceFile(q.getSourceFile())
                .sourceContent(q.getSourceContent())
                .build())
            .collect(Collectors.toList());

        // Randomize MCQ options in the copy
        questionsCopy.forEach(q -> {
            if (q.getType() == QuestionType.MULTIPLE_CHOICE && q.getOptions() != null) {
                Collections.shuffle(q.getOptions());
                log.debug("Shuffled options for MCQ - ID: {}", q.getId());
            }
        });
        
        String sessionId = UUID.randomUUID().toString();
        log.debug("Creating new session: {}", sessionId);
        
        // Create session with both sets of questions
        QuizSession session = QuizSession.builder()
                .id(sessionId)
                .questions(originalQuestions) // Store original questions with answers
                .visibleQuestions(questionsCopy) // Store questions for display
                .answers(new ArrayList<>())
                .topic(topic)
                .difficulty(difficulty)
                .startTime(LocalDateTime.now())
                .status(SessionStatus.IN_PROGRESS)
                .score(0.0)
                .build();

        activeSessions.put(session.getId(), session);
        log.debug("Session created and stored: {}", session.getId());
        
        // Return session with visible questions only
        QuizSession responseSession = session.toBuilder()
                .questions(questionsCopy)
                .build();
        
        return responseSession;
    }

    @Override
    public QuizSession submitAnswer(String sessionId, String questionId, String answer) {
        QuizSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session is not in progress");
        }

        // Find the question in both questions and visibleQuestions lists
        Question question = session.getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        Question visibleQuestion = session.getVisibleQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Question not found in visible questions: " + questionId));

        // Validate and evaluate the answer
        boolean isCorrect = validateAnswer(question, answer);
        
        // Since validateAnswer sets the feedback on the question, copy it to visibleQuestion
        visibleQuestion.setAnswerFeedback(question.getAnswerFeedback());
        visibleQuestion.setCorrectAnswer(question.getCorrectAnswer()); // Include correct answer after submission

        // Record the answer
        UserAnswer userAnswer = UserAnswer.builder()
                .questionId(questionId)
                .answer(answer)
                .correct(isCorrect)
                .answeredAt(LocalDateTime.now())
                .build();

        session.getAnswers().add(userAnswer);
        
        // Update session score
        session.setScore(calculateScore(session));

        return session;
    }

    @Override
    public QuizSession getSession(String sessionId) {
        QuizSession session = activeSessions.get(sessionId);
        if (session != null) {
            // Return session with visible questions
            return session.toBuilder()
                    .questions(session.getVisibleQuestions())
                    .build();
        }
        return null;
    }

    @Override
    public QuizSession endSession(String sessionId) {
        QuizSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new IllegalStateException("Session is already completed");
        }

        // Reveal all correct answers and restore original options
        session.getVisibleQuestions().forEach(visibleQuestion -> {
            Question originalQuestion = session.getQuestions().stream()
                .filter(q -> q.getId().equals(visibleQuestion.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Question not found"));
                
            visibleQuestion.setCorrectAnswer(originalQuestion.getCorrectAnswer());
            if (visibleQuestion.getType() == QuestionType.MULTIPLE_CHOICE) {
                visibleQuestion.setOptions(new ArrayList<>(originalQuestion.getOptions()));
            }
        });

        session.setStatus(SessionStatus.COMPLETED);
        session.setEndTime(LocalDateTime.now());

        // Add empty answers for unanswered questions
        session.getQuestions().forEach(question -> {
            if (session.getAnswers().stream()
                    .noneMatch(a -> a.getQuestionId().equals(question.getId()))) {
                session.getAnswers().add(UserAnswer.builder()
                    .questionId(question.getId())
                    .answer("")
                    .correct(false)
                    .answeredAt(session.getEndTime())
                    .build());
            }
        });

        // Calculate final score
        session.setScore(calculateScore(session));
        
        // Return session with visible questions
        return session.toBuilder()
                .questions(session.getVisibleQuestions())
                .build();
    }

    private boolean validateAnswer(Question question, String answer) {
        if (question.getCorrectAnswer() == null) {
            log.error("Question {} has no correct answer defined", question.getId());
            throw new IllegalStateException("Question has no correct answer defined");
        }

        if (answer == null || answer.trim().isEmpty()) {
            return false;
        }

        String userAnswer = answer.trim();

        switch (question.getType()) {
            case MULTIPLE_CHOICE:
                // For MCQ, answer must match exactly one of the options
                if (question.getOptions() == null || question.getOptions().isEmpty()) {
                    log.error("MCQ question {} has no options defined", question.getId());
                    throw new IllegalStateException("Multiple choice question has no options defined");
                }
                if (!question.getOptions().contains(userAnswer)) {
                    throw new IllegalArgumentException("Invalid option selected");
                }
                return userAnswer.equals(question.getCorrectAnswer().trim());

            case TRUE_FALSE:
                // Case-insensitive comparison for true/false
                String normalizedAnswer = userAnswer.toLowerCase();
                String normalizedCorrect = question.getCorrectAnswer().trim().toLowerCase();
                if (!normalizedAnswer.equals("true") && !normalizedAnswer.equals("false")) {
                    throw new IllegalArgumentException("True/False answer must be 'true' or 'false'");
                }
                return normalizedAnswer.equals(normalizedCorrect);

            case SHORT_ANSWER:
            case SCENARIO_BASED:
                // Use AI-based evaluation for text answers
                try {
                    AnswerFeedback feedback = answerEvaluationService.evaluateAnswer(question, userAnswer);
                    
                    // Store the feedback in the question for frontend display
                    question.setAnswerFeedback(feedback);
                    
                    // Consider the answer correct if similarity score is high enough
                    return feedback.getSimilarityScore() >= 80.0;
                } catch (Exception e) {
                    log.error("Failed to evaluate answer using AI, falling back to basic comparison", e);
                    return userAnswer.equalsIgnoreCase(question.getCorrectAnswer().trim());
                }

            default:
                throw new IllegalStateException("Unsupported question type: " + question.getType());
        }
    }

    private double calculateScore(QuizSession session) {
        if (session.getQuestions().isEmpty()) {
            return 0.0;
        }

        long correctAnswers = session.getAnswers().stream()
                .filter(UserAnswer::isCorrect)
                .count();

        // If session is completed, score is based on all questions
        // If in progress, score is based on answered questions only
        int totalQuestions = session.getStatus() == SessionStatus.COMPLETED ? 
            session.getQuestions().size() : 
            Math.max(1, session.getAnswers().size());

        return (double) correctAnswers / totalQuestions * 100;
    }
} 