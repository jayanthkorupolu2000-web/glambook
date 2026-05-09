import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

const BASE = 'http://localhost:8080';

@Component({
  selector: 'app-customer-orders',
  templateUrl: './customer-orders.component.html'
})
export class CustomerOrdersComponent implements OnInit {
  orders: any[] = [];
  loading = false;

  // Review state — keyed by productId
  reviewOpen:       Record<number, boolean>  = {};
  reviewRating:     Record<number, number>   = {};
  reviewText:       Record<number, string>   = {};
  reviewSubmitting: Record<number, boolean>  = {};
  reviewSuccess:    Record<number, string>   = {};
  reviewError:      Record<number, string>   = {};
  existingReview:   Record<number, any>      = {};   // productId → review object
  canReview:        Record<number, boolean>  = {};
  reviewMode:       Record<number, 'create' | 'update'> = {};

  // Favorite state — keyed by productId
  favorited:        Record<number, boolean>  = {};
  togglingFav:      Record<number, boolean>  = {};

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.http.get<any[]>(`${BASE}/api/products/orders`).subscribe({
      next: data => {
        this.orders = Array.isArray(data) ? data : [];
        this.loading = false;
        // For each delivered order, check review eligibility and favorite status
        this.orders.forEach(o => {
          if (o.status === 'DELIVERED' && o.productId) {
            this.checkCanReview(o.productId);
            this.loadExistingReview(o.productId);
            this.checkFavorited(o.productId);
          }
        });
      },
      error: () => { this.orders = []; this.loading = false; }
    });
  }

  checkCanReview(productId: number): void {
    this.http.get<any>(`${BASE}/api/products/${productId}/can-review`).subscribe({
      next: res => { this.canReview[productId] = res.canReview; },
      error: () => { this.canReview[productId] = false; }
    });
  }

  loadExistingReview(productId: number): void {
    this.http.get<any[]>(`${BASE}/api/products/${productId}/reviews`).subscribe({
      next: reviews => {
        const myId = this.auth.getUserId();
        const mine = reviews.find(r => r.customerId === myId);
        if (mine) {
          this.existingReview[productId] = mine;
          this.reviewRating[productId] = mine.rating;
          this.reviewText[productId] = mine.reviewText || '';
        }
      },
      error: () => {}
    });
  }

  checkFavorited(productId: number): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.http.get<any[]>(`${BASE}/api/v1/customers/${id}/favorites/products`).subscribe({
      next: favs => { this.favorited[productId] = favs.some(f => f.id === productId); },
      error: () => {}
    });
  }

  // ── Review ────────────────────────────────────────────────────────────────

  toggleReview(productId: number): void {
    this.reviewOpen[productId] = !this.reviewOpen[productId];
    if (!this.reviewRating[productId]) this.reviewRating[productId] = 0;
    if (!this.reviewText[productId])   this.reviewText[productId]   = '';
    this.reviewMode[productId] = this.existingReview[productId] ? 'update' : 'create';
  }

  openUpdateReview(productId: number): void {
    const rev = this.existingReview[productId];
    this.reviewRating[productId] = rev?.rating || 0;
    this.reviewText[productId]   = rev?.reviewText || '';
    this.reviewMode[productId]   = 'update';
    this.reviewOpen[productId]   = true;
    this.reviewError[productId]  = '';
    this.reviewSuccess[productId] = '';
  }

  setRating(productId: number, r: number): void {
    this.reviewRating[productId] = r;
  }

  submitReview(order: any): void {
    const productId = order.productId;
    if (!this.reviewRating[productId] || this.reviewRating[productId] < 1) {
      this.reviewError[productId] = 'Please select a star rating.';
      return;
    }
    this.reviewSubmitting[productId] = true;
    this.reviewError[productId] = '';
    this.reviewSuccess[productId] = '';

    const payload = {
      rating: this.reviewRating[productId],
      reviewText: this.reviewText[productId] || ''
    };

    const isUpdate = this.reviewMode[productId] === 'update';
    const req$ = isUpdate
      ? this.http.patch<any>(`${BASE}/api/products/${productId}/reviews`, payload)
      : this.http.post<any>(`${BASE}/api/products/${productId}/reviews`, payload);

    req$.subscribe({
      next: (saved) => {
        this.reviewSuccess[productId] = isUpdate ? '✅ Review updated!' : '✅ Review submitted!';
        this.existingReview[productId] = saved;
        this.canReview[productId] = false;
        this.reviewOpen[productId] = false;
        this.reviewSubmitting[productId] = false;
      },
      error: (e: any) => {
        this.reviewError[productId] = e?.error?.message || 'Failed to save review.';
        this.reviewSubmitting[productId] = false;
      }
    });
  }

  // ── Favorite ──────────────────────────────────────────────────────────────

  toggleFavorite(productId: number): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.togglingFav[productId] = true;

    if (this.favorited[productId]) {
      this.http.delete(`${BASE}/api/products/${productId}/favorites`).subscribe({
        next: () => { this.favorited[productId] = false; this.togglingFav[productId] = false; },
        error: () => { this.togglingFav[productId] = false; }
      });
    } else {
      this.http.post<any>(`${BASE}/api/products/${productId}/favorites`, {}).subscribe({
        next: () => { this.favorited[productId] = true; this.togglingFav[productId] = false; },
        error: () => { this.togglingFav[productId] = false; }
      });
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  stars(n: number): number[] { return [1, 2, 3, 4, 5]; }

  statusBadge(s: string): string {
    const m: Record<string, string> = {
      PLACED: 'primary', CONFIRMED: 'warning text-dark',
      SHIPPED: 'info text-dark', DELIVERED: 'success', CANCELLED: 'danger'
    };
    return `badge bg-${m[s] ?? 'secondary'}`;
  }
}
