package com.interview.quizsystem.service;

import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.QuestionDTO;
import com.interview.quizsystem.model.QuestionType;
import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.repository.QuestionBankRepository;
import com.interview.quizsystem.service.impl.QuestionBankServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionBankServiceImplTest {

    private QuestionBankRepository bankRepo;
    private GitHubParserService git;
    private QuestionGeneratorService generator;
    private ContentHashService hashService;
    private QuestionBankService service;

    @BeforeEach
    void setUp() {
        bankRepo = mock(QuestionBankRepository.class);
        git = mock(GitHubParserService.class);
        generator = mock(QuestionGeneratorService.class);
        hashService = mock(ContentHashService.class);
        service = new QuestionBankServiceImpl(bankRepo, git, generator, hashService);
    }

    @Test
    void regenerateCreatesTargetPerDifficulty() {
        Topic topic = Topic.builder().id(1L).name("TopicA").build();
        when(git.getContentByTopic("TopicA")).thenReturn(Map.of("a.md", "content"));
        when(hashService.calculateHash(anyString())).thenReturn("hash");

        // Return 3 questions for EASY only; service loops all difficulties
        List<QuestionDTO> qs = List.of(
                QuestionDTO.builder().content("Q1").type(QuestionType.MULTIPLE_CHOICE).options(List.of("A"))
                        .correctAnswer("A").difficulty(Difficulty.EASY).build(),
                QuestionDTO.builder().content("Q2").type(QuestionType.SHORT_ANSWER)
                        .correctAnswer("X").difficulty(Difficulty.EASY).build(),
                QuestionDTO.builder().content("Q3").type(QuestionType.MULTIPLE_CHOICE).options(List.of("B"))
                        .correctAnswer("B").difficulty(Difficulty.EASY).build()
        );
        when(generator.generateQuestions(eq("TopicA"), anyInt(), any())).thenReturn(qs);

        service.regenerateForTopic(topic, 3);

        // Save invoked at least for EASY; total calls >= 3 across diffs
        verify(bankRepo, atLeast(3)).save(any());
        verify(bankRepo).deleteByTopic(topic);
    }

    @Test
    void getQuestionsReturnsLimitedAndValid() {
        Topic topic = Topic.builder().id(1L).name("TopicA").build();
        when(bankRepo.findByTopicAndDifficulty(topic, Difficulty.EASY)).thenReturn(
                TestFixtures.bankList(
                        // valid MCQ
                        TestFixtures.bank("Q1", QuestionType.MULTIPLE_CHOICE, List.of("A","B"), "A", Difficulty.EASY),
                        // invalid MCQ (no options)
                        TestFixtures.bank("Q2", QuestionType.MULTIPLE_CHOICE, List.of(), "A", Difficulty.EASY),
                        // valid SA
                        TestFixtures.bank("Q3", QuestionType.SHORT_ANSWER, null, "Ans", Difficulty.EASY)
                )
        );

        List<QuestionDTO> out = service.getQuestionsByTopicAndDifficulty(topic, Difficulty.EASY, 2);
        assertTrue(out.size() <= 2);
        assertTrue(out.stream().allMatch(q -> q.getCorrectAnswer() != null));
    }

    // --- minimal fixtures ---
    static class TestFixtures {
        static com.interview.quizsystem.model.entity.QuestionBank bank(
                String text, QuestionType type, List<String> options, String answer, Difficulty diff
        ) {
            return com.interview.quizsystem.model.entity.QuestionBank.builder()
                    .id(1L)
                    .questionText(text)
                    .questionType(type)
                    .options(options)
                    .expectedAnswer(answer)
                    .difficulty(diff)
                    .build();
        }
        static List<com.interview.quizsystem.model.entity.QuestionBank> bankList(com.interview.quizsystem.model.entity.QuestionBank... arr) {
            return List.of(arr);
        }
    }
}


