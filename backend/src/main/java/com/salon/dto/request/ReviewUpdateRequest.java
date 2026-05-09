package com.salon.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Used when a customer edits their review — comment is optional,
 * photos are appended to the existing list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewUpdateRequest {

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;

    /** Updated quality rating (optional) */
    @Min(value = 1, message = "Quality rating must be between 1 and 5")
    @Max(value = 5, message = "Quality rating must be between 1 and 5")
    private Integer qualityRating;

    /** Updated timeliness rating (optional) */
    @Min(value = 1, message = "Timeliness rating must be between 1 and 5")
    @Max(value = 5, message = "Timeliness rating must be between 1 and 5")
    private Integer timelinessRating;

    /** Updated professionalism rating (optional) */
    @Min(value = 1, message = "Professionalism rating must be between 1 and 5")
    @Max(value = 5, message = "Professionalism rating must be between 1 and 5")
    private Integer professionalismRating;

    /** Additional photos to append */
    @Builder.Default
    private List<String> photos = new ArrayList<>();
}
