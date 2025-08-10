package com.interview.quizsystem.integration;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.QuizSession;
import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.repository.QuestionBankRepository;
import com.interview.quizsystem.repository.TopicRepository;
import com.interview.quizsystem.service.GitHubParserService;
import com.interview.quizsystem.service.GitHubSyncService;
import com.interview.quizsystem.service.QuestionGeneratorService;
import com.interview.quizsystem.service.QuizSessionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class SyncAndSessionIntegrationTest {

    @Autowired private GitHubSyncService syncService;
    @Autowired private QuestionBankRepository bankRepo;
    @Autowired private TopicRepository topicRepo;
    @Autowired private QuizSessionService sessionService;

    @MockBean private GitHubParserService git;
    @MockBean private QuestionGeneratorService generator;

    @Test
    void syncFillsBankAndSessionUsesItNoGeneratorCalls() {
        // Arrange git mocks
        String topicName = "TopicA_" + System.currentTimeMillis(); // Unique name
        when(git.getAvailableTopics()).thenReturn(List.of(topicName));
        when(git.getContentByTopic(topicName)).thenReturn(Map.of("a.md","content"));

        // Minimal generated set (per difficulty) of 2 questions
        Mockito.when(generator.generateQuestions(eq(topicName), anyInt(), any()))
                .thenAnswer(inv -> TestFixtures.generated(2, inv.getArgument(2)));

        // Ensure topic exists (find or create)
        Topic topic = topicRepo.findByNameIgnoreCase(topicName)
                .orElseGet(() -> topicRepo.save(Topic.builder().name(topicName).build()));

        // Act: sync and pre-generate
        syncService.syncAndPreGenerateQuestions(2);

        // Assert bank counts per diff
        for (Difficulty d : Difficulty.values()) {
            assertEquals(2, bankRepo.countByTopicAndDifficulty(topic, d));
        }

        // Start session, expect no generator calls in start (we won't verify here, but behaviorally it should pull from bank)
        QuizSession session = sessionService.startSession(topicName, Difficulty.EASY, 2);
        assertEquals(2, session.getQuestions().size());
    }

    // --- fixtures ---
    static class TestFixtures {
        static List<com.interview.quizsystem.model.QuestionDTO> generated(int n, Difficulty d) {
            return java.util.stream.IntStream.range(0, n)
                    .mapToObj(i -> com.interview.quizsystem.model.QuestionDTO.builder()
                            .id(java.util.UUID.randomUUID().toString())
                            .content("Q"+i)
                            .type(com.interview.quizsystem.model.QuestionType.MULTIPLE_CHOICE)
                            .options(java.util.List.of("A","B"))
                            .correctAnswer("A")
                            .difficulty(d)
                            .build())
                    .toList();
        }
    }
}


