package com.interview.quizsystem.service;

import java.util.List;
import java.util.Map;

public interface GitHubParserService {
    void syncRepository();
    List<String> getAvailableTopics();
    Map<String, String> getContentByTopic(String topic);
    String getContentFromFile(String filePath);
} 