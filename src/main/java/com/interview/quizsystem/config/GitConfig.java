package com.interview.quizsystem.config;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Slf4j
@Configuration
public class GitConfig {

    @Value("${github.repository.url}")
    private String repositoryUrl;

    @Value("${github.repository.local-path}")
    private String localPath;

    @Value("${github.auth.username:}")
    private String username;

    @Value("${github.auth.token:}")
    private String token;

    @Bean
    public Git gitClient() throws IOException, GitAPIException {
        File localRepo = new File(localPath);
        
        if (localRepo.exists()) {
            return Git.open(localRepo);
        }

        // Create clone command
        var cloneCommand = Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(localRepo);

        // Add credentials if available
        if (!username.isEmpty() && !token.isEmpty()) {
            cloneCommand.setCredentialsProvider(
                new UsernamePasswordCredentialsProvider(username, token)
            );
        } else {
            log.warn("Git credentials not provided. Some operations may fail if repository requires authentication.");
        }
        
        return cloneCommand.call();
    }
} 