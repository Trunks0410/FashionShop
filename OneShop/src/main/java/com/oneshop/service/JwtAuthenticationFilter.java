// src/main/java/com/oneshop/service/JwtAuthenticationFilter.java
package com.oneshop.service; // Đảm bảo đúng package

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier; // Import Qualifier
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService; // Import UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Import Exception
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component; // Đảm bảo là Component
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component // Đảm bảo được Spring quản lý
@RequiredArgsConstructor // Tự động tạo constructor cho các field final
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class); // Thêm Logger

    private final JwtService jwtService; // Inject JwtService (đã có)

    // Inject UserDetailsService (chính là UserService của bạn)
    // Dùng @Qualifier nếu có nhiều bean UserDetailsService
    @Qualifier("userService") // Chỉ định tên bean là "userService" (mặc định của class UserService)
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization"); // Lấy header Authorization
        final String jwt;
        final String username;

        // 1. Kiểm tra header: Không có hoặc không bắt đầu bằng "Bearer " -> bỏ qua filter này
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. Lấy JWT token từ header
            jwt = authHeader.substring(7); // Bỏ "Bearer "
            username = jwtService.extractUsername(jwt); // Trích xuất username

            // 3. Nếu có username và chưa có ai được xác thực trong SecurityContext
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Tải UserDetails từ UserDetailsService (UserService)
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // 4. Kiểm tra token có hợp lệ không (chưa hết hạn, khớp với userDetails)
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Tạo đối tượng Authentication
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Credentials (password) không cần thiết với JWT
                            userDetails.getAuthorities() // Lấy quyền từ userDetails
                    );
                    // Gắn thêm chi tiết request vào Authentication (ví dụ: IP address)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 5. SET Authentication vào SecurityContext -> User được xác thực cho request này
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("JWT Authenticated user: {}, setting security context", username);
                } else {
                     logger.warn("Invalid JWT token received for user: {}", username);
                }
            }
        } catch (UsernameNotFoundException e) {
             logger.warn("User not found for username extracted from JWT: {}", e.getMessage());
        } catch (Exception e) {
            // Bắt các lỗi khác từ JWT (ExpiredJwtException, MalformedJwtException, SignatureException...)
            logger.error("Could not set user authentication in security context: {}", e.getMessage());
        }

        // 6. Cho phép request đi tiếp trong filter chain
        filterChain.doFilter(request, response);
    }
}