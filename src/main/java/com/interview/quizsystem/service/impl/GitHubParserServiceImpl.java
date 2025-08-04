package com.interview.quizsystem.service.impl;

import com.interview.quizsystem.service.GitHubParserService;
import com.interview.quizsystem.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubParserServiceImpl implements GitHubParserService {

    private final Git gitClient;
    private final TopicService topicService;

    @Value("${github.repository.local-path}")
    private String localPath;

    @Value("${github.repository.file-patterns}")
    private String filePatterns;

    @Value("${github.repository.exclude-patterns:}")
    private String excludePatterns;

    @Override
    public void syncRepository() {
        try {
            log.info("Syncing repository...");
            gitClient.pull().call();
            log.info("Repository synced successfully");
        } catch (GitAPIException e) {
            log.error("Error syncing repository", e);
            throw new RuntimeException("Failed to sync repository", e);
        }
    }

    @Override
    @Transactional
    public List<String> getAvailableTopics() {
        try {
            List<PathMatcher> includeMatchers = Arrays.stream(filePatterns.split(","))
                    .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern.trim()))
                    .collect(Collectors.toList());

            List<PathMatcher> excludeMatchers = excludePatterns.isEmpty() ? 
                Collections.emptyList() : 
                Arrays.stream(excludePatterns.split(","))
                    .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern.trim()))
                    .collect(Collectors.toList());

            List<String> topics = Files.walk(Paths.get(localPath))
                    .filter(Files::isRegularFile)
                    .filter(path -> includeMatchers.stream().anyMatch(matcher -> matcher.matches(path)))
                    .filter(path -> excludeMatchers.stream().noneMatch(matcher -> matcher.matches(path)))
                    .map(this::extractTopic)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            // Create topics in the database
            topics.forEach(topicService::getOrCreateTopic);

            return topics;
        } catch (IOException e) {
            log.error("Error getting available topics", e);
            throw new RuntimeException("Failed to get available topics", e);
        }
    }

    @Override
    @Transactional
    public Map<String, String> getContentByTopic(String topic) {
        try {
            // Ensure topic exists in database
            topicService.getOrCreateTopic(topic);

            List<PathMatcher> includeMatchers = Arrays.stream(filePatterns.split(","))
                    .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern.trim()))
                    .collect(Collectors.toList());

            Map<String, String> contentMap = new HashMap<>();
            Files.walk(Paths.get(localPath))
                    .filter(Files::isRegularFile)
                    .filter(path -> includeMatchers.stream().anyMatch(matcher -> matcher.matches(path)))
                    .filter(path -> extractTopic(path).equals(topic))
                    .forEach(path -> {
                        try {
                            String content = Files.readString(path);
                            contentMap.put(path.getFileName().toString(), content);
                        } catch (IOException e) {
                            log.error("Error reading file: {}", path, e);
                        }
                    });
            return contentMap;
        } catch (IOException e) {
            log.error("Error getting content for topic: {}", topic, e);
            throw new RuntimeException("Failed to get content for topic: " + topic, e);
        }
    }

    @Override
    public String getContentFromFile(String filePath) {
        try {
            Path path = Paths.get(localPath, filePath);
            return Files.readString(path);
        } catch (IOException e) {
            log.error("Error reading file: {}", filePath, e);
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    private String extractTopic(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        
        // Remove file extension
        fileName = fileName.replaceAll("\\.md$", "");
        
        // Handle different naming patterns
        if (fileName.contains("-")) {
            // For files like "algorithms-sorting.md"
            return fileName.split("-")[0];
        } else if (fileName.contains("_")) {
            // For files like "algorithms_sorting.md"
            return fileName.split("_")[0];
        } else {
            // For files without separators, return the whole name
            return fileName;
        }
    }
} 