package com.salon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResourceRequest {

    @NotNull(message = "Please provide a valid ownerId")
    private Long ownerId;

    @NotBlank(message = "Please provide a valid type")
    @Pattern(regexp = "ROOM|EQUIPMENT", message = "Type must be ROOM or EQUIPMENT")
    private String type;

    @NotBlank(message = "Please provide a valid name")
    private String name;

    private String description;
}
