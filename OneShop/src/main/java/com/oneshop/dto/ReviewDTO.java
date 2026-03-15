// src/main/java/com/oneshop/dto/ReviewDTO.java
package com.oneshop.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReviewDTO {
    private Long reviewId;
    private String comment;
    private Integer rating;
    private LocalDateTime reviewDate;
    private String username;
    private List<String> mediaUrls = new ArrayList<>();

    // Constructor cho JPQL
    public ReviewDTO(Long reviewId, String comment, Integer rating, 
                     LocalDateTime reviewDate, String username, String mediaUrl) {
        this.reviewId = reviewId;
        this.comment = comment;
        this.rating = rating;
        this.reviewDate = reviewDate;
        this.username = username;
        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            this.mediaUrls.add(mediaUrl);
        }
    }

    // Getters
    public Long getReviewId() { return reviewId; }
    public String getComment() { return comment; }
    public Integer getRating() { return rating; }
    public LocalDateTime getReviewDate() { return reviewDate; }
    public String getUsername() { return username; }
    public List<String> getMediaUrls() { return mediaUrls; }
}