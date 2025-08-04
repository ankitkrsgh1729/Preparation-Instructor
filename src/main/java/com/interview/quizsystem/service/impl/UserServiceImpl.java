package com.interview.quizsystem.service.impl;

import com.interview.quizsystem.model.entity.User;
import com.interview.quizsystem.repository.UserRepository;
import com.interview.quizsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    // TODO: Replace with actual authentication
    private static final String DEFAULT_USER_EMAIL = "default@example.com";
    private static final String DEFAULT_USERNAME = "default_user";

    @Override
    @Transactional // Changed from readOnly to allow write
    public User getCurrentUser() {
        return getOrCreateUser(DEFAULT_USERNAME, DEFAULT_USER_EMAIL);
    }

    @Override
    @Transactional // Ensure we can write to database
    public User getOrCreateUser(String username, String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User user = User.builder()
                            .username(username)
                            .email(email)
                            .createdAt(LocalDateTime.now())
                            .lastLogin(LocalDateTime.now())
                            .build();
                    return userRepository.save(user);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }
} 