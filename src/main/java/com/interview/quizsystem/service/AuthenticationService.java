package com.interview.quizsystem.service;

import com.interview.quizsystem.dto.auth.AuthResponse;
import com.interview.quizsystem.dto.auth.LoginRequest;
import com.interview.quizsystem.dto.auth.RegisterRequest;

public interface AuthenticationService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String token);
} 