package com.salon.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PolicyResponse {
    private Long id;
    private String title;
    private String content;
    private String publishedBy;
    private LocalDateTime createdAt;
}
