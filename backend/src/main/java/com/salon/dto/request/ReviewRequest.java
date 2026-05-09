package com.salon.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequest {

    @NotNull(message = "Please provide a valid professionalId")
    private Long professionalId;

    @NotNull(message = "Please provide a valid appointmentId")
    private Long appointmentId;

    @NotNull(message = "Please provide a valid rating")
    @Min(value = 1, message = "Please provide a valid rating")
    @Max(value = 5, message = "Please provide a valid rating")
    private Integer rating;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;

    /** Optional list of uploaded photo paths */
    @Builder.Default
    private List<String> photos = new ArrayList<>();
}
