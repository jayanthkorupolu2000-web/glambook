package com.salon.service;

import com.salon.dto.request.ReviewResponseRequest;
import com.salon.dto.response.ReviewWithResponseDTO;

import java.util.List;

public interface ReviewResponseService {
    ReviewWithResponseDTO respondToReview(Long professionalId, Long reviewId, ReviewResponseRequest dto);
    List<ReviewWithResponseDTO> getReviewsForProfessional(Long professionalId);
}
