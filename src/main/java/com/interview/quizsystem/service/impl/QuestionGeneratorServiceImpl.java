package com.interview.quizsystem.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.interview.quizsystem.model.Question;
import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.QuestionType;
import com.interview.quizsystem.service.QuestionGeneratorService;
import com.interview.quizsystem.service.GitHubParserService;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGeneratorServiceImpl implements QuestionGeneratorService {

    private final GitHubParserService gitHubParserService;
    private final ObjectMapper objectMapper;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.temperature}")
    private double temperature;

    @Value("${openai.max-tokens}")
    private int maxTokens;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @Override
    public List<Question> generateQuestions(String topic, int count, Difficulty difficulty) {
        Map<String, String> topicContent = gitHubParserService.getContentByTopic(topic);
        List<Question> questions = new ArrayList<>();

        for (Map.Entry<String, String> entry : topicContent.entrySet()) {
            String content = entry.getValue();
            try {
                Question question = generateQuestion(content, topic, difficulty);
                questions.add(question);
                if (questions.size() >= count) {
                    break;
                }
            } catch (Exception e) {
                log.error("Error generating question for content from file: {}", entry.getKey(), e);
            }
        }

        return questions;
    }

    @Override
    public Question generateQuestion(String content, String topic, Difficulty difficulty) {
        try {
            String prompt = buildPrompt(content, difficulty);
            
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

            // Clean the response content
            responseContent = cleanJsonResponse(responseContent);

            return parseQuestionFromResponse(responseContent, topic, difficulty, content);
        } catch (Exception e) {
            log.error("Error generating question: {}", e.getMessage());
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

    private String buildPrompt(String content, Difficulty difficulty) {
        return String.format("""
                Create a technical interview question based on the following content:
                
                %s
                
                Requirements:
                - Difficulty level: %s
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

    private Question parseQuestionFromResponse(String response, String topic, Difficulty difficulty, String sourceContent) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            return Question.builder()
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