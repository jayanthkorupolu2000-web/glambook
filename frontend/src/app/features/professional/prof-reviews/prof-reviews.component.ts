import { Component, HostListener, OnInit } from '@angular/core';
import { ReviewWithResponse } from '../../../models/professional.model';
import { AuthService } from '../../../services/auth.service';
import { ProfessionalReviewService } from '../../../services/professional-review.service';

@Component({
  selector: 'app-prof-reviews',
  templateUrl: './prof-reviews.component.html',
  styleUrls: ['./prof-reviews.component.css']
})
export class ProfReviewsComponent implements OnInit {
  reviews: ReviewWithResponse[] = [];
  filtered: ReviewWithResponse[] = [];
  loading = false;
  filter = 'ALL';
  profId = 0;

  /** Track liked review IDs (stored in localStorage) */
  likedIds: Set<number> = new Set();

  // ── Lightbox state ───────────────────────────────────────────────
  lightboxPhotos: string[] = [];
  lightboxIndex  = 0;
  lightboxOpen   = false;

  private readonly LIKE_KEY = 'prof_liked_reviews';

  constructor(
    private reviewService: ProfessionalReviewService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.loadLiked();
    this.load();
  }

  // ── Like state (localStorage) ─────────────────────────────────────
  private loadLiked(): void {
    try {
      const raw = localStorage.getItem(this.LIKE_KEY);
      const ids: number[] = raw ? JSON.parse(raw) : [];
      this.likedIds = new Set(ids);
    } catch { this.likedIds = new Set(); }
  }

  private saveLiked(): void {
    localStorage.setItem(this.LIKE_KEY, JSON.stringify([...this.likedIds]));
  }

  isLiked(reviewId: number): boolean {
    return this.likedIds.has(reviewId);
  }

  toggleLike(reviewId: number): void {
    if (this.likedIds.has(reviewId)) {
      this.likedIds.delete(reviewId);
    } else {
      this.likedIds.add(reviewId);
    }
    this.saveLiked();
    this.applyFilter();
  }

  load(): void {
    this.loading = true;
    this.reviewService.getReviews(this.profId).subscribe({
      next: data => {
        this.reviews = data as ReviewWithResponse[];
        this.applyFilter();
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  applyFilter(): void {
    if (this.filter === 'LIKED')
      this.filtered = this.reviews.filter(r => this.isLiked(r.id));
    else if (this.filter === 'NOT_LIKED')
      this.filtered = this.reviews.filter(r => !this.isLiked(r.id));
    else
      this.filtered = this.reviews;
  }

  // ── Lightbox ─────────────────────────────────────────────────────
  openLightbox(photos: string[], index: number): void {
    this.lightboxPhotos = photos.map(p => this.resolvePhotoUrl(p));
    this.lightboxIndex  = index;
    this.lightboxOpen   = true;
  }

  closeLightbox(): void { this.lightboxOpen = false; }

  prevPhoto(): void {
    if (this.lightboxIndex > 0) this.lightboxIndex--;
  }

  nextPhoto(): void {
    if (this.lightboxIndex < this.lightboxPhotos.length - 1) this.lightboxIndex++;
  }

  @HostListener('document:keydown', ['$event'])
  onKey(e: KeyboardEvent): void {
    if (!this.lightboxOpen) return;
    if (e.key === 'Escape')     this.closeLightbox();
    if (e.key === 'ArrowLeft')  this.prevPhoto();
    if (e.key === 'ArrowRight') this.nextPhoto();
  }

  // ── Helpers ──────────────────────────────────────────────────────
  avgRating(): number {
    if (!this.reviews.length) return 0;
    return this.reviews.reduce((s, r) => s + r.rating, 0) / this.reviews.length;
  }

  likedCount(): number {
    return this.reviews.filter(r => this.isLiked(r.id)).length;
  }

  stars(n: number): number[] { return Array.from({ length: 5 }, (_, i) => i + 1); }

  initials(name: string): string {
    return (name || '?').charAt(0).toUpperCase();
  }

  resolvePhotoUrl(url: string): string {
    if (!url) return '';
    if (url.startsWith('http')) return url;
    return `http://localhost:8080${url}`;
  }
}
