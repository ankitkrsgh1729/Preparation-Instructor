package com.interview.quizsystem.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.interview.quizsystem.dto.AnswerFeedback;
import com.interview.quizsystem.model.QuestionDTO;
import com.interview.quizsystem.service.AnswerEvaluationService;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIAnswerEvaluationService implements AnswerEvaluationService {

    private final ObjectMapper objectMapper;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.temperature}")
    private double temperature;

    @Value("${openai.max-tokens}")
    private int maxTokens;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @Override
    public AnswerFeedback evaluateAnswer(QuestionDTO question, String userAnswer) {
        try {
            String prompt = buildPrompt(question, userAnswer);
            String response = callOpenAI(prompt);
            return parseResponse(response);
        } catch (Exception e) {
            log.error("Failed to evaluate answer using OpenAI", e);
            // Fallback to basic comparison if AI evaluation fails
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