package com.salon.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SalonPolicyResponse {
    private Long id;
    private Long ownerId;
    private String ownerName;
    private String title;
    private String content;
    private LocalDateTime createdAt;
}
