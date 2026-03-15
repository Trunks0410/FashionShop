// src/main/java/com/oneshop/service/JwtUtils.java
package com.oneshop.service;

import java.util.Date;
// Bỏ import User nếu không dùng trực tiếp ở đây nữa
// import com.oneshop.entity.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication; // Giữ lại nếu vẫn dùng hàm cũ ở đâu đó
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    // Hàm cũ (có thể giữ lại nếu bạn vẫn dùng nó ở đâu đó cho form login)
    // public String generateJwtToken(Authentication authentication) {
    //     User userPrincipal = (User) authentication.getPrincipal();
    //     return Jwts.builder()
    //             .setSubject(userPrincipal.getUsername())
    //             .setIssuedAt(new Date())
    //             .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
    //             .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), SignatureAlgorithm.HS512)
    //             .compact();
    // }

    // ===>>> THÊM HÀM MỚI NÀY <<<===
    /**
     * Tạo JWT token trực tiếp từ username.
     * @param username Tên đăng nhập của người dùng trong hệ thống.
     * @return Chuỗi JWT token.
     */
    public String generateJwtTokenFromUsername(String username) {
        return Jwts.builder()
                .setSubject(username) // Subject là username
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), SignatureAlgorithm.HS512) // Nên dùng HS512 hoặc thuật toán mạnh hơn
                .compact();
    }
    // ===>>> KẾT THÚC HÀM MỚI <<<===

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes())).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes())).build().parseClaimsJws(authToken);
            return true;
        } catch (Exception e) {
             // Nên log lỗi cụ thể ở đây (ExpiredJwtException, MalformedJwtException, ...)
             // logger.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}