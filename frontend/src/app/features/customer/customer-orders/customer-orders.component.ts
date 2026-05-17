import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

const BASE = 'http://localhost:8080';
const PAY_LATER_KEY = 'order_pay_later_pending';

@Component({
  selector: 'app-customer-orders',
  templateUrl: './customer-orders.component.html',
  styleUrls: ['./customer-orders.component.scss']
})
export class CustomerOrdersComponent implements OnInit {
  orders: any[] = [];
  loading = false;

  // ── Payment modal ─────────────────────────────────────────────────────────
  payingOrder: any = null;
  payMethod: 'CASH' | 'CARD' = 'CASH';
  cardNumber = '';
  cardExpiry = '';
  cardCvv = '';
  payLoading = false;
  payError = '';
  paySuccess = '';
  paySubmitted = false;
  orderSuccess: any = null;

  // Wallet
  walletBalance = 0;
  walletLoading = false;
  useWallet = false;

  // Pay Later
  payLaterEligible = false;
  payLaterChecked = false;
  payLaterReason = '';
  payLaterDeadline = '';
  usePayLater = false;
  payLaterLoading = false;

  // Pay Later pending order IDs (tracked locally — backend has no Pay Later for product orders)
  private payLaterPendingIds: Set<number> = new Set();

  // Review state — keyed by productId
  reviewOpen:       Record<number, boolean>  = {};
  reviewRating:     Record<number, number>   = {};
  reviewText:       Record<number, string>   = {};
  reviewSubmitting: Record<number, boolean>  = {};
  reviewSuccess:    Record<number, string>   = {};
  reviewError:      Record<number, string>   = {};
  existingReview:   Record<number, any>      = {};
  canReview:        Record<number, boolean>  = {};
  reviewMode:       Record<number, 'create' | 'update'> = {};

  // Settle Pay Later modal
  settlingOrder: any = null;
  settleMethod: 'CASH' | 'CARD' = 'CASH';
  settleCardNumber = '';
  settleCardExpiry = '';
  settleCardCvv = '';
  settleWalletBalance = 0;
  settleUseWallet = false;
  settleLoading = false;
  settleError = '';
  settleSuccess = '';
  settleSubmitted = false;

  // Favorite state — keyed by productId
  favorited:        Record<number, boolean>  = {};
  togglingFav:      Record<number, boolean>  = {};

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void {
    this.loadPayLaterPending();
    this.load();
  }

  // ── Pay Later local persistence ───────────────────────────────────────────

  private storageKey(): string {
    return `${PAY_LATER_KEY}_${this.auth.getUserId() ?? 'anon'}`;
  }

  private loadPayLaterPending(): void {
    try {
      const raw = sessionStorage.getItem(this.storageKey());
      const ids: number[] = raw ? JSON.parse(raw) : [];
      this.payLaterPendingIds = new Set(ids);
    } catch { this.payLaterPendingIds = new Set(); }
  }

  private savePayLaterPending(): void {
    try {
      sessionStorage.setItem(this.storageKey(), JSON.stringify([...this.payLaterPendingIds]));
    } catch {}
  }

  hasPayLaterPending(order: any): boolean {
    return this.payLaterPendingIds.has(order.orderId);
  }

  // ── Load ──────────────────────────────────────────────────────────────────

  load(): void {
    this.loading = true;
    this.http.get<any[]>(`${BASE}/api/products/orders`).subscribe({
      next: data => {
        this.orders = Array.isArray(data) ? data : [];
        this.loading = false;
        this.orders.forEach(o => {
          // Clean up delivered/cancelled orders from pay-later tracking
          if (o.status === 'DELIVERED' || o.status === 'CANCELLED') {
            this.payLaterPendingIds.delete(o.orderId);
          }
          if (o.status === 'DELIVERED' && o.productId) {
            this.checkCanReview(o.productId);
            this.loadExistingReview(o.productId);
            this.checkFavorited(o.productId);
          }
        });
        this.savePayLaterPending();
      },
      error: () => { this.orders = []; this.loading = false; }
    });
  }

  // ── Payment ───────────────────────────────────────────────────────────────

