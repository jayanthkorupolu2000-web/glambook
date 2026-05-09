import { Component, OnInit } from '@angular/core';
import { ReviewWithResponse } from '../../../models/professional.model';
import { AuthService } from '../../../services/auth.service';
import { ProfessionalReviewService } from '../../../services/professional-review.service';
import { ReviewPhotoStoreService } from '../../../services/review-photo-store.service';

@Component({
  selector: 'app-prof-reviews',
  templateUrl: './prof-reviews.component.html'
})
export class ProfReviewsComponent implements OnInit {
  reviews: ReviewWithResponse[] = [];
  filtered: ReviewWithResponse[] = [];
  loading = false;
  filter = 'ALL';
  profId = 0;
  responseText: Record<number, string | undefined> = {};
  submitting: Record<number, boolean> = {};

  constructor(
    private reviewService: ProfessionalReviewService,
    private auth: AuthService,
    private photoStore: ReviewPhotoStoreService
  ) {}

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.reviewService.getReviews(this.profId).subscribe({
      next: data => {
        // Merge locally-stored customer photos into each review
        this.reviews = this.photoStore.mergeIntoReviews(data) as ReviewWithResponse[];
        this.applyFilter();
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  applyFilter(): void {
    if (this.filter === 'RESPONDED') this.filtered = this.reviews.filter(r => r.professionalResponse);
    else if (this.filter === 'PENDING') this.filtered = this.reviews.filter(r => !r.professionalResponse);
    else this.filtered = this.reviews;
  }

  respond(review: ReviewWithResponse): void {
    const text = this.responseText[review.id];
    if (!text || !text.trim()) return;
    this.submitting[review.id] = true;
    this.reviewService.respondToReview(this.profId, review.id, text).subscribe({
      next: updated => {
        const idx = this.reviews.findIndex(r => r.id === review.id);
        if (idx !== -1) this.reviews[idx] = updated;
        this.applyFilter();
        this.submitting[review.id] = false;
      },
      error: () => this.submitting[review.id] = false
    });
  }

  avgRating(): number {
    if (!this.reviews.length) return 0;
    return this.reviews.reduce((s, r) => s + r.rating, 0) / this.reviews.length;
  }

  stars(n: number): number[] { return Array.from({ length: 5 }, (_, i) => i + 1); }
}
