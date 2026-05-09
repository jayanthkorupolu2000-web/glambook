import { HttpClient } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { AppointmentResponse } from '../../../models/customer.model';
import { AuthService } from '../../../services/auth.service';
import { CustomerAppointmentService } from '../../../services/customer-appointment.service';

const BASE = 'http://localhost:8080';

@Component({
  selector: 'app-customer-appointments',
  templateUrl: './customer-appointments.component.html'
})
export class CustomerAppointmentsComponent implements OnInit, OnDestroy {
  appointments: AppointmentResponse[] = [];
  activeTab = 'upcoming';
  loading = false;
  customerId = 0;

  // Payment modal
  payingAppt: AppointmentResponse | null = null;
  payMethod: 'CASH' | 'CARD' = 'CASH';
  cardNumber = '';
  cardExpiry = '';
  cardCvv = '';
  payLoading = false;
  payError = '';
  paySuccess = '';

  // Payment history
  paymentHistory: any[] = [];
  historyLoading = false;

  // Reviews map: appointmentId → review
  myReviews: Map<number, any> = new Map();

  // Review modal
  reviewingAppt: AppointmentResponse | null = null;
  reviewRating = 5;
  reviewQualityRating = 5;
  reviewTimelinessRating = 5;
  reviewProfessionalismRating = 5;
  reviewComment = '';
  reviewLoading = false;
  reviewError = '';
  reviewSuccess = '';
  reviewPhotos: File[] = [];
  reviewPhotoPreview: string[] = [];
  reviewMode: 'create' | 'update' = 'create';

  // Payment guard state
  reviewedAppointments: Map<number, boolean> = new Map();
  pendingPayAppt: AppointmentResponse | null = null;
  private payTimerHandle: ReturnType<typeof setInterval> | null = null;

  // Favorites
  favoritedServices: Record<number, boolean> = {};
  togglingFav: Record<number, boolean> = {};

  constructor(
    private apptService: CustomerAppointmentService,
    private http: HttpClient,
    private auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.customerId = this.auth.getUserId() || 0;
    this.load();
    this.loadPaymentHistory();
    this.startPayTimer();
    this.loadMyReviews();
    this.loadFavoriteServices();
  }

  ngOnDestroy(): void {
    if (this.payTimerHandle !== null) {
      clearInterval(this.payTimerHandle);
    }
  }

  /** Re-evaluate pay button enabled state every 60 seconds */
  private startPayTimer(): void {
    this.payTimerHandle = setInterval(() => {
      this.cdr.markForCheck();
    }, 60_000);
  }