  openPayment(order: any): void {
    this.payingOrder = order;
    this.payMethod = 'CASH';
    this.cardNumber = '';
    this.cardExpiry = '';
    this.cardCvv = '';
    this.payError = '';
    this.paySuccess = '';
    this.paySubmitted = false;
    this.useWallet = false;
    this.walletBalance = 0;
    this.usePayLater = false;
    this.payLaterEligible = false;
    this.payLaterChecked = false;
    this.payLaterReason = '';
    this.orderSuccess = null;
    this.walletLoading = true;
    this.payLaterLoading = true;

    this.http.get<{ balance: number }>(`${BASE}/api/wallet/balance`).subscribe({
      next: d => { this.walletBalance = d.balance; this.walletLoading = false; },
      error: () => { this.walletBalance = 0; this.walletLoading = false; }
    });

    this.http.get<any>(`${BASE}/api/v1/customers/${this.auth.getUserId()}/loyalty`).subscribe({
      next: data => {
        const tier = data.tier || 'BRONZE';
        const points = data.points || 0;
        const cost = order.totalPrice || 0;
        const pointsValue = points / 10;
        const tierOk = tier === 'SILVER' || tier === 'GOLD' || tier === 'DIAMOND';
        const pointsOk = pointsValue >= cost / 2;
        this.payLaterEligible = tierOk && pointsOk;
        if (!tierOk) this.payLaterReason = 'Requires Silver tier or above (current: ' + tier + ')';
        else if (!pointsOk) this.payLaterReason = 'Need points worth ₹' + (cost / 2).toFixed(0) + ' (you have ₹' + pointsValue.toFixed(0) + ')';
        const d = new Date(); d.setHours(d.getHours() + 24);
        this.payLaterDeadline = d.toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
        this.payLaterChecked = true;
        this.payLaterLoading = false;
      },
      error: () => { this.payLaterChecked = true; this.payLaterLoading = false; }
    });
  }

  closePayment(): void { this.payingOrder = null; this.orderSuccess = null; }

  get walletDeduction(): number {
    if (!this.useWallet || !this.payingOrder) return 0;
    return Math.min(this.walletBalance, this.payingOrder.totalPrice);
  }

  get remainingAmount(): number {
    if (!this.payingOrder) return 0;
    return Math.max(0, this.payingOrder.totalPrice - this.walletDeduction);
  }

  get fullyPaidByWallet(): boolean {
    return this.useWallet && this.remainingAmount === 0;
  }

  get cardValid(): boolean {
    if (this.payMethod !== 'CARD') return true;
    return this.cardNumber.length === 12 && this.isValidExpiry(this.cardExpiry) && this.cardCvv.length === 3;
  }

  isValidExpiry(expiry: string): boolean {
    if (!/^\d{2}\/\d{2}$/.test(expiry)) return false;
    const [mm, yy] = expiry.split('/').map(Number);
    if (mm < 1 || mm > 12) return false;
    const now = new Date();
    const expDate = new Date(2000 + yy, mm - 1, 1);
    return expDate >= new Date(now.getFullYear(), now.getMonth(), 1);
  }

  onCardNumberInput(e: Event): void {
    const v = (e.target as HTMLInputElement).value;
    this.cardNumber = v.replace(/[^0-9]/g, '').slice(0, 12);
    (e.target as HTMLInputElement).value = this.cardNumber;
  }

  onCvvInput(e: Event): void {
    const v = (e.target as HTMLInputElement).value;
    this.cardCvv = v.replace(/[^0-9]/g, '').slice(0, 3);
    (e.target as HTMLInputElement).value = this.cardCvv;
  }

  onConfirmClick(): void {
    if (this.usePayLater) { this.confirmPayLater(); return; }
    if (!this.fullyPaidByWallet && this.payMethod === 'CARD') {
      this.paySubmitted = true;
      if (this.cardNumber.length !== 12 || !this.isValidExpiry(this.cardExpiry) || this.cardCvv.length !== 3) return;
    }
    this.confirmPayment();
  }

  confirmPayLater(): void {
    if (!this.payingOrder) return;
    this.payLoading = true;
    this.payError = '';
    this.paySuccess = `✅ Pay Later activated! ₹${this.payingOrder.totalPrice} due by ${this.payLaterDeadline}. Settle anytime before the deadline.`;
    // Track this order as pay-later pending locally
    this.payLaterPendingIds.add(this.payingOrder.orderId);
    this.savePayLaterPending();
    this.payLoading = false;
    setTimeout(() => { this.closePayment(); this.load(); }, 2500);
  }

