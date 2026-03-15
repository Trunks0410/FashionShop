// src/main/java/com/oneshop/controller/ChatController.java
package com.oneshop.controller;

import com.oneshop.entity.ChatMessageEntity;
import com.oneshop.model.ChatMessage;
import com.oneshop.repository.ChatMessageRepository;
// Thêm các import này
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller; // Đã có

import java.time.LocalDateTime;

@Controller // Đã có
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate; // Đã có
    private final ChatMessageRepository chatMessageRepository; // Đã có

    public ChatController(SimpMessagingTemplate messagingTemplate, // Đã có
                          ChatMessageRepository chatMessageRepository) { // Đã có
        this.messagingTemplate = messagingTemplate;
        this.chatMessageRepository = chatMessageRepository;
    }

    @MessageMapping("/chat.send") // Đã có
    public void send(@Payload ChatMessage msg) { // Đã có
        // Lưu vào CSDL
        chatMessageRepository.save(ChatMessageEntity.builder()
                .sender(msg.getSender())
                .receiver(msg.getReceiver())
                .content(msg.getContent())
                .timestamp(LocalDateTime.now())
                .type(ChatMessageEntity.MessageType.CHAT)
                .build());

        // Gửi đến người nhận
        messagingTemplate.convertAndSendToUser(msg.getReceiver(), "/queue/private", msg); // Đã có
    }

    @MessageMapping("/chat.join") // Đã có
    public void join(@Payload ChatMessage msg) { // Đã có
        msg.setType("JOIN");
        messagingTemplate.convertAndSendToUser(msg.getReceiver(), "/queue/private", msg); // Đã có
    }
}