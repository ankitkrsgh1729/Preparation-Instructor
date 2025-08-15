package com.interview.quizsystem.service;

import com.interview.quizsystem.model.entity.SessionMomentum;
import com.interview.quizsystem.model.entity.User;
import com.interview.quizsystem.repository.SessionMomentumRepository;
import com.interview.quizsystem.service.impl.SessionMomentumServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionMomentumServiceImplTest {

    @Mock
    private SessionMomentumRepository sessionMomentumRepository;

    @InjectMocks
    private SessionMomentumServiceImpl sessionMomentumService;

    private User testUser;
    private String testSessionId;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .password("password")
            .build();
        testSessionId = "test-session-123";
    }

    @Test
    void initializeSession_NewSession_CreatesMomentumData() {
        // Given
        when(sessionMomentumRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.empty());
        when(sessionMomentumRepository.save(any(SessionMomentum.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SessionMomentum result = sessionMomentumService.initializeSession(testSessionId, testUser);

        // Then
        assertNotNull(result);
        assertEquals(testSessionId, result.getSessionId());
        assertEquals(testUser, result.getUser());
        assertEquals(BigDecimal.ZERO, result.getMomentumScore());
        assertEquals(0, result.getQuestionsAnswered());
        assertEquals(0, result.getCorrectAnswers());
        assertEquals(0L, result.getAverageResponseTimeMs());

        verify(sessionMomentumRepository).save(any(SessionMomentum.class));
    }

    @Test
    void initializeSession_ExistingSession_ReturnsExistingData() {
        // Given
        SessionMomentum existingMomentum = SessionMomentum.builder()
            .sessionId(testSessionId)
            .user(testUser)
            .momentumScore(BigDecimal.valueOf(75.0))
            .questionsAnswered(5)
            .correctAnswers(4)
            .build();

        when(sessionMomentumRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(existingMomentum));

        // When
        SessionMomentum result = sessionMomentumService.initializeSession(testSessionId, testUser);

        // Then
        assertNotNull(result);
        assertEquals(existingMomentum, result);
        verify(sessionMomentumRepository, never()).save(any(SessionMomentum.class));
    }

    @Test
    void recordAnswer_CorrectAnswer_UpdatesMomentum() {
        // Given
        SessionMomentum existingMomentum = SessionMomentum.builder()
            .sessionId(testSessionId)
            .user(testUser)
            .momentumScore(BigDecimal.ZERO)
            .questionsAnswered(0)
            .correctAnswers(0)
            .averageResponseTimeMs(0L)
            .build();

        when(sessionMomentumRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(existingMomentum));
        when(sessionMomentumRepository.save(any(SessionMomentum.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SessionMomentum result = sessionMomentumService.recordAnswer(testSessionId, testUser, true, 5000L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getQuestionsAnswered());
        assertEquals(1, result.getCorrectAnswers());
        assertEquals(5000L, result.getAverageResponseTimeMs());
        assertEquals(100.0, result.getAccuracyRate());
        assertTrue(result.getMomentumScore().doubleValue() > 0);

        verify(sessionMomentumRepository).save(result);
    }

    @Test
    void recordAnswer_IncorrectAnswer_UpdatesMomentum() {
        // Given
        SessionMomentum existingMomentum = SessionMomentum.builder()
            .sessionId(testSessionId)
            .user(testUser)
            .momentumScore(BigDecimal.ZERO)
            .questionsAnswered(0)
            .correctAnswers(0)
            .averageResponseTimeMs(0L)
            .build();

        when(sessionMomentumRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(existingMomentum));
        when(sessionMomentumRepository.save(any(SessionMomentum.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SessionMomentum result = sessionMomentumService.recordAnswer(testSessionId, testUser, false, 10000L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getQuestionsAnswered());
        assertEquals(0, result.getCorrectAnswers());
        assertEquals(10000L, result.getAverageResponseTimeMs());
        assertEquals(0.0, result.getAccuracyRate());
        assertTrue(result.getMomentumScore().doubleValue() < 50.0);

        verify(sessionMomentumRepository).save(result);
    }

    @Test
    void isInFlow_HighMomentumScore_ReturnsTrue() {
        // Given
        SessionMomentum momentum = SessionMomentum.builder()
            .momentumScore(BigDecimal.valueOf(85.0))
            .build();

        when(sessionMomentumRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(momentum));

        // When
        boolean result = sessionMomentumService.isInFlow(testSessionId);

        // Then
        assertTrue(result);
    }

    @Test
    void isInFlow_LowMomentumScore_ReturnsFalse() {
        // Given
        SessionMomentum momentum = SessionMomentum.builder()
            .momentumScore(BigDecimal.valueOf(50.0))
            .build();

        when(sessionMomentumRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(momentum));

        // When
        boolean result = sessionMomentumService.isInFlow(testSessionId);

        // Then
        assertFalse(result);
    }

    @Test
    void isStruggling_LowMomentumScore_ReturnsTrue() {
        // Given
        SessionMomentum momentum = SessionMomentum.builder()
            .momentumScore(BigDecimal.valueOf(20.0))
            .build();

        when(sessionMomentumRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(momentum));

        // When
        boolean result = sessionMomentumService.isStruggling(testSessionId);

        // Then
        assertTrue(result);
    }

    @Test
    void isStruggling_HighMomentumScore_ReturnsFalse() {
        // Given
        SessionMomentum momentum = SessionMomentum.builder()
            .momentumScore(BigDecimal.valueOf(60.0))
            .build();

        when(sessionMomentumRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(momentum));

        // When
        boolean result = sessionMomentumService.isStruggling(testSessionId);

        // Then
        assertFalse(result);
    }

    @Test
    void getMomentumScore_ReturnsCorrectScore() {
        // Given
        SessionMomentum momentum = SessionMomentum.builder()
            .momentumScore(BigDecimal.valueOf(75.5))
            .build();

        when(sessionMomentumRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(momentum));

        // When
        double result = sessionMomentumService.getMomentumScore(testSessionId);

        // Then
        assertEquals(75.5, result);
    }

    @Test
    void getMomentumScore_NoMomentumData_ReturnsZero() {
        // Given
        when(sessionMomentumRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.empty());

        // When
        double result = sessionMomentumService.getMomentumScore(testSessionId);

        // Then
        assertEquals(0.0, result);
    }

    @Test
    void getSessionAccuracy_ReturnsCorrectAccuracy() {
        // Given
        SessionMomentum momentum = SessionMomentum.builder()
            .questionsAnswered(10)
            .correctAnswers(7)
            .build();

        when(sessionMomentumRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(momentum));

        // When
        double result = sessionMomentumService.getSessionAccuracy(testSessionId);

        // Then
        assertEquals(70.0, result);
    }

    @Test
    void getAverageResponseTime_ReturnsCorrectTime() {
        // Given
        SessionMomentum momentum = SessionMomentum.builder()
            .averageResponseTimeMs(5000L)
            .build();

        when(sessionMomentumRepository.findBySessionId(testSessionId))
            .thenReturn(Optional.of(momentum));

        // When
        long result = sessionMomentumService.getAverageResponseTime(testSessionId);

        // Then
        assertEquals(5000L, result);
    }
}

