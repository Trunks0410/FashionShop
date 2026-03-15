package com.oneshop.config; // (Hoặc package config của bạn)

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component; // <-- Rất quan trọng

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component // <-- Đánh dấu đây là một Bean
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        AuthenticationException exception) 
            throws IOException, ServletException {
        
        // Đây là logic bạn đã viết (lấy từ inner class cũ)
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\": false, \"message\": \"Tài khoản hoặc mật khẩu không chính xác!\"}");
        } else {
            response.sendRedirect("/login?error=true");
        }
    }
}