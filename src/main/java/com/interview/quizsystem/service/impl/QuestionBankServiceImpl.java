package com.interview.quizsystem.service.impl;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.QuestionDTO;
import com.interview.quizsystem.model.QuestionType;
import com.interview.quizsystem.model.entity.QuestionBank;
import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.repository.QuestionBankRepository;
import com.interview.quizsystem.service.ContentHashService;
import com.interview.quizsystem.service.GitHubParserService;
import com.interview.quizsystem.service.QuestionBankService;
import com.interview.quizsystem.service.QuestionGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionBankRepository questionBankRepository;
    private final GitHubParserService gitHubParserService;
    private final QuestionGeneratorService questionGeneratorService;
    private final ContentHashService contentHashService;

    @Override
    @Transactional
    public void regenerateForTopic(Topic topic, int perDifficultyTarget) {
        Map<String, String> content = gitHubParserService.getContentByTopic(topic.getName());
        if (content.isEmpty()) {
            log.warn("No content found for topic: {}", topic.getName());
            questionBankRepository.deleteByTopic(topic);
            return;
        }

        String combined = content.values().stream().collect(Collectors.joining("\n\n"));
        String contentHash = contentHashService.calculateHash(combined);

        // Clear existing bank for the topic for simplicity in Phase 1
        questionBankRepository.deleteByTopic(topic);

        for (Difficulty difficulty : Difficulty.values()) {
            int totalNeeded = perDifficultyTarget;
            if (totalNeeded <= 0) continue;

            List<QuestionDTO> generated = questionGeneratorService.generateQuestions(topic.getName(), totalNeeded, difficulty);
            for (QuestionDTO dto : generated) {
                // Normalize correct answer for MCQ to match one of the option strings
                if (dto.getType() == QuestionType.MULTIPLE_CHOICE && dto.getOptions() != null && !dto.getOptions().isEmpty()) {
                    dto = dto.toBuilder()
                            .correctAnswer(normalizeMcqCorrectAnswer(dto.getCorrectAnswer(), dto.getOptions()))
                            .build();
                }
                QuestionBank qb = QuestionBank.builder()
                        .topic(topic)
                        .difficulty(difficulty)
                        .questionText(dto.getContent())
                        .questionType(dto.getType() == null ? QuestionType.SHORT_ANSWER : dto.getType())
                        .options(dto.getOptions())
                        .expectedAnswer(dto.getCorrectAnswer())
                        .explanation(dto.getExplanation())
                        .sourceFile(dto.getSourceFile())
                        .sourceContent(dto.getSourceContent())
                        .contentHash(contentHash)
                        .build();
                questionBankRepository.save(qb);
            }
        }
    }

    private String normalizeMcqCorrectAnswer(String rawCorrect, List<String> options) {
        if (rawCorrect == null) return null;
        String trimmed = rawCorrect.trim();
        // Case 1: Exact match
        for (String opt : options) {
            if (opt != null && opt.trim().equals(trimmed)) {
                return opt.trim();
            }
        }
        // Case 2: Starts with letter prefix like "A.", "B)", etc.
        if (trimmed.length() >= 2 && Character.isLetter(trimmed.charAt(0)) && (trimmed.charAt(1) == '.' || trimmed.charAt(1) == ')' )) {
            int idx = Character.toUpperCase(trimmed.charAt(0)) - 'A';
            if (idx >= 0 && idx < options.size()) {
                return options.get(idx);
            }
            // Also try to match substring after the prefix
            String withoutPrefix = trimmed.substring(2).trim();
            for (String opt : options) {
                if (opt != null && (opt.trim().equals(withoutPrefix) || opt.trim().contains(withoutPrefix))) {
                    return opt.trim();
                }
            }
        }
        // Case 3: Raw correct string is contained within an option
        for (String opt : options) {
            if (opt != null) {
                String o = opt.trim();
                if (o.equalsIgnoreCase(trimmed) || o.contains(trimmed) || trimmed.contains(o)) {
                    return o;
                }
            }
        }
        // Fallback: return raw, validation may fail later
        return trimmed;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDTO> getQuestionsByTopicAndDifficulty(Topic topic, Difficulty difficulty, int limit) {
        List<QuestionBank> bank = questionBankRepository.findByTopicAndDifficulty(topic, difficulty);
        // Filter out invalid entries (no expected answer or empty MCQ options)
        List<QuestionBank> valid = bank.stream()
                .filter(qb -> qb.getExpectedAnswer() != null && !qb.getExpectedAnswer().isBlank())
                .filter(qb -> qb.getQuestionType() != QuestionType.MULTIPLE_CHOICE || (qb.getOptions() != null && !qb.getOptions().isEmpty()))
                .collect(Collectors.toList());
        // Shuffle to avoid same order every time
        Collections.shuffle(valid);
        return valid.stream().limit(limit)
                .map(qb -> QuestionDTO.builder()
                        .id(UUID.randomUUID().toString())
                        .content(qb.getQuestionText())
                        .type(qb.getQuestionType())
                        .options(qb.getOptions())
                        .correctAnswer(qb.getExpectedAnswer())
                        .explanation(qb.getExplanation())
                        .topic(topic.getName())
                        .difficulty(qb.getDifficulty())
                        .sourceFile(qb.getSourceFile())
                        .sourceContent(qb.getSourceContent())
                        .questionBankId(qb.getId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionDTO> getQuestionsByIds(List<String> questionIds) {
        log.debug("Getting questions by IDs: {}", questionIds);
        
        // Since we don't have a direct mapping from question IDs to question bank entries,
        // we'll need to get all questions and filter by the IDs
        // This is a limitation of the current design - in a real implementation,
        // we'd want to store the generated question IDs in the question bank
        
        List<QuestionBank> allQuestions = questionBankRepository.findAll();
        
        // For now, we'll return questions based on the order of IDs
        // This is a simplified implementation
        List<QuestionDTO> result = new ArrayList<>();
        for (int i = 0; i < Math.min(questionIds.size(), allQuestions.size()); i++) {
            QuestionBank qb = allQuestions.get(i);
            result.add(QuestionDTO.builder()
                .id(questionIds.get(i)) // Use the provided ID
                .content(qb.getQuestionText())
                .type(qb.getQuestionType())
                .options(qb.getOptions())
                .correctAnswer(qb.getExpectedAnswer())
                .explanation(qb.getExplanation())
                .topic(qb.getTopic().getName())
                .difficulty(qb.getDifficulty())
                .sourceFile(qb.getSourceFile())
                .sourceContent(qb.getSourceContent())
                .questionBankId(qb.getId())
                .build());
        }
        
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public long countByTopicAndDifficulty(Topic topic, Difficulty difficulty) {
        return questionBankRepository.countByTopicAndDifficulty(topic, difficulty);
    }
}


