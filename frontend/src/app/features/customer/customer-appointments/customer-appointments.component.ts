import { HttpClient } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { AppointmentResponse } from '../../../models/customer.model';
import { AuthService } from '../../../services/auth.service';
import { CustomerAppointmentService } from '../../../services/customer-appointment.service';
import { ReviewPhotoStoreService } from '../../../services/review-photo-store.service';

const BASE = 'http://localhost:8080';

@Component({
  selector: 'app-customer-appointments',
  templateUrl: './customer-appointments.component.html',
  styleUrls: ['./customer-appointments.component.css']
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
  paySubmitted = false;

  // Wallet
  walletBalance = 0;
  walletLoading = false;
  useWallet = false;

  // Payment history
  paymentHistory: any[] = [];
  historyLoading = false;

  // Reviews map: appointmentId → review
  myReviews: Map<number, any> = new Map();

  // Review modal
  reviewingAppt: AppointmentResponse | null = null;
  reviewRating = 0;
  reviewQualityRating = 0;
  reviewTimelinessRating = 0;
  reviewProfessionalismRating = 0;
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
    private cdr: ChangeDetectorRef,
    private photoStore: ReviewPhotoStoreService
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
    this.useWallet = false;
    this.walletBalance = 0;
    this.paySubmitted = false;
    this.walletLoading = true;
    this.http.get<{ balance: number; currency: string }>(`${BASE}/api/wallet/balance`).subscribe({
      next: data => { this.walletBalance = data.balance; this.walletLoading = false; },
      error: () => { this.walletBalance = 0; this.walletLoading = false; }
    });
  }

  closePay(): void { this.payingAppt = null; this.paySubmitted = false; }

  isValidExpiry(expiry: string): boolean {
    if (!/^\d{2}\/\d{2}$/.test(expiry)) return false;
    const [mm, yy] = expiry.split('/').map(Number);
    if (mm < 1 || mm > 12) return false;
    const now = new Date();
    const expDate = new Date(2000 + yy, mm - 1, 1);
    return expDate >= new Date(now.getFullYear(), now.getMonth(), 1);
  }

  onConfirmPayClick(): void {
    if (!this.fullyPaidByWallet && this.payMethod === 'CARD') {
      this.paySubmitted = true;
      if (this.cardNumber.length !== 12 || !this.isValidExpiry(this.cardExpiry) || this.cardCvv.length !== 3) {
        return;
      }
    }
    this.confirmPay();
  }

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

  /** Amount deducted from wallet (capped at total) */
  get walletDeduction(): number {
    if (!this.useWallet || !this.payingAppt) return 0;
    return Math.min(this.walletBalance, this.payingAppt.servicePrice);
  }

  /** Remaining amount to pay via Cash/Card */
  get remainingAmount(): number {
    if (!this.payingAppt) return 0;
    return Math.max(0, this.payingAppt.servicePrice - this.walletDeduction);
  }

  /** True when wallet covers the full amount */
  get fullyPaidByWallet(): boolean {
    return this.useWallet && this.remainingAmount === 0;
  }

  confirmPay(): void {
    if (!this.payingAppt) return;
    this.payLoading = true;
    this.payError = '';

    const walletAmountUsed = this.walletDeduction;
    const remaining = this.remainingAmount;

    const body: any = {
      appointmentId: this.payingAppt.id,
      amount: remaining,
      method: this.fullyPaidByWallet ? 'WALLET' : this.payMethod,
      walletAmountUsed: walletAmountUsed
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
    this.reviewRating = 0;
    this.reviewQualityRating = 0;
    this.reviewTimelinessRating = 0;
    this.reviewProfessionalismRating = 0;
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
    this.reviewPhotoPreview = [];

    // Read all files as data-URLs — wait for ALL to complete before marking ready
    const readers = this.reviewPhotos.map((file, idx) =>
      new Promise<void>(resolve => {
        const reader = new FileReader();
        reader.onload = e => {
          this.reviewPhotoPreview[idx] = e.target?.result as string;
          resolve();
        };
        reader.readAsDataURL(file);
      })
    );
    Promise.all(readers).then(() => {
      this.reviewPhotoPreview = [...this.reviewPhotoPreview]; // trigger change detection
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

    if (this.reviewPhotos.length > 0) {
      // Read all photos as data-URLs synchronously before submitting
      // so localStorage store is always populated correctly
      this.readAllPhotosAsDataUrls(this.reviewPhotos).then(dataUrls => {
        const formData = new FormData();
        const data = {
          professionalId: this.reviewingAppt!.professionalId,
          appointmentId: this.reviewingAppt!.id,
          qualityRating: this.reviewQualityRating,
          timelinessRating: this.reviewTimelinessRating,
          professionalismRating: this.reviewProfessionalismRating,
          comment: this.reviewComment
        };
        formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
        this.reviewPhotos.forEach(f => formData.append('photos', f, f.name));

        this.http.post<any>(`${BASE}/api/reviews`, formData).subscribe({
          next: (created: any) => {
            if (created?.id && dataUrls.length > 0) {
              this.photoStore.savePhotos(created.id, dataUrls);
            }
            this.onReviewSuccess('Review submitted!');
          },
          error: (err: any) => {
            this.reviewError = err?.error?.message || 'Failed to submit review.';
            this.reviewLoading = false;
          }
        });
      });
    } else {
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
      this.readAllPhotosAsDataUrls(this.reviewPhotos).then(dataUrls => {
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
            if (dataUrls.length > 0) {
              this.photoStore.appendPhotos(reviewId, dataUrls);
            }
            this.myReviews.set(this.reviewingAppt!.id, updated);
            this.onReviewSuccess('Review updated!');
          },
          error: (err: any) => {
            this.reviewError = err?.error?.message || 'Failed to update review.';
            this.reviewLoading = false;
          }
        });
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

  /** Read an array of File objects into base64 data-URLs, preserving order */
  private readAllPhotosAsDataUrls(files: File[]): Promise<string[]> {
    return Promise.all(
      files.map(file => new Promise<string>(resolve => {
        const reader = new FileReader();
        reader.onload = e => resolve(e.target?.result as string);
        reader.readAsDataURL(file);
      }))
    );
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

  resolveReviewPhotoUrl(url: string): string {
    if (!url) return '';
    if (url.startsWith('http')) return url;
    return `http://localhost:8080${url}`;
  }
}
