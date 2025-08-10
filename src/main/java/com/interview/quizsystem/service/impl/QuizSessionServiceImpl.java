package com.interview.quizsystem.service.impl;

import com.interview.quizsystem.model.*;
import com.interview.quizsystem.model.entity.Question;
import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.model.entity.User;
import com.interview.quizsystem.service.*;
import com.interview.quizsystem.dto.AnswerFeedback;
import com.interview.quizsystem.repository.QuizSessionRepository;
import com.interview.quizsystem.repository.UserAnswerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizSessionServiceImpl implements QuizSessionService {

    private final QuestionGeneratorService questionGeneratorService;
    private final QuestionBankService questionBankService;
    private final AnswerEvaluationService answerEvaluationService;
    private final QuizSessionRepository quizSessionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final ProgressService progressService;
    private final TopicService topicService;
    private final UserService userService;

    private Question convertToEntity(QuestionDTO dto) {
        return Question.builder()
                .id(dto.getId())
                .questionText(dto.getContent())
                .questionType(dto.getType())
                .options(dto.getOptions())
                .difficulty(dto.getDifficulty())
                .expectedAnswer(dto.getCorrectAnswer())
                .explanation(dto.getExplanation())
                .sourceFile(dto.getSourceFile())
                .sourceContent(dto.getSourceContent())
                .questionBank(dto.getQuestionBankId() == null ? null : com.interview.quizsystem.model.entity.QuestionBank.builder().id(dto.getQuestionBankId()).build())
                .build();
    }

    private QuestionDTO convertToDTO(Question entity) {
        return QuestionDTO.builder()
                .id(entity.getId())
                .content(entity.getQuestionText())
                .type(entity.getQuestionType())
                .options(entity.getOptions())
                .correctAnswer(entity.getExpectedAnswer())
                .explanation(entity.getExplanation())
                .topic(entity.getTopic() != null ? entity.getTopic().getName() : null)
                .difficulty(entity.getDifficulty())
                .sourceFile(entity.getSourceFile())
                .sourceContent(entity.getSourceContent())
                .build();
    }

    @Override
    @Transactional
    public QuizSession startSession(String topic, Difficulty difficulty, int questionCount) {
        log.debug("Selecting {} questions for topic: {}, difficulty: {} from question bank", questionCount, topic, difficulty);
        List<QuestionDTO> questions = questionBankService.getQuestionsByTopicAndDifficulty(
                topicService.getTopicByName(topic), difficulty, questionCount);
        
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
        List<QuestionDTO> questionsCopy = questions.stream()
            .map(q -> QuestionDTO.builder()
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
                .sourceContent("")
                .build())
            .collect(Collectors.toList());

        // Convert DTOs to entities for storage
        List<Question> storedQuestions = questions.stream()
            .map(this::convertToEntity)
            .collect(Collectors.toList());
        
        String sessionId = UUID.randomUUID().toString();
        log.debug("Creating new session: {}", sessionId);
        
        QuizSession session = QuizSession.builder()
                .id(sessionId)
                .questions(questions)
                .visibleQuestions(questionsCopy)
                .storedQuestions(storedQuestions)
                .answers(new ArrayList<>())
                .topic(topic)
                .difficulty(difficulty)
                .startTime(LocalDateTime.now())
                .status(SessionStatus.IN_PROGRESS)
                .score(0.0)
                .build();

        session = quizSessionRepository.save(session);
        log.debug("Session created and stored: {}", session.getId());
        
        // Return session with visible questions only
        return session.toBuilder()
                .questions(questionsCopy)
                .build();
    }

    @Override
    @Transactional
    public QuizSession submitAnswer(String sessionId, String questionId, String answer) {
        QuizSession session = quizSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session is not in progress");
        }

        // Convert stored questions to DTOs
        List<QuestionDTO> questions = session.getStoredQuestions().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        session.setQuestions(questions);

        List<QuestionDTO> visibleQuestions = questions.stream()
            .map(q -> q.toBuilder()
                .correctAnswer(null) // Hide correct answer initially
                .sourceContent("")
                .build())
            .collect(Collectors.toList());
        session.setVisibleQuestions(visibleQuestions);

        // Find the question in both questions and visibleQuestions lists
        QuestionDTO question = session.getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        QuestionDTO visibleQuestion = session.getVisibleQuestions().stream()
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
                .quizSession(session)
                .build();

        userAnswer = userAnswerRepository.save(userAnswer);
        session.getAnswers().add(userAnswer);
        
        // Update session score
        session.setScore(calculateScore(session));
        session = quizSessionRepository.save(session);

        // Update topic progress
        User user = userService.getCurrentUser();
        Topic topic = topicService.getTopicByName(session.getTopic());
        progressService.updateProgress(user, topic, session.getDifficulty(), isCorrect);

        return session;
    }

    @Override
    @Transactional(readOnly = true)
    public QuizSession getSession(String sessionId) {
        QuizSession session = quizSessionRepository.findById(sessionId).orElse(null);
        if (session != null) {
            // Return session with visible questions
            return session.toBuilder()
                    .questions(session.getVisibleQuestions())
                    .build();
        }
        return null;
    }

    @Override
    @Transactional
    public QuizSession endSession(String sessionId) {
        final QuizSession session = quizSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new IllegalStateException("Session is already completed");
        }

        // Convert stored questions to DTOs
        List<QuestionDTO> questions = session.getStoredQuestions().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        session.setQuestions(questions);

        // Reveal all correct answers and restore original options
        session.getVisibleQuestions().forEach(visibleQuestion -> {
            QuestionDTO originalQuestion = questions.stream()
                .filter(q -> q.getId().equals(visibleQuestion.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Question not found"));
                
            visibleQuestion.setCorrectAnswer(originalQuestion.getCorrectAnswer());
            visibleQuestion.setSourceContent("");
            if (visibleQuestion.getType() == QuestionType.MULTIPLE_CHOICE) {
                visibleQuestion.setOptions(new ArrayList<>(originalQuestion.getOptions()));
            }
        });

        session.setStatus(SessionStatus.COMPLETED);
        session.setEndTime(LocalDateTime.now());

        // Add empty answers for unanswered questions
        questions.forEach(question -> {
            if (session.getAnswers().stream()
                    .noneMatch(a -> a.getQuestionId().equals(question.getId()))) {
                UserAnswer emptyAnswer = UserAnswer.builder()
                    .questionId(question.getId())
                    .answer("")
                    .correct(false)
                    .answeredAt(session.getEndTime())
                    .quizSession(session)
                    .build();
                emptyAnswer = userAnswerRepository.save(emptyAnswer);
                session.getAnswers().add(emptyAnswer);

                // Update topic progress for unanswered questions
                User user = userService.getCurrentUser();
                Topic topic = topicService.getTopicByName(session.getTopic());
                progressService.updateProgress(user, topic, session.getDifficulty(), false);
            }
        });

        // Calculate final score
        session.setScore(calculateScore(session));
        QuizSession savedSession = quizSessionRepository.save(session);
        
        // Return session with visible questions
        return savedSession.toBuilder()
                .questions(session.getVisibleQuestions())
                .build();
    }

    private boolean validateAnswer(QuestionDTO question, String answer) {
        log.info("Validating answer for question type: {}", question.getType());
        
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
                log.debug("Validating multiple choice answer");
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
                log.debug("Validating true/false answer");
                // Case-insensitive comparison for true/false
                String normalizedAnswer = userAnswer.toLowerCase();
                String normalizedCorrect = question.getCorrectAnswer().trim().toLowerCase();
                if (!normalizedAnswer.equals("true") && !normalizedAnswer.equals("false")) {
                    throw new IllegalArgumentException("True/False answer must be 'true' or 'false'");
                }
                return normalizedAnswer.equals(normalizedCorrect);

            case SHORT_ANSWER:
            case SCENARIO_BASED:
                log.info("Using AI evaluation for {} answer", question.getType());
                // Use AI-based evaluation for text answers
                try {
                    AnswerFeedback feedback = answerEvaluationService.evaluateAnswer(question, userAnswer);
                    log.info("AI evaluation completed with similarity score: {}", feedback.getSimilarityScore());
                    
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