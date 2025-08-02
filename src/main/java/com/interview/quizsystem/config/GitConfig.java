package com.interview.quizsystem.config;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Configuration
public class GitConfig {

    @Value("${github.repository.url}")
    private String repositoryUrl;

    @Value("${github.repository.local-path}")
    private String localPath;

    @Bean
    public Git gitClient() throws IOException, GitAPIException {
        File localRepo = new File(localPath);
        
        if (localRepo.exists()) {
            return Git.open(localRepo);
        }
        
        return Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(localRepo)
                .call();
    }
} 