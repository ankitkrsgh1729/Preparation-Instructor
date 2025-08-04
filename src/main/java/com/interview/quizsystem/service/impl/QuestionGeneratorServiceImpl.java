package com.interview.quizsystem.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.interview.quizsystem.model.QuestionDTO;
import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.QuestionType;
import com.interview.quizsystem.model.AIOperationType;
import com.interview.quizsystem.model.AIUsageStatus;
import com.interview.quizsystem.model.entity.AIModelUsage;
import com.interview.quizsystem.model.entity.AIModelError;
import com.interview.quizsystem.repository.AIModelUsageRepository;
import com.interview.quizsystem.repository.AIModelErrorRepository;
import com.interview.quizsystem.service.QuestionGeneratorService;
import com.interview.quizsystem.service.GitHubParserService;
import com.interview.quizsystem.service.TopicService;
import com.interview.quizsystem.service.UserService;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGeneratorServiceImpl implements QuestionGeneratorService {

    private final GitHubParserService gitHubParserService;
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
    public List<QuestionDTO> generateQuestions(String topic, int count, Difficulty difficulty) {
        Map<String, String> topicContent = gitHubParserService.getContentByTopic(topic);
        if (topicContent.isEmpty()) {
            log.warn("No content found for topic: {}", topic);
            return Collections.emptyList();
        }

        List<QuestionDTO> questions = new ArrayList<>();
        List<Map.Entry<String, String>> contentEntries = new ArrayList<>(topicContent.entrySet());
        int questionsPerContent = Math.max(2, (int) Math.ceil((double) count / contentEntries.size()));
        int maxAttempts = count * 2; // Allow some extra attempts for error cases
        int attempts = 0;

        while (questions.size() < count && attempts < maxAttempts) {
            // Cycle through content pieces
            int contentIndex = (attempts / questionsPerContent) % contentEntries.size();
            Map.Entry<String, String> entry = contentEntries.get(contentIndex);
            
            try {
                // Try to generate a question from this content piece
                QuestionDTO question = generateQuestion(entry.getValue(), topic, difficulty);
                
                // Check for duplicate questions
                if (isDifferentFromExisting(question, questions)) {
                    questions.add(question);
                    log.debug("Generated question {} of {} for topic: {}", questions.size(), count, topic);
                }
            } catch (Exception e) {
                log.warn("Failed to generate question from content piece {}, attempt {}: {}", 
                    entry.getKey(), attempts, e.getMessage());
            }
            
            attempts++;
        }

        if (questions.isEmpty()) {
            log.error("Failed to generate any questions for topic: {} after {} attempts", topic, attempts);
        } else if (questions.size() < count) {
            log.warn("Only generated {} of {} requested questions for topic: {}", 
                questions.size(), count, topic);
        }

        return questions;
    }

    @Override
    @Transactional
    public QuestionDTO generateQuestion(String content, String topic, Difficulty difficulty) {
        log.info("Starting question generation for topic: {}, difficulty: {}", topic, difficulty);
        long startTime = System.currentTimeMillis();
        AIModelUsage usage = null;
        
        try {
            String prompt = buildPrompt(content, difficulty);
            log.debug("Sending prompt to OpenAI: {}", prompt);
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            
            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode systemMessage = messages.addObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a technical interviewer creating questions based on provided content.");
            
            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            HttpResponse<String> response = Unirest.post(OPENAI_API_URL)
                    .body(requestBody.toString())
                    .asString();

            if (response.getStatus() != 200) {
                throw new RuntimeException("OpenAI API call failed with status: " + response.getStatus());
            }

            JSONObject jsonResponse = new JSONObject(response.getBody());
            String responseContent = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            // Calculate tokens (approximate)
            int promptTokens = prompt.length() / 4; // rough estimate
            int responseTokens = responseContent.length() / 4;
            int totalTokens = promptTokens + responseTokens;
            
            log.info("Calculated tokens - prompt: {}, response: {}, total: {}", promptTokens, responseTokens, totalTokens);
            
            // Create usage record
            usage = AIModelUsage.builder()
                    .topic(topicService.getTopicByName(topic))
                    .user(userService.getCurrentUser())
                    .operationType(AIOperationType.QUESTION_GENERATION)
                    .modelProvider("OPENAI")
                    .modelName(model)
                    .tokensUsed(totalTokens)
                    .costInUsd(COST_PER_TOKEN.multiply(new BigDecimal(totalTokens)).setScale(6, RoundingMode.HALF_UP))
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .status(AIUsageStatus.SUCCESS)
                    .build();
            
            usage = aiModelUsageRepository.save(usage);
            log.info("Saved AI usage record with ID: {}", usage.getId());

            // Clean and parse the response
            responseContent = cleanJsonResponse(responseContent);
            return parseQuestionFromResponse(responseContent, topic, difficulty, content);
            
        } catch (Exception e) {
            log.error("Error generating question: {}", e.getMessage(), e);
            
            // Create failed usage record if not already created
            if (usage == null) {
                usage = AIModelUsage.builder()
                        .topic(topicService.getTopicByName(topic))
                        .user(userService.getCurrentUser())
                        .operationType(AIOperationType.QUESTION_GENERATION)
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
            
            throw new RuntimeException("Failed to generate question", e);
        }
    }

    private String cleanJsonResponse(String response) {
        // Remove markdown code block syntax if present
        response = response.replaceAll("```json\\s*", "");
        response = response.replaceAll("```\\s*$", "");
        // Remove any other markdown code block syntax
        response = response.replaceAll("```[a-zA-Z]*\\s*", "");
        // Trim any whitespace
        response = response.trim();
        return response;
    }

    private boolean isDifferentFromExisting(QuestionDTO newQuestion, List<QuestionDTO> existingQuestions) {
        return existingQuestions.stream()
            .noneMatch(existing -> 
                existing.getContent().equals(newQuestion.getContent()) ||
                (existing.getCorrectAnswer() != null && 
                 existing.getCorrectAnswer().equals(newQuestion.getCorrectAnswer())));
    }

    private String buildPrompt(String content, Difficulty difficulty) {
        return String.format("""
                Create a unique technical interview question based on the following content.
                Make sure the question tests understanding, not just memorization.
                
                Content:
                %s
                
                Requirements:
                - Difficulty level: %s
                - Question should be challenging but answerable
                - Focus on practical understanding
                - For EASY difficulty: test basic concepts
                - For MEDIUM difficulty: test application of concepts
                - For HARD difficulty: test deep understanding and edge cases
                - Include a clear question
                - Provide multiple choice options (if applicable)
                - Include the correct answer
                - Add a detailed explanation
                
                Format the response as JSON with the following structure:
                {
                    "question": "...",
                    "type": "MULTIPLE_CHOICE|TRUE_FALSE|SHORT_ANSWER|SCENARIO_BASED",
                    "options": ["...", "..."] (for multiple choice),
                    "correctAnswer": "...",
                    "explanation": "..."
                }
                """, content, difficulty);
    }

    private QuestionDTO parseQuestionFromResponse(String response, String topic, Difficulty difficulty, String sourceContent) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            return QuestionDTO.builder()
                    .id(UUID.randomUUID().toString())
                    .content(jsonNode.get("question").asText())
                    .type(QuestionType.valueOf(jsonNode.get("type").asText()))
                    .options(extractOptions(jsonNode))
                    .correctAnswer(jsonNode.get("correctAnswer").asText())
                    .explanation(jsonNode.get("explanation").asText())
                    .topic(topic)
                    .difficulty(difficulty)
                    .sourceContent(sourceContent)
                    .build();
        } catch (Exception e) {
            log.error("Error parsing question from response: {}", response, e);
            throw new RuntimeException("Failed to parse question from AI response", e);
        }
    }

    private List<String> extractOptions(JsonNode jsonNode) {
        List<String> options = new ArrayList<>();
        if (jsonNode.has("options")) {
            jsonNode.get("options").forEach(option -> options.add(option.asText()));
        }
        return options;
    }
} 