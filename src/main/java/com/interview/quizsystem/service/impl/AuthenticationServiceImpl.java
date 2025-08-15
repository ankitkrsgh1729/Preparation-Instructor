package com.interview.quizsystem.service.impl;

import com.interview.quizsystem.dto.auth.AuthResponse;
import com.interview.quizsystem.dto.auth.LoginRequest;
import com.interview.quizsystem.dto.auth.RegisterRequest;
import com.interview.quizsystem.dto.auth.ResetPasswordRequest;
import com.interview.quizsystem.model.Difficulty;
import com.interview.quizsystem.model.Role;
import com.interview.quizsystem.model.entity.DifficultyProgress;
import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.model.entity.TopicProgress;
import com.interview.quizsystem.model.entity.User;
import com.interview.quizsystem.repository.DifficultyProgressRepository;
import com.interview.quizsystem.repository.TopicProgressRepository;
import com.interview.quizsystem.repository.UserRepository;
import com.interview.quizsystem.security.JwtService;
import com.interview.quizsystem.service.AuthenticationService;
import com.interview.quizsystem.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final TopicService topicService;
    private final TopicProgressRepository topicProgressRepository;
    private final DifficultyProgressRepository difficultyProgressRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        // Create new user
        var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .lastLogin(LocalDateTime.now())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        user = userRepository.save(user);

        // Initialize progress data
        initializeUserProgress(user);

        // Generate token
        var token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Get user and generate token
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        var token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    @Override
    public void logout(String token) {
        // In a stateless JWT setup, we don't need to do anything here
        // If you want to implement token blacklisting, you would add the token to a blacklist here
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + request.getEmail()));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private void initializeUserProgress(User user) {
        // Initialize topic progress for all available topics
        List<Topic> topics = topicService.getAllTopics();
        for (Topic topic : topics) {
            TopicProgress topicProgress = TopicProgress.builder()
                    .user(user)
                    .topic(topic)
                    .startDate(LocalDateTime.now())
                    .active(true)
                    .questionsAttempted(0)
                    .questionsCorrect(0)
                    .overallScore(0.0)
                    .build();
            topicProgress = topicProgressRepository.save(topicProgress);

            // Initialize difficulty progress for each topic
            for (Difficulty difficulty : Difficulty.values()) {
                DifficultyProgress difficultyProgress = DifficultyProgress.builder()
                        .topicProgress(topicProgress)
                        .difficulty(difficulty)
                        .questionsAttempted(0)
                        .questionsCorrect(0)
                        .score(0.0)
                        .lastAttemptDate(LocalDateTime.now())
                        .build();
                difficultyProgressRepository.save(difficultyProgress);
            }
        }
    }
} 