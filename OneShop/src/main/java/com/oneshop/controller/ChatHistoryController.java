package com.oneshop.controller;

import com.oneshop.entity.ChatMessageEntity;
import com.oneshop.repository.ChatMessageRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatHistoryController {

    private final ChatMessageRepository repo;

    public ChatHistoryController(ChatMessageRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/history")
    public List<ChatMessageEntity> getHistory(@RequestParam String userA, @RequestParam String userB) {
        return repo.findChatHistory(userA, userB);
    }
}