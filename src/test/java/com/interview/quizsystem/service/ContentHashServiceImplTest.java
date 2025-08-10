package com.interview.quizsystem.service;

import com.interview.quizsystem.service.impl.ContentHashServiceImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContentHashServiceImplTest {

    private final ContentHashService service = new ContentHashServiceImpl();

    @Test
    void sameContentProducesSameHash() {
        String a = "hello world";
        String b = "hello world";
        assertEquals(service.calculateHash(a), service.calculateHash(b));
    }

    @Test
    void differentContentProducesDifferentHash() {
        String a = "hello world";
        String b = "hello world!";
        assertNotEquals(service.calculateHash(a), service.calculateHash(b));
    }
}


