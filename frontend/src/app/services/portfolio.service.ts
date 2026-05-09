import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

const BASE = 'http://localhost:8080/api/v1/professionals';

/** Mirrors the backend PortfolioResponse DTO */
export interface PortfolioItem {
  id: number;
  professionalId: number;
  professionalName: string;
  mediaType: 'BEFORE_AFTER_PHOTO' | 'SINGLE_PHOTO' | 'VIDEO_CLIP';
  serviceTag: string;
  tags: string;
  caption: string;
  testimonial: string;
  /** Relative path to a file in Angular assets, e.g. /assets/portfolio/haircut1.jpg */
  filePath: string | null;
  /** Legacy server-upload paths (still supported) */
  beforePhotoUrl: string | null;
  afterPhotoUrl: string | null;
  photoUrl: string | null;
  videoUrl: string | null;
  isFeatured: boolean;
  createdAt: string;
}

/** Request body for the metadata-only POST endpoint */
export interface PortfolioMetadataRequest {
  /** Base64 data-URL for a single photo/video (preferred) */
  dataUrl?: string;
  /** Base64 data-URL for the "before" photo */
  beforeDataUrl?: string;
  /** Base64 data-URL for the "after" photo */
  afterDataUrl?: string;
  /** Fallback: relative asset path */
  filePath?: string;
  beforeFilePath?: string;
  afterFilePath?: string;
  serviceTag?: string;
  tags?: string;
  caption?: string;
  testimonial?: string;
  featured?: boolean;
}

@Injectable({ providedIn: 'root' })
export class PortfolioService {
  constructor(private http: HttpClient) {}

  /**
   * Fetch all portfolio items for a professional.
   * Uses the /public endpoint — no JWT required.
   */
  getPortfolioByProfessional(professionalId: number): Observable<PortfolioItem[]> {
    return this.http.get<PortfolioItem[]>(`${BASE}/${professionalId}/portfolio/public`);
  }

  /**
   * Add a metadata-only portfolio entry.
   * The actual file must already exist in frontend/src/assets/portfolio/.
   */
  addPortfolioItem(professionalId: number, dto: PortfolioMetadataRequest): Observable<PortfolioItem> {
    return this.http.post<PortfolioItem>(`${BASE}/${professionalId}/portfolio/metadata`, dto);
  }

  /**
   * Delete a portfolio entry by ID.
   */
  deletePortfolioItem(professionalId: number, portfolioId: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${professionalId}/portfolio/${portfolioId}`);
  }
}
