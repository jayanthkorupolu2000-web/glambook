package com.salon.controller;

import com.salon.dto.request.PortfolioMetadataRequest;
import com.salon.dto.response.PortfolioResponse;
import com.salon.entity.Portfolio;
import com.salon.entity.PortfolioMediaType;
import com.salon.entity.Professional;
import com.salon.exception.ResourceNotFoundException;
import com.salon.repository.PortfolioRepository;
import com.salon.repository.ProfessionalRepository;
import com.salon.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/professionals/{professionalId}/portfolio")
@RequiredArgsConstructor
@Tag(name = "Portfolio")
public class PortfolioController {

    private final PortfolioRepository portfolioRepository;
    private final ProfessionalRepository professionalRepository;
    private final FileStorageService fileStorageService;

    @GetMapping
    @Operation(summary = "Get portfolio items for a professional (authenticated)")
    public ResponseEntity<List<PortfolioResponse>> getPortfolio(@PathVariable Long professionalId) {
        List<PortfolioResponse> items = portfolioRepository.findByProfessionalId(professionalId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    /**
     * Public endpoint — no JWT required.
     * Customers browsing professionals can view portfolios without being logged in.
     * Mapped at /api/v1/professionals/{id}/portfolio/public so the security config
     * can permit it without affecting the authenticated endpoint above.
     */
    @GetMapping("/public")
    @Operation(summary = "Get portfolio items for a professional (public — no auth required)")
    public ResponseEntity<List<PortfolioResponse>> getPortfolioPublic(@PathVariable Long professionalId) {
        List<PortfolioResponse> items = portfolioRepository.findByProfessionalId(professionalId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    /**
     * Unified metadata POST.
     * If dataUrl / beforeDataUrl / afterDataUrl are provided, the backend
     * decodes them and saves as real files — so ALL users can view the images.
     * Falls back to filePath fields if data-URLs are not provided.
     */
    @PostMapping("/metadata")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Add a portfolio entry — accepts data-URLs or file paths")
    public ResponseEntity<PortfolioResponse> addMetadataItem(
            @PathVariable Long professionalId,
            @RequestBody PortfolioMetadataRequest req) {

        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));

        boolean isPair = hasValue(req.getBeforeDataUrl()) || hasValue(req.getBeforeFilePath());

        Portfolio.PortfolioBuilder builder = Portfolio.builder()
                .professional(professional)
                .serviceTag(req.getServiceTag())
                .tags(req.getTags())
                .caption(req.getCaption())
                .testimonial(req.getTestimonial())
                .isFeatured(req.isFeatured());

        PortfolioMediaType mediaType;

        if (isPair) {
            mediaType = PortfolioMediaType.BEFORE_AFTER_PHOTO;
            String beforeUrl = hasValue(req.getBeforeDataUrl())
                    ? fileStorageService.storeBase64(req.getBeforeDataUrl(), "portfolio")
                    : req.getBeforeFilePath();
            String afterUrl = hasValue(req.getAfterDataUrl())
                    ? fileStorageService.storeBase64(req.getAfterDataUrl(), "portfolio")
                    : req.getAfterFilePath();
            builder.beforePhotoUrl(beforeUrl)
                   .afterPhotoUrl(afterUrl)
                   .filePath(beforeUrl);
        } else {
            String resolvedPath;
            if (hasValue(req.getDataUrl())) {
                resolvedPath = fileStorageService.storeBase64(req.getDataUrl(), "portfolio");
            } else if (hasValue(req.getFilePath())) {
                resolvedPath = req.getFilePath();
            } else {
                return ResponseEntity.badRequest().build();
            }
            String lower = resolvedPath.toLowerCase();
            mediaType = (lower.endsWith(".mp4") || lower.endsWith(".webm"))
                    ? PortfolioMediaType.VIDEO_CLIP
                    : PortfolioMediaType.SINGLE_PHOTO;
            builder.filePath(resolvedPath)
                   .photoUrl(mediaType == PortfolioMediaType.SINGLE_PHOTO ? resolvedPath : null)
                   .videoUrl(mediaType == PortfolioMediaType.VIDEO_CLIP   ? resolvedPath : null);
        }

        builder.mediaType(mediaType);
        Portfolio saved = portfolioRepository.save(builder.build());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    private boolean hasValue(String s) { return s != null && !s.isBlank(); }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Add a portfolio item")
    public ResponseEntity<PortfolioResponse> addItem(
            @PathVariable Long professionalId,
            @RequestPart("dto") String dtoJson,
            @RequestPart(value = "beforeFile", required = false) MultipartFile beforeFile,
            @RequestPart(value = "afterFile", required = false) MultipartFile afterFile,
            @RequestPart(value = "singleFile", required = false) MultipartFile singleFile,
            @RequestPart(value = "videoFile", required = false) MultipartFile videoFile) {

        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional not found"));

        // Parse DTO JSON manually
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map<String, Object> dto;
        try {
            dto = mapper.readValue(dtoJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        String mediaTypeStr = (String) dto.getOrDefault("mediaType", "SINGLE_PHOTO");
        PortfolioMediaType mediaType;
        try { mediaType = PortfolioMediaType.valueOf(mediaTypeStr); }
        catch (Exception e) { mediaType = PortfolioMediaType.SINGLE_PHOTO; }

        Portfolio.PortfolioBuilder builder = Portfolio.builder()
                .professional(professional)
                .mediaType(mediaType)
                .serviceTag((String) dto.get("serviceTag"))
                .caption((String) dto.get("caption"))
                .testimonial((String) dto.get("testimonial"))
                .isFeatured(Boolean.TRUE.equals(dto.get("isFeatured")));

        // Store files based on media type
        if (mediaType == PortfolioMediaType.BEFORE_AFTER_PHOTO) {
            if (beforeFile != null && !beforeFile.isEmpty())
                builder.beforePhotoUrl(fileStorageService.storePhoto(beforeFile, "portfolio"));
            if (afterFile != null && !afterFile.isEmpty())
                builder.afterPhotoUrl(fileStorageService.storePhoto(afterFile, "portfolio"));
        } else if (mediaType == PortfolioMediaType.SINGLE_PHOTO) {
            if (singleFile != null && !singleFile.isEmpty())
                builder.photoUrl(fileStorageService.storePhoto(singleFile, "portfolio"));
        } else if (mediaType == PortfolioMediaType.VIDEO_CLIP) {
            if (videoFile != null && !videoFile.isEmpty())
                builder.videoUrl(fileStorageService.storeVideo(videoFile, "portfolio"));
        }

        Portfolio saved = portfolioRepository.save(builder.build());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @DeleteMapping("/{itemId}")
    @Operation(summary = "Delete a portfolio item")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long professionalId,
            @PathVariable Long itemId) {
        Portfolio item = portfolioRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio item not found"));
        // Clean up stored files
        fileStorageService.deleteFile(item.getBeforePhotoUrl());
        fileStorageService.deleteFile(item.getAfterPhotoUrl());
        fileStorageService.deleteFile(item.getPhotoUrl());
        fileStorageService.deleteFile(item.getVideoUrl());
        portfolioRepository.delete(item);
        return ResponseEntity.noContent().build();
    }

    private PortfolioResponse toResponse(Portfolio p) {
        PortfolioResponse r = new PortfolioResponse();
        r.setId(p.getId());
        r.setProfessionalId(p.getProfessional().getId());
        r.setProfessionalName(p.getProfessional().getName());
        r.setMediaType(p.getMediaType().name());
        r.setServiceTag(p.getServiceTag());
        r.setFilePath(p.getFilePath());
        r.setTags(p.getTags());
        r.setCaption(p.getCaption());
        r.setTestimonial(p.getTestimonial());
        r.setBeforePhotoUrl(p.getBeforePhotoUrl());
        r.setAfterPhotoUrl(p.getAfterPhotoUrl());
        r.setPhotoUrl(p.getPhotoUrl());
        r.setVideoUrl(p.getVideoUrl());
        r.setFeatured(p.isFeatured());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }
}
