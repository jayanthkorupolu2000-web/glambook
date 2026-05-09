package com.salon.dto.request;

import lombok.Data;

/**
 * Unified metadata request for adding a portfolio entry.
 *
 * Modes:
 *  1. Single photo/video  → dataUrl (preferred) or filePath
 *  2. Before/After pair   → beforeDataUrl + afterDataUrl (preferred) or beforeFilePath + afterFilePath
 *
 * When dataUrl fields are provided, the backend decodes and saves them as real files
 * so ALL users (including customers on other browsers) can view the images.
 */
@Data
public class PortfolioMetadataRequest {

    // ── Data-URL fields (preferred — backend saves as real files) ─────────────

    /** Base64 data-URL for a single photo/video. */
    private String dataUrl;

    /** Base64 data-URL for the "before" photo in a before/after pair. */
    private String beforeDataUrl;

    /** Base64 data-URL for the "after" photo in a before/after pair. */
    private String afterDataUrl;

    // ── Path fields (fallback — file must exist in assets folder) ─────────────

    /** Relative path for a single file, e.g. /assets/portfolio/haircut1.jpg */
    private String filePath;

    /** Relative path for the "before" image in a pair. */
    private String beforeFilePath;

    /** Relative path for the "after" image in a pair. */
    private String afterFilePath;

    // ── Metadata ──────────────────────────────────────────────────────────────

    /** Human-readable service label, e.g. "Bridal Makeup" */
    private String serviceTag;

    /** Comma-separated tags, e.g. "Makeup,Bridal,Women" */
    private String tags;

    /** Short description shown below the image */
    private String caption;

    /** Client testimonial quote */
    private String testimonial;

    /** Whether this item should appear in the featured section */
    private boolean featured;
}