  confirmPayment(): void {
    if (!this.payingOrder) return;
    this.payLoading = true;
    this.payError = '';
    this.http.post<any>(`${BASE}/api/products/orders/${this.payingOrder.orderId}/pay`, {
      method: this.fullyPaidByWallet ? 'WALLET' : this.payMethod,
      walletAmountUsed: this.walletDeduction
    }).subscribe({
      next: (paid) => {
        this.payLoading = false;
        this.payingOrder = null;
        this.orderSuccess = paid;
        this.load();
      },
      error: (e: any) => {
        this.payError = e?.error?.message || 'Payment failed. Please try again.';
        this.payLoading = false;
      }
    });
  }

  // ── Review ────────────────────────────────────────────────────────────────

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

  setRating(productId: number, r: number): void { this.reviewRating[productId] = r; }

  submitReview(order: any): void {
    const productId = order.productId;
    if (!this.reviewRating[productId] || this.reviewRating[productId] < 1) {
      this.reviewError[productId] = 'Please select a star rating.'; return;
    }
    this.reviewSubmitting[productId] = true;
    this.reviewError[productId] = '';
    this.reviewSuccess[productId] = '';
    const payload = { rating: this.reviewRating[productId], reviewText: this.reviewText[productId] || '' };
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

  // ── Favorites ─────────────────────────────────────────────────────────────

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

  // ── Settle Pay Later ──────────────────────────────────────────────────────

  openSettle(order: any): void {
    this.settlingOrder = order;
    this.settleMethod = 'CASH';
    this.settleCardNumber = '';
    this.settleCardExpiry = '';
    this.settleCardCvv = '';
    this.settleError = '';
    this.settleSuccess = '';
    this.settleUseWallet = false;
    this.settleWalletBalance = 0;
    this.settleLoading = false;
    this.settleSubmitted = false;
    this.http.get<{ balance: number }>(`${BASE}/api/wallet/balance`).subscribe({
      next: d => { this.settleWalletBalance = d.balance; },
      error: () => {}
    });
  }

  closeSettle(): void { this.settlingOrder = null; }

  get settleWalletDeduction(): number {
    if (!this.settleUseWallet || !this.settlingOrder) return 0;
    return Math.min(this.settleWalletBalance, this.settlingOrder.totalPrice);
  }

  get settleRemaining(): number {
    if (!this.settlingOrder) return 0;
    return Math.max(0, this.settlingOrder.totalPrice - this.settleWalletDeduction);
  }

  get settleFullyByWallet(): boolean {
    return this.settleUseWallet && this.settleRemaining === 0;
  }

  get settleCardValid(): boolean {
    if (this.settleMethod !== 'CARD') return true;
    return this.settleCardNumber.length === 12 && this.isValidExpiry(this.settleCardExpiry) && this.settleCardCvv.length === 3;
  }

  onSettleCardInput(e: Event): void {
    const v = (e.target as HTMLInputElement).value;
    this.settleCardNumber = v.replace(/[^0-9]/g, '').slice(0, 12);
    (e.target as HTMLInputElement).value = this.settleCardNumber;
  }

  onSettleCvvInput(e: Event): void {
    const v = (e.target as HTMLInputElement).value;
    this.settleCardCvv = v.replace(/[^0-9]/g, '').slice(0, 3);
    (e.target as HTMLInputElement).value = this.settleCardCvv;
  }

  onConfirmSettleClick(): void {
    if (!this.settleFullyByWallet && this.settleMethod === 'CARD') {
      this.settleSubmitted = true;
      if (this.settleCardNumber.length !== 12 || !this.isValidExpiry(this.settleCardExpiry) || this.settleCardCvv.length !== 3) return;
    }
    this.confirmSettle();
  }

  confirmSettle(): void {
    if (!this.settlingOrder) return;
    this.settleLoading = true;
    this.settleError = '';
    // Settle calls the same /pay endpoint — Pay Later was tracked locally
    this.http.post<any>(`${BASE}/api/products/orders/${this.settlingOrder.orderId}/pay`, {
      method: this.settleFullyByWallet ? 'WALLET' : this.settleMethod,
      walletAmountUsed: this.settleWalletDeduction
    }).subscribe({
      next: (paid) => {
        this.payLaterPendingIds.delete(this.settlingOrder.orderId);
        this.savePayLaterPending();
        this.settleLoading = false;
        this.settleSuccess = '✅ Pay Later settled successfully!';
        setTimeout(() => {
          this.closeSettle();
          this.orderSuccess = paid;
          this.load();
        }, 1800);
      },
      error: (e: any) => {
        this.settleError = e?.error?.message || 'Settlement failed. Please try again.';
        this.settleLoading = false;
      }
    });
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
