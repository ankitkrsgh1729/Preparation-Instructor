package com.interview.quizsystem.service;

import com.interview.quizsystem.model.entity.Topic;
import java.util.List;

public interface TopicService {
    Topic createTopic(String name, String description);
    Topic getOrCreateTopic(String name);
    List<Topic> getAllTopics();
    Topic getTopicByName(String name);
} 