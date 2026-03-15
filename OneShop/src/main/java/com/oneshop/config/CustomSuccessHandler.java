// src/main/java/com/oneshop/config/CustomSuccessHandler.java
package com.oneshop.config;

import com.oneshop.service.JwtUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority; // <<< THÊM IMPORT NÀY
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser; 
import org.springframework.security.oauth2.core.user.OAuth2User; 
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Collection; // <<< THÊM IMPORT NÀY
import java.util.Map;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomSuccessHandler.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String identifierForJwt = null;
        Object principal = authentication.getPrincipal();
        logger.debug("Authentication successful. Principal type: {}", principal.getClass().getName());

        try {
            if (principal instanceof UserDetails userDetails) {
                // Case 1: Form login
                identifierForJwt = userDetails.getUsername();
                logger.debug("Form login successful. Using username for JWT: {}", identifierForJwt);

            } else if (principal instanceof OidcUser oidcUser) {
                // Case 2: Google OIDC Login
                logger.debug("OIDC principal detected (Google). Attributes: {}", oidcUser.getAttributes());

                identifierForJwt = oidcUser.getAttribute("internal_username");

                if (StringUtils.hasText(identifierForJwt)) {
                    logger.info("Using 'internal_username' ({}) from OIDC attributes for JWT.", identifierForJwt);
                } else {
                    identifierForJwt = oidcUser.getEmail();
                    if (StringUtils.hasText(identifierForJwt)) {
                        logger.warn("Attribute 'internal_username' missing/empty in OidcUser attributes. Using email ({}) directly from OidcUser for JWT.", identifierForJwt);
                    } else {
                        logger.error("CRITICAL: Both 'internal_username' attribute and email from OidcUser are missing/empty. Cannot generate JWT. Subject ID: {}", oidcUser.getName());
                        throw new ServletException("Không thể xác định người dùng Google (thiếu thông tin định danh email).");
                    }
                }

            } else if (principal instanceof OAuth2User oauth2User) {
                // Case 3: Other OAuth2 Logins (e.g., Facebook)
                logger.debug("Generic OAuth2 principal detected. Attributes: {}", oauth2User.getAttributes());

                identifierForJwt = oauth2User.getAttribute("internal_username");

                if (StringUtils.hasText(identifierForJwt)) {
                    logger.info("Using 'internal_username' ({}) from OAuth2 attributes for JWT.", identifierForJwt);
                } else {
                    identifierForJwt = oauth2User.getAttribute("email");
                    if (StringUtils.hasText(identifierForJwt)) {
                        logger.warn("Attribute 'internal_username' missing/empty in OAuth2 attributes. Falling back to 'email' attribute ({}) for JWT.", identifierForJwt);
                    } else {
                        logger.error("CRITICAL: Both 'internal_username' and 'email' attributes are missing/empty for generic OAuth2 user. Cannot generate JWT. Principal Name: {}", oauth2User.getName());
                        throw new ServletException("Không thể xác định người dùng OAuth2 (thiếu thông tin định danh email).");
                    }
                }

            } else {
                // Case 4: Unexpected principal type
                logger.error("Unexpected principal type after successful authentication: {}. Cannot reliably generate JWT.", principal.getClass().getName());
                throw new ServletException("Loại thông tin xác thực không được hỗ trợ sau khi đăng nhập.");
            }

            if (!StringUtils.hasText(identifierForJwt)) {
                 logger.error("Identifier for JWT is still empty after processing principal.");
                 throw new ServletException("Không thể xác định định danh người dùng để tạo phiên đăng nhập.");
            }

        } catch (Exception e) {
             logger.error("Error retrieving identifier from Principal: {}", e.getMessage(), e);
             throw new ServletException("Lỗi xử lý thông tin người dùng sau đăng nhập.", e);
        }

        // --- Generate JWT token and Cookie ---
        String jwt = jwtUtils.generateJwtTokenFromUsername(identifierForJwt); // Use the determined identifier
        logger.debug("Generated JWT with subject: {}", identifierForJwt);
        Cookie cookie = new Cookie("jwtToken", jwt);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60); // 1 day
        response.addCookie(cookie);


        String targetUrl = "/"; 

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        boolean isAdmin = authorities.stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            targetUrl = "/admin/shops"; 
            logger.debug("Admin user detected. Redirecting to: {}", targetUrl);
        } else {
            logger.debug("Non-admin user detected. Redirecting to: {}", targetUrl);
        }

        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
           response.setContentType("application/json");
           response.getWriter().write("{\"success\": true, \"redirect\": \"" + targetUrl + "\"}");
        } else {
            response.sendRedirect(targetUrl);
        }
    }
}