  load(): void {
    this.loading = true;
    this.apptService.getHistory(this.customerId).subscribe({
      next: data => { this.appointments = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  loadMyReviews(): void {
    this.http.get<any[]>(`${BASE}/api/reviews/my`).subscribe({
      next: reviews => {
        this.myReviews.clear();
        reviews.forEach((r: any) => {
          if (r.appointmentId) this.myReviews.set(r.appointmentId, r);
        });
      },
      error: () => {}
    });
  }

  getReviewForAppt(apptId: number): any | null {
    return this.myReviews.get(apptId) || null;
  }

  loadPaymentHistory(): void {
    this.historyLoading = true;
    this.apptService.getHistory(this.customerId).subscribe({
      next: data => {
        this.paymentHistory = data
          .filter(a => a.status === 'COMPLETED')
          .map(a => ({
            service: a.serviceName,
            professional: a.professionalName,
            amount: a.servicePrice,
            date: a.scheduledAt,
            status: 'PAID'
          }));
        this.historyLoading = false;
      },
      error: () => { this.historyLoading = false; }
    });
  }

  get upcoming(): AppointmentResponse[] {
    return this.appointments.filter(a => a.status === 'PENDING' || a.status === 'CONFIRMED');
  }

  get past(): AppointmentResponse[] {
    return this.appointments.filter(a => a.status === 'COMPLETED');
  }

  get cancelled(): AppointmentResponse[] {
    return this.appointments.filter(a => a.status === 'CANCELLED');
  }

  // ── Payment guard ─────────────────────────────────────────────────────────

  /** Pay button enabled only at or after the appointment time */
  isPayButtonEnabled(appt: AppointmentResponse): boolean {
    if (!appt.scheduledAt) return false;
    return Date.now() >= new Date(appt.scheduledAt).getTime();
  }

  payButtonTooltip(appt: AppointmentResponse): string {
    if (this.isPayButtonEnabled(appt)) return '';
    return 'Payment available from ' + new Date(appt.scheduledAt).toLocaleString();
  }

  /** Entry point for Pay button */
  openPayGuarded(appt: AppointmentResponse): void {
    if (!this.isPayButtonEnabled(appt)) return;
    this.openPay(appt);
  }

  checkReviewExists(apptId: number, callback: (exists: boolean) => void): void {
    this.http.get<{ exists: boolean }>(`${BASE}/api/reviews/exists?appointmentId=${apptId}`)
      .subscribe({
        next: res => {
          this.reviewedAppointments.set(apptId, res.exists);
          callback(res.exists);
        },
        error: () => callback(false)
      });
  }

  /** Called after successful review submission — opens payment modal */
  onRatingSubmitted(): void {
    if (!this.pendingPayAppt) return;
    this.reviewedAppointments.set(this.pendingPayAppt.id, true);
    const appt = this.pendingPayAppt;
    this.pendingPayAppt = null;
    this.openPay(appt);
  }

  // ── Payment modal ─────────────────────────────────────────────────────────

  openPay(appt: AppointmentResponse): void {
    this.payingAppt = appt;
    this.payMethod = 'CASH';
    this.cardNumber = '';
    this.cardExpiry = '';
    this.cardCvv = '';
    this.payError = '';
    this.paySuccess = '';
  }

  closePay(): void { this.payingAppt = null; }

  onCardNumberInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.cardNumber = val.replace(/[^0-9]/g, '').slice(0, 12);
    (event.target as HTMLInputElement).value = this.cardNumber;
  }

  onCvvInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.cardCvv = val.replace(/[^0-9]/g, '').slice(0, 3);
    (event.target as HTMLInputElement).value = this.cardCvv;
  }

  get cardValid(): boolean {
    if (this.payMethod !== 'CARD') return true;
    return this.cardNumber.length === 12 && this.cardExpiry.length === 5 && this.cardCvv.length === 3;
  }

  confirmPay(): void {
    if (!this.payingAppt) return;
    this.payLoading = true;
    this.payError = '';

    const body = {
      appointmentId: this.payingAppt.id,
      amount: this.payingAppt.servicePrice,
      method: this.payMethod
    };

    this.http.post(`${BASE}/api/payments`, body).subscribe({
      next: () => {
        this.http.patch(`${BASE}/api/appointments/${this.payingAppt!.id}/complete`, {}).subscribe({
          next: (updated: any) => {
            const idx = this.appointments.findIndex(a => a.id === updated.id);
            if (idx !== -1) this.appointments[idx] = updated;
            this.payLoading = false;
            this.paySuccess = 'Payment successful! Service marked as completed.';
            setTimeout(() => { this.closePay(); this.load(); this.loadPaymentHistory(); }, 1500);
          },
          error: () => {
            this.payLoading = false;
            this.paySuccess = 'Payment done! Refreshing...';
            setTimeout(() => { this.closePay(); this.load(); this.loadPaymentHistory(); }, 1200);
          }
        });
      },
      error: (err: any) => {
        this.payError = err?.error?.message || 'Payment failed. Please try again.';
        this.payLoading = false;
      }
    });
  }

  // ── Review modal ──────────────────────────────────────────────────────────

  openReview(appt: AppointmentResponse): void {
    this.reviewingAppt = appt;
    this.reviewRating = 5;
    this.reviewQualityRating = 5;
    this.reviewTimelinessRating = 5;
    this.reviewProfessionalismRating = 5;
    this.reviewComment = '';
    this.reviewPhotos = [];
    this.reviewPhotoPreview = [];
    this.reviewError = '';
    this.reviewSuccess = '';
    this.reviewMode = 'create';
  }

  openUpdateReview(appt: AppointmentResponse, rev: any): void {
    this.reviewingAppt = appt;
    this.reviewRating = rev.rating || 5;
    this.reviewQualityRating = rev.qualityRating || 5;
    this.reviewTimelinessRating = rev.timelinessRating || 5;
    this.reviewProfessionalismRating = rev.professionalismRating || 5;
    this.reviewComment = rev.comment || '';
    this.reviewPhotos = [];
    this.reviewPhotoPreview = [];
    this.reviewError = '';
    this.reviewSuccess = '';
    this.reviewMode = 'update';
    // Store the review object for the PATCH call
    (this.reviewingAppt as any).existingReview = rev;
  }

  closeReview(): void {
    this.reviewingAppt = null;
    this.pendingPayAppt = null;
    this.reviewPhotos = [];
    this.reviewPhotoPreview = [];
  }

  setRating(r: number): void { this.reviewRating = r; }

  setQualityRating(r: number): void { this.reviewQualityRating = r; }
  setTimelinessRating(r: number): void { this.reviewTimelinessRating = r; }
  setProfessionalismRating(r: number): void { this.reviewProfessionalismRating = r; }

  onPhotoSelected(event: Event): void {
    const files = (event.target as HTMLInputElement).files;
    if (!files) return;
    this.reviewPhotos = Array.from(files);
    // Convert to data-URLs for persistent storage
    this.reviewPhotoPreview = [];
    this.reviewPhotos.forEach(file => {
      const reader = new FileReader();
      reader.onload = e => {
        this.reviewPhotoPreview.push(e.target?.result as string);
      };
      reader.readAsDataURL(file);
    });
  }

  submitReview(): void {
    if (!this.reviewingAppt) return;
    this.reviewLoading = true;
    this.reviewError = '';

    if (this.reviewMode === 'update' && (this.reviewingAppt as any).existingReview) {
      this.doUpdateReview();
    } else {
      this.doCreateReview();
    }
  }

  private doCreateReview(): void {
    if (!this.reviewingAppt) return;

    // Capture photos before async call (they may be cleared)
    const photosToStore = [...this.reviewPhotoPreview];

    if (this.reviewPhotos.length > 0) {
      // Send as multipart so backend saves the actual files
      const formData = new FormData();
      const data = {
        professionalId: this.reviewingAppt.professionalId,
        appointmentId: this.reviewingAppt.id,
        qualityRating: this.reviewQualityRating,
        timelinessRating: this.reviewTimelinessRating,
        professionalismRating: this.reviewProfessionalismRating,
        comment: this.reviewComment
      };
      formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
      this.reviewPhotos.forEach(f => formData.append('photos', f, f.name));

      this.http.post<any>(`${BASE}/api/reviews`, formData).subscribe({
        next: () => { this.onReviewSuccess('Review submitted!'); },
        error: (err: any) => {
          this.reviewError = err?.error?.message || 'Failed to submit review.';
          this.reviewLoading = false;
        }
      });
    } else {
      // No photos — plain JSON is fine
      const body = {
        professionalId: this.reviewingAppt.professionalId,
        appointmentId: this.reviewingAppt.id,
        qualityRating: this.reviewQualityRating,
        timelinessRating: this.reviewTimelinessRating,
        professionalismRating: this.reviewProfessionalismRating,
        comment: this.reviewComment
      };
      this.http.post<any>(`${BASE}/api/reviews`, body).subscribe({
        next: () => { this.onReviewSuccess('Review submitted!'); },
        error: (err: any) => {
          this.reviewError = err?.error?.message || 'Failed to submit review.';
          this.reviewLoading = false;
        }
      });
    }
  }

  private doUpdateReview(): void {
    if (!this.reviewingAppt) return;
    const existingReview = (this.reviewingAppt as any).existingReview;
    if (!existingReview) return;
    const reviewId = existingReview.id;

    if (this.reviewPhotos.length > 0) {
      // Send as multipart so backend saves the actual files
      const formData = new FormData();
      const data = {
        comment: this.reviewComment,
        qualityRating: this.reviewQualityRating,
        timelinessRating: this.reviewTimelinessRating,
        professionalismRating: this.reviewProfessionalismRating
      };
      formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
      this.reviewPhotos.forEach(f => formData.append('photos', f, f.name));

      this.http.patch<any>(`${BASE}/api/reviews/${reviewId}`, formData).subscribe({
        next: (updated: any) => {
          this.myReviews.set(this.reviewingAppt!.id, updated);
          this.onReviewSuccess('Review updated!');
        },
        error: (err: any) => {
          this.reviewError = err?.error?.message || 'Failed to update review.';
          this.reviewLoading = false;
        }
      });
    } else {
      this.http.patch<any>(`${BASE}/api/reviews/${reviewId}`, {
        comment: this.reviewComment,
        qualityRating: this.reviewQualityRating,
        timelinessRating: this.reviewTimelinessRating,
        professionalismRating: this.reviewProfessionalismRating
      }).subscribe({
        next: (updated: any) => {
          this.myReviews.set(this.reviewingAppt!.id, updated);
          this.onReviewSuccess('Review updated!');
        },
        error: (err: any) => {
          this.reviewError = err?.error?.message || 'Failed to update review.';
          this.reviewLoading = false;
        }
      });
    }
  }

  private onReviewSuccess(msg: string): void {
    this.reviewLoading = false;
    this.reviewSuccess = msg;
    if (this.reviewMode === 'create') {
      // Reload reviews to get the new one
      this.loadMyReviews();
      setTimeout(() => {
        this.reviewingAppt = null;
        this.onRatingSubmitted();
      }, 800);
    } else {
      // Reload reviews to get the updated one
      this.loadMyReviews();
      setTimeout(() => this.closeReview(), 1200);
    }
  }

  // ── Favorites ─────────────────────────────────────────────────────────────

  loadFavoriteServices(): void {
    if (!this.customerId) return;
    this.http.get<any[]>(`${BASE}/api/v1/customers/${this.customerId}/favorites/services`).subscribe({
      next: favs => {
        this.favoritedServices = {};
        favs.forEach(f => { this.favoritedServices[f.id] = true; });
      },
      error: () => {}
    });
  }

  toggleServiceFavorite(appt: AppointmentResponse): void {
    if (!appt.serviceId || !this.customerId) return;
    this.togglingFav[appt.serviceId] = true;
    this.http.post<any>(`${BASE}/api/v1/customers/${this.customerId}/favorites/services/${appt.serviceId}`, {}).subscribe({
      next: (res) => {
        this.favoritedServices[appt.serviceId] = res.favorited;
        this.togglingFav[appt.serviceId] = false;
      },
      error: () => { this.togglingFav[appt.serviceId] = false; }
    });
  }

  // ── Cancel ────────────────────────────────────────────────────────────────

  cancel(id: number): void {
    if (!confirm('Cancel this appointment?')) return;
    this.apptService.cancel(this.customerId, id).subscribe({
      next: () => this.load(),
      error: () => alert('Cannot cancel this appointment.')
    });
  }

  statusBadge(status: string): string {
    const m: Record<string, string> = {
      PENDING: 'warning text-dark', CONFIRMED: 'success',
      COMPLETED: 'primary', CANCELLED: 'danger'
    };
    return `badge bg-${m[status] ?? 'secondary'}`;
  }

  stars(n: number): number[] { return [1, 2, 3, 4, 5]; }
}
