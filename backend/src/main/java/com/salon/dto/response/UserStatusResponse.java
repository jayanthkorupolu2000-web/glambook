package com.salon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserStatusResponse {
    private Long id;
    private String name;
    private String userType;
    private String status;
}
