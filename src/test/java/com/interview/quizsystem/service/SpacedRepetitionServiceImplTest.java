package com.interview.quizsystem.service;

import com.interview.quizsystem.model.entity.SpacedRepetitionData;
import com.interview.quizsystem.model.entity.User;
import com.interview.quizsystem.repository.SpacedRepetitionDataRepository;
import com.interview.quizsystem.service.impl.SpacedRepetitionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpacedRepetitionServiceImplTest {

    @Mock
    private SpacedRepetitionDataRepository spacedRepetitionDataRepository;

    @InjectMocks
    private SpacedRepetitionServiceImpl spacedRepetitionService;

    private User testUser;
    private String testQuestionId;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .password("password")
            .build();
        testQuestionId = "test-question-123";
    }

    @Test
    void initializeQuestion_NewQuestion_CreatesInitialData() {
        // Given
        when(spacedRepetitionDataRepository.findByUserIdAndQuestionId(testUser.getId(), testQuestionId))
            .thenReturn(Optional.empty());
        when(spacedRepetitionDataRepository.save(any(SpacedRepetitionData.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SpacedRepetitionData result = spacedRepetitionService.initializeQuestion(testUser, testQuestionId);

        // Then
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(testQuestionId, result.getQuestionId());
        assertEquals(0, result.getRepetitionNumber());
        assertEquals(new BigDecimal("2.5"), result.getEaseFactor());
        assertEquals(0, result.getIntervalDays());
        assertTrue(result.isDueForReview());
        assertEquals(0, result.getConsecutiveCorrect());
        assertEquals(0, result.getConsecutiveIncorrect());

        verify(spacedRepetitionDataRepository).save(any(SpacedRepetitionData.class));
    }

    @Test
    void initializeQuestion_ExistingQuestion_ReturnsExistingData() {
        // Given
        SpacedRepetitionData existingData = SpacedRepetitionData.builder()
            .id(1L)
            .user(testUser)
            .questionId(testQuestionId)
            .repetitionNumber(5)
            .easeFactor(new BigDecimal("2.8"))
            .intervalDays(30)
            .nextReviewDate(LocalDateTime.now().plusDays(5))
            .build();

        when(spacedRepetitionDataRepository.findByUserIdAndQuestionId(testUser.getId(), testQuestionId))
            .thenReturn(Optional.of(existingData));

        // When
        SpacedRepetitionData result = spacedRepetitionService.initializeQuestion(testUser, testQuestionId);

        // Then
        assertNotNull(result);
        assertEquals(existingData, result);
        verify(spacedRepetitionDataRepository, never()).save(any(SpacedRepetitionData.class));
    }

    @Test
    void processAnswer_CorrectAnswer_UpdatesEaseFactorAndInterval() {
        // Given
        SpacedRepetitionData existingData = SpacedRepetitionData.builder()
            .id(1L)
            .user(testUser)
            .questionId(testQuestionId)
            .repetitionNumber(2)
            .easeFactor(new BigDecimal("2.5"))
            .intervalDays(6)
            .nextReviewDate(LocalDateTime.now().minusDays(1))
            .consecutiveCorrect(1)
            .consecutiveIncorrect(0)
            .build();

        when(spacedRepetitionDataRepository.findByUserIdAndQuestionId(testUser.getId(), testQuestionId))
            .thenReturn(Optional.of(existingData));
        when(spacedRepetitionDataRepository.save(any(SpacedRepetitionData.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SpacedRepetitionData result = spacedRepetitionService.processAnswer(testUser, testQuestionId, true);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getRepetitionNumber());
        assertEquals(new BigDecimal("2.6"), result.getEaseFactor()); // 2.5 + 0.1
        assertEquals(16, result.getIntervalDays()); // 6 * 2.6 = 15.6, rounded to 16
        assertEquals(2, result.getConsecutiveCorrect());
        assertEquals(0, result.getConsecutiveIncorrect());
        assertTrue(result.getNextReviewDate().isAfter(LocalDateTime.now()));

        verify(spacedRepetitionDataRepository).save(result);
    }

    @Test
    void processAnswer_IncorrectAnswer_DecreasesEaseFactorAndResetsInterval() {
        // Given
        SpacedRepetitionData existingData = SpacedRepetitionData.builder()
            .id(1L)
            .user(testUser)
            .questionId(testQuestionId)
            .repetitionNumber(3)
            .easeFactor(new BigDecimal("2.5"))
            .intervalDays(16)
            .nextReviewDate(LocalDateTime.now().minusDays(1))
            .consecutiveCorrect(2)
            .consecutiveIncorrect(0)
            .build();

        when(spacedRepetitionDataRepository.findByUserIdAndQuestionId(testUser.getId(), testQuestionId))
            .thenReturn(Optional.of(existingData));
        when(spacedRepetitionDataRepository.save(any(SpacedRepetitionData.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SpacedRepetitionData result = spacedRepetitionService.processAnswer(testUser, testQuestionId, false);

        // Then
        assertNotNull(result);
        assertEquals(4, result.getRepetitionNumber());
        assertEquals(new BigDecimal("2.3"), result.getEaseFactor()); // 2.5 - 0.2
        assertEquals(37, result.getIntervalDays()); // 16 * 2.3 = 36.8, rounded to 37
        assertEquals(0, result.getConsecutiveCorrect());
        assertEquals(1, result.getConsecutiveIncorrect());
        assertTrue(result.getNextReviewDate().isAfter(LocalDateTime.now()));

        verify(spacedRepetitionDataRepository).save(result);
    }

    @Test
    void processAnswer_EaseFactorBelowMinimum_ClampsToMinimum() {
        // Given
        SpacedRepetitionData existingData = SpacedRepetitionData.builder()
            .id(1L)
            .user(testUser)
            .questionId(testQuestionId)
            .repetitionNumber(1)
            .easeFactor(new BigDecimal("1.4"))
            .intervalDays(1)
            .nextReviewDate(LocalDateTime.now().minusDays(1))
            .build();

        when(spacedRepetitionDataRepository.findByUserIdAndQuestionId(testUser.getId(), testQuestionId))
            .thenReturn(Optional.of(existingData));
        when(spacedRepetitionDataRepository.save(any(SpacedRepetitionData.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SpacedRepetitionData result = spacedRepetitionService.processAnswer(testUser, testQuestionId, false);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("1.3"), result.getEaseFactor()); // Clamped to minimum
        verify(spacedRepetitionDataRepository).save(result);
    }

    @Test
    void getDueQuestions_ReturnsDueQuestionsOrdered() {
        // Given
        SpacedRepetitionData due1 = SpacedRepetitionData.builder()
            .id(1L)
            .user(testUser)
            .questionId("question1")
            .nextReviewDate(LocalDateTime.now().minusDays(2))
            .build();
        SpacedRepetitionData due2 = SpacedRepetitionData.builder()
            .id(2L)
            .user(testUser)
            .questionId("question2")
            .nextReviewDate(LocalDateTime.now().minusDays(1))
            .build();

        when(spacedRepetitionDataRepository.findDueQuestionsByUserIdOrdered(eq(testUser.getId()), any(LocalDateTime.class)))
            .thenReturn(List.of(due1, due2));

        // When
        List<SpacedRepetitionData> result = spacedRepetitionService.getDueQuestions(testUser);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(due1, result.get(0));
        assertEquals(due2, result.get(1));
    }

    @Test
    void isQuestionDue_NewQuestion_ReturnsTrue() {
        // Given
        when(spacedRepetitionDataRepository.findByUserIdAndQuestionId(testUser.getId(), testQuestionId))
            .thenReturn(Optional.empty());

        // When
        boolean result = spacedRepetitionService.isQuestionDue(testUser, testQuestionId);

        // Then
        assertTrue(result);
    }

    @Test
    void isQuestionDue_DueQuestion_ReturnsTrue() {
        // Given
        SpacedRepetitionData data = SpacedRepetitionData.builder()
            .nextReviewDate(LocalDateTime.now().minusDays(1))
            .build();

        when(spacedRepetitionDataRepository.findByUserIdAndQuestionId(testUser.getId(), testQuestionId))
            .thenReturn(Optional.of(data));

        // When
        boolean result = spacedRepetitionService.isQuestionDue(testUser, testQuestionId);

        // Then
        assertTrue(result);
    }

    @Test
    void isQuestionDue_NotDueQuestion_ReturnsFalse() {
        // Given
        SpacedRepetitionData data = SpacedRepetitionData.builder()
            .nextReviewDate(LocalDateTime.now().plusDays(1))
            .build();

        when(spacedRepetitionDataRepository.findByUserIdAndQuestionId(testUser.getId(), testQuestionId))
            .thenReturn(Optional.of(data));

        // When
        boolean result = spacedRepetitionService.isQuestionDue(testUser, testQuestionId);

        // Then
        assertFalse(result);
    }
}
