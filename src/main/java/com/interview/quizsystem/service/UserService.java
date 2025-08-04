package com.interview.quizsystem.service;

import com.interview.quizsystem.model.entity.User;

public interface UserService {
    User getCurrentUser();
    User getOrCreateUser(String username, String email);
    User getUserById(Long id);
} 