// src/main/java/com/oneshop/config/WebSocketConfig.java
package com.oneshop.config;

import org.springframework.context.annotation.Configuration;
// Thêm các import này
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker; // Đã có
import org.springframework.web.socket.config.annotation.StompEndpointRegistry; // Đã có
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer; // Đã có

@Configuration
@EnableWebSocketMessageBroker // Đã có
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer { // Đã có

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) { // Đã có
        config.enableSimpleBroker("/queue", "/topic");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) { // Đã có
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}