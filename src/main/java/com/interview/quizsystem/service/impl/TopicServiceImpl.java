package com.interview.quizsystem.service.impl;

import com.interview.quizsystem.model.entity.Topic;
import com.interview.quizsystem.repository.TopicRepository;
import com.interview.quizsystem.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;

    @Override
    @Transactional
    public Topic createTopic(String name, String description) {
        Topic topic = Topic.builder()
                .name(name)
                .description(description)
                .build();
        return topicRepository.save(topic);
    }

    @Override
    @Transactional
    public Topic getOrCreateTopic(String name) {
        return topicRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> createTopic(name, null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> getAllTopics() {
        return topicRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Topic getTopicByName(String name) {
        return topicRepository.findByNameIgnoreCase(name)
                .orElse(null);
    }
} 