package org.example.common.dto.review;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ProductReviewResponse {
    private Integer id;
    private Integer productId;
    private Integer userId;
    private String username;
    private String fullName;
    private Integer rating;
    private String title;
    private String comment;
    private LocalDateTime createdAt;
}
