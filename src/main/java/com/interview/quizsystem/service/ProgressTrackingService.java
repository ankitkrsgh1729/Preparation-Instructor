package com.interview.quizsystem.service;

import com.interview.quizsystem.model.Progress;
import com.interview.quizsystem.model.QuizSession;

public interface ProgressTrackingService {
    Progress getProgress();
    Progress updateProgress(QuizSession completedSession);
    void saveProgress(Progress progress);
    Progress loadProgress();
} 