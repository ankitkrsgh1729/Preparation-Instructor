package com.interview.quizsystem.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.interview.quizsystem.dto.AnswerFeedback;
import com.interview.quizsystem.model.AIOperationType;
import com.interview.quizsystem.model.AIUsageStatus;
import com.interview.quizsystem.model.QuestionDTO;
import com.interview.quizsystem.model.entity.AIModelError;
import com.interview.quizsystem.model.entity.AIModelUsage;
import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.repository.AIModelErrorRepository;
import com.interview.quizsystem.repository.AIModelUsageRepository;
import com.interview.quizsystem.service.AnswerEvaluationService;
import com.interview.quizsystem.service.TopicService;
import com.interview.quizsystem.service.UserService;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIAnswerEvaluationService implements AnswerEvaluationService {

    private final ObjectMapper objectMapper;
    private final AIModelUsageRepository aiModelUsageRepository;
    private final AIModelErrorRepository aiModelErrorRepository;
    private final TopicService topicService;
    private final UserService userService;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.temperature}")
    private double temperature;

    @Value("${openai.max-tokens}")
    private int maxTokens;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final BigDecimal COST_PER_TOKEN = new BigDecimal("0.000002"); // $0.002 per 1K tokens

    @Override
    @Transactional
    public AnswerFeedback evaluateAnswer(QuestionDTO question, String userAnswer) {
        log.info("Starting OpenAI evaluation for question type: {}, topic: {}", question.getType(), question.getTopic());
        long startTime = System.currentTimeMillis();
        AIModelUsage usage = null;
        
        try {
            String prompt = buildPrompt(question, userAnswer);
            log.debug("Sending prompt to OpenAI: {}", prompt);
            String response = callOpenAI(prompt);
            log.debug("Received response from OpenAI: {}", response);
            
            // Calculate tokens (approximate)
            int promptTokens = prompt.length() / 4; // rough estimate
            int responseTokens = response.length() / 4;
            int totalTokens = promptTokens + responseTokens;
            
            log.info("Calculated tokens - prompt: {}, response: {}, total: {}", promptTokens, responseTokens, totalTokens);
            
            // Create usage record
            usage = AIModelUsage.builder()
                    .topic(topicService.getTopicByName(question.getTopic()))
                    .user(userService.getCurrentUser())
                    .operationType(AIOperationType.ANSWER_EVALUATION)
                    .modelProvider("OPENAI")
                    .modelName(model)
                    .tokensUsed(totalTokens)
                    .costInUsd(COST_PER_TOKEN.multiply(new BigDecimal(totalTokens)).setScale(6, RoundingMode.HALF_UP))
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .status(AIUsageStatus.SUCCESS)
                    .build();
            
            usage = aiModelUsageRepository.save(usage);
            log.info("Saved AI usage record with ID: {}", usage.getId());
            
            AnswerFeedback feedback = parseResponse(response);
            feedback.setCorrectAnswer(question.getCorrectAnswer());
            return feedback;
            
        } catch (Exception e) {
            log.error("Failed to evaluate answer using OpenAI: {}", e.getMessage(), e);
            
            // Create failed usage record if not already created
            if (usage == null) {
                usage = AIModelUsage.builder()
                        .topic(topicService.getTopicByName(question.getTopic()))
                        .user(userService.getCurrentUser())
                        .operationType(AIOperationType.ANSWER_EVALUATION)
                        .modelProvider("OPENAI")
                        .modelName(model)
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .status(AIUsageStatus.FAILED)
                        .build();
                
                usage = aiModelUsageRepository.save(usage);
                log.info("Saved failed AI usage record with ID: {}", usage.getId());
            }
            
            // Log error
            AIModelError error = AIModelError.builder()
                    .usage(usage)
                    .errorCode(e.getClass().getSimpleName())
                    .errorMessage(e.getMessage())
                    .build();
            
            aiModelErrorRepository.save(error);
            log.info("Saved AI error record for usage ID: {}", usage.getId());
            
            // Fallback to basic comparison
            boolean isCorrect = userAnswer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
            return AnswerFeedback.builder()
                    .correct(isCorrect)
                    .similarityScore(isCorrect ? 100.0 : 0.0)
                    .feedback("Unable to provide detailed feedback. Using basic comparison.")
                    .correctAnswer(question.getCorrectAnswer())
                    .build();
        }
    }

    private String buildPrompt(QuestionDTO question, String userAnswer) {
        return String.format("""
            You are an expert evaluator for technical interview questions.
            Your goal is to provide detailed, educational feedback that helps the user learn and improve.
            
            Question: %s
            
            Correct Answer: %s
            
            User's Answer: %s
            
            Question's Explanation: %s
            
            Please evaluate the user's answer and provide detailed feedback in the following JSON format:
            {
                "correct": boolean,
                "similarityScore": number (0-100),
                "feedback": "Detailed explanation of the evaluation, including why the answer was correct/incorrect",
                "correctParts": "Specific concepts and points that were correctly addressed",
                "incorrectParts": "Key concepts that were missing or incorrectly explained",
                "improvementSuggestions": "Detailed suggestions for improvement, including examples and explanations",
                "conceptualUnderstanding": "Assessment of the user's understanding of the core concepts"
            }
            
            Consider:
            1. Technical accuracy and precision
            2. Completeness of the answer
            3. Key concepts covered vs missing
            4. Depth of understanding shown
            5. Practical application of concepts
            6. Clarity of explanation
            
            Provide specific examples and explanations in your feedback to help the user understand where they can improve.
            Focus on being educational and constructive.
            """,
            question.getContent(),
            question.getCorrectAnswer(),
            userAnswer,
            question.getExplanation()
        );
    }

    private String callOpenAI(String prompt) throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        
        var messages = requestBody.putArray("messages");
        var systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a technical interviewer evaluating answers.");
        
        var userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        HttpResponse<String> response = Unirest.post(OPENAI_API_URL)
                .body(requestBody.toString())
                .asString();

        if (response.getStatus() != 200) {
            throw new RuntimeException("OpenAI API call failed with status: " + response.getStatus());
        }

        JsonNode jsonResponse = objectMapper.readTree(response.getBody());
        return jsonResponse
                .get("choices")
                .get(0)
                .get("message")
                .get("content")
                .asText();
    }

    private AnswerFeedback parseResponse(String response) throws Exception {
        // Clean the response string to ensure it's valid JSON
        response = response.replaceAll("```json\\s*", "")
                         .replaceAll("```\\s*$", "")
                         .trim();

        JsonNode jsonNode = objectMapper.readTree(response);
        
        return AnswerFeedback.builder()
                .correct(jsonNode.get("correct").asBoolean())
                .similarityScore(jsonNode.get("similarityScore").asDouble())
                .feedback(jsonNode.get("feedback").asText())
                .correctParts(jsonNode.get("correctParts").asText())
                .incorrectParts(jsonNode.get("incorrectParts").asText())
                .improvementSuggestions(jsonNode.get("improvementSuggestions").asText())
                .build();
    }
} 