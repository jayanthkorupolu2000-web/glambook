import { Component, HostListener, OnInit } from '@angular/core';
import { ReviewWithResponse } from '../../../models/professional.model';
import { AuthService } from '../../../services/auth.service';
import { ProfessionalReviewService } from '../../../services/professional-review.service';
import { ReviewPhotoStoreService } from '../../../services/review-photo-store.service';

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
  responseText: Record<number, string | undefined> = {};
  submitting: Record<number, boolean> = {};

  /** Track which review cards have the reply box open */
  replyOpen: Record<number, boolean> = {};

  /** Track locally-acknowledged review IDs (stored in localStorage) */
  acknowledgedIds: Set<number> = new Set();

  // ── Lightbox state ───────────────────────────────────────────────
  lightboxPhotos: string[] = [];
  lightboxIndex  = 0;
  lightboxOpen   = false;

  private readonly ACK_KEY = 'prof_ack_reviews';

  constructor(
    private reviewService: ProfessionalReviewService,
    private auth: AuthService,
    private photoStore: ReviewPhotoStoreService
  ) {}

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.loadAcknowledged();
    this.load();
  }

  // ── Acknowledged state (localStorage) ────────────────────────────
  private loadAcknowledged(): void {
    try {
      const raw = localStorage.getItem(this.ACK_KEY);
      const ids: number[] = raw ? JSON.parse(raw) : [];
      this.acknowledgedIds = new Set(ids);
    } catch { this.acknowledgedIds = new Set(); }
  }

  private saveAcknowledged(): void {
    localStorage.setItem(this.ACK_KEY, JSON.stringify([...this.acknowledgedIds]));
  }

  isAcknowledged(reviewId: number): boolean {
    return this.acknowledgedIds.has(reviewId);
  }

  acknowledge(reviewId: number): void {
    this.acknowledgedIds.add(reviewId);
    this.saveAcknowledged();
  }

  // ── Reply toggle ──────────────────────────────────────────────────
  toggleReply(reviewId: number): void {
    this.replyOpen[reviewId] = !this.replyOpen[reviewId];
  }

  isReplyOpen(reviewId: number): boolean {
    return !!this.replyOpen[reviewId];
  }

  load(): void {
    this.loading = true;
    this.reviewService.getReviews(this.profId).subscribe({
      next: data => {
        this.reviews = this.photoStore.mergeIntoReviews(data) as ReviewWithResponse[];
        this.applyFilter();
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  applyFilter(): void {
    if (this.filter === 'RESPONDED')
      this.filtered = this.reviews.filter(r => r.professionalResponse);
    else if (this.filter === 'PENDING')
      this.filtered = this.reviews.filter(r => !r.professionalResponse);
    else
      this.filtered = this.reviews;
  }

  respond(review: ReviewWithResponse): void {
    const text = this.responseText[review.id];
    if (!text?.trim()) return;
    this.submitting[review.id] = true;
    this.reviewService.respondToReview(this.profId, review.id, text).subscribe({
      next: updated => {
        const idx = this.reviews.findIndex(r => r.id === review.id);
        if (idx !== -1) this.reviews[idx] = updated;
        this.replyOpen[review.id] = false;
        // Also mark as acknowledged when a reply is sent
        this.acknowledge(review.id);
        this.applyFilter();
        this.submitting[review.id] = false;
      },
      error: () => { this.submitting[review.id] = false; }
    });
  }

  // ── Lightbox ─────────────────────────────────────────────────────
  openLightbox(photos: string[], index: number): void {
    this.lightboxPhotos = photos;
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

  stars(n: number): number[] { return Array.from({ length: 5 }, (_, i) => i + 1); }

  initials(name: string): string {
    return (name || '?').charAt(0).toUpperCase();
  }
}
