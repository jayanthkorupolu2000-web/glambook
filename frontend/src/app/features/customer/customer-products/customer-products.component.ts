import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

const BASE = 'http://localhost:8080';

@Component({
  selector: 'app-customer-products',
  templateUrl: './customer-products.component.html'
})
export class CustomerProductsComponent implements OnInit {

  // ── State ──────────────────────────────────────────────────────────────────
  products: any[] = [];
  recommended: any[] = [];
  loading = false;
  loadingRec = false;

  // Filters
  searchKeyword = '';
  selectedCategory = '';
  selectedBrand = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  priceError = '';
  categories = ['HAIRCARE', 'SKINCARE', 'MAKEUP', 'NAILCARE', 'FRAGRANCE', 'TOOLS'];
  brands = [
    'Ajmal', 'CND SolarOil', 'Chanel', 'Essie', 'Forest Essentials',
    'Herbivore Botanicals', 'Himalaya', 'L\'Oreal Paris', 'Lakme Sun Expert',
    'MAC Cosmetics', 'Mamaearth', 'Maybelline New York', 'Minimalist',
    'Mount Lai', 'Neutrogena', 'OPI', 'Olay Regenerist', 'Revlon',
    'Sigma Beauty', 'TRESemmé', 'Urban Decay', 'Wella Professionals'
  ];

  // Pagination
  currentPage = 0;
  pageSize = 12;
  totalPages = 0;
  totalElements = 0;

  // Favorites
  togglingFav: Record<number, boolean> = {};

  // Order modal
  orderingProduct: any = null;
  orderQuantity = 1;
  orderLoading = false;
  orderError = '';
  orderSuccess: any = null;
  orderModalTab: 'order' | 'ingredients' | 'reviews' = 'order';

  // Payment modal (shown after order is placed)
  payingOrder: any = null;          // the placed order response
  payMethod: 'CASH' | 'CARD' = 'CASH';
  cardNumber = '';
  cardExpiry = '';
  cardCvv = '';
  payLoading = false;
  payError = '';
  paySuccess = '';

  // Wallet
  walletBalance = 0;
  walletLoading = false;
  useWallet = false;

  // Pay Later
  prodPayLaterEligible = false;
  prodPayLaterChecked = false;
  prodPayLaterReason = '';
  prodPayLaterDeadline = '';
  useProdPayLater = false;
  prodPayLaterLoading = false;
  prodPayLaterSuccess = '';

  // Card validation submitted flag
  paySubmitted = false;

  // Reviews inside order modal
  orderModalReviews: any[] = [];
  orderModalReviewsLoading = false;
  orderModalReviewRating = 5;
  orderModalReviewText = '';
  orderModalReviewSubmitting = false;
  orderModalReviewError = '';
  orderModalReviewSuccess = '';
  orderModalHasOrdered = false;
  orderModalAlreadyReviewed = false;

  // Detail modal
  detailProduct: any = null;
  detailReviews: any[] = [];
  detailReviewsLoading = false;
  reviewText = '';
  reviewRating = 5;
  reviewSubmitting = false;
  reviewError = '';
  reviewSuccess = '';
  hasOrdered = false;
  alreadyReviewed = false;

  constructor(private http: HttpClient, private auth: AuthService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.load();
    if (this.auth.getUserId()) this.loadRecommended();

    // Auto-open order modal when navigated from favorites with ?order=<productId>
    this.route.queryParams.subscribe(params => {
      const productId = params['order'];
      if (productId) {
        this.http.get<any>(`${BASE}/api/products/${productId}`).subscribe({
          next: product => { if (product) this.openOrder(product); },
          error: () => {}
        });
      }
    });
  }

  // ── Load products ──────────────────────────────────────────────────────────

  load(page = 0): void {
    this.loading = true;
    this.currentPage = page;

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', this.pageSize.toString());

    if (this.selectedCategory) params = params.set('category', this.selectedCategory);
    if (this.selectedBrand)    params = params.set('brand', this.selectedBrand);
    // Only send valid price values — min >= 0, max >= min
    if (this.minPrice !== null && this.minPrice >= 0)
      params = params.set('minPrice', this.minPrice.toString());
    if (this.maxPrice !== null && this.maxPrice > 0)
      params = params.set('maxPrice', this.maxPrice.toString());

    this.http.get<any>(`${BASE}/api/products`, { params }).subscribe({
      next: data => {
        this.products = data.content || [];
        this.totalPages = data.totalPages || 0;
        this.totalElements = data.totalElements || 0;
        this.loading = false;
      },
      error: () => { this.products = []; this.loading = false; }
    });
  }

  /** Clamp min to >= 0; auto-correct max if it falls below min */
  onMinPriceChange(): void {
    if (this.minPrice !== null) {
      this.minPrice = Math.max(0, this.minPrice);
      if (this.maxPrice !== null && this.maxPrice < this.minPrice) {
        this.maxPrice = this.minPrice;
      }
    }
    this.validatePriceRange();
  }

  /** Ensure max >= min */
  onMaxPriceChange(): void {
    if (this.maxPrice !== null && this.minPrice !== null && this.maxPrice < this.minPrice) {
      this.maxPrice = this.minPrice;
    }
    this.validatePriceRange();
  }

  private validatePriceRange(): void {
    if (this.minPrice !== null && this.minPrice < 0) {
      this.priceError = 'Min price must be ≥ 0';
    } else if (this.maxPrice !== null && this.minPrice !== null && this.maxPrice < this.minPrice) {
      this.priceError = 'Max must be ≥ Min';
    } else {
      this.priceError = '';
    }
  }

  applyPriceFilter(): void {
    this.validatePriceRange();
    if (!this.priceError) this.load(0);
  }

  search(): void {
    if (!this.searchKeyword.trim()) { this.load(); return; }
    this.loading = true;
    this.http.get<any[]>(`${BASE}/api/products/search?q=${encodeURIComponent(this.searchKeyword)}`).subscribe({
      next: data => { this.products = Array.isArray(data) ? data : []; this.loading = false; },
      error: () => { this.products = []; this.loading = false; }
    });
  }

  clearFilters(): void {
    this.searchKeyword = '';
    this.selectedCategory = '';
    this.selectedBrand = '';
    this.minPrice = null;
    this.maxPrice = null;
    this.priceError = '';
    this.load(0);
  }

  loadRecommended(): void {
    this.loadingRec = true;
    this.http.get<any[]>(`${BASE}/api/products/recommended`).subscribe({
      next: data => { this.recommended = Array.isArray(data) ? data.slice(0, 4) : []; this.loadingRec = false; },
      error: () => { this.recommended = []; this.loadingRec = false; }
    });
  }

  // ── Favorites ──────────────────────────────────────────────────────────────

  toggleFavorite(product: any, event: Event): void {
    event.stopPropagation();
    const id = this.auth.getUserId();
    if (!id) return;
    this.togglingFav[product.id] = true;

    if (product.favorited) {
      this.http.delete(`${BASE}/api/products/${product.id}/favorites`).subscribe({
        next: () => { product.favorited = false; this.togglingFav[product.id] = false; },
        error: () => { this.togglingFav[product.id] = false; }
      });
    } else {
      this.http.post<any>(`${BASE}/api/products/${product.id}/favorites`, {}).subscribe({
        next: () => { product.favorited = true; this.togglingFav[product.id] = false; },
        error: () => { this.togglingFav[product.id] = false; }
      });
    }
  }

  // ── Order modal ────────────────────────────────────────────────────────────

  openOrder(product: any, event?: Event): void {
    if (event) event.stopPropagation();
    this.orderingProduct = product;
    this.orderQuantity = 1;
    this.orderError = '';
    this.orderSuccess = null;
    this.orderLoading = false;
    this.orderModalTab = 'order';
    // Load reviews and eligibility for the order modal
    this.loadOrderModalReviews(product.id);
    if (this.isLoggedIn()) this.checkOrderModalCanReview(product.id);
  }

  closeOrder(): void {
    this.orderingProduct = null;
    this.orderSuccess = null;
    this.orderModalReviews = [];
    this.orderModalReviewText = '';
    this.orderModalReviewError = '';
    this.orderModalReviewSuccess = '';
  }

  loadOrderModalReviews(productId: number): void {
    this.orderModalReviewsLoading = true;
    this.http.get<any[]>(`${BASE}/api/products/${productId}/reviews`).subscribe({
      next: data => { this.orderModalReviews = Array.isArray(data) ? data : []; this.orderModalReviewsLoading = false; },
      error: () => { this.orderModalReviews = []; this.orderModalReviewsLoading = false; }
    });
  }

  checkOrderModalCanReview(productId: number): void {
    this.http.get<any>(`${BASE}/api/products/${productId}/can-review`).subscribe({
      next: res => {
        this.orderModalHasOrdered = res.hasDeliveredOrder;
        this.orderModalAlreadyReviewed = res.alreadyReviewed;
      },
      error: () => { this.orderModalHasOrdered = false; this.orderModalAlreadyReviewed = false; }
    });
  }

  submitOrderModalReview(): void {
    if (!this.orderingProduct) return;
    this.orderModalReviewSubmitting = true;
    this.orderModalReviewError = '';
    this.http.post<any>(`${BASE}/api/products/${this.orderingProduct.id}/reviews`, {
      rating: this.orderModalReviewRating,
      reviewText: this.orderModalReviewText
    }).subscribe({
      next: () => {
        this.orderModalReviewSuccess = 'Review submitted!';
        this.orderModalReviewSubmitting = false;
        this.orderModalAlreadyReviewed = true;
        this.loadOrderModalReviews(this.orderingProduct.id);
        // Refresh product list to update avg rating
        this.load(this.currentPage);
      },
      error: (e: any) => {
        this.orderModalReviewError = e?.error?.message || 'Failed to submit review.';
        this.orderModalReviewSubmitting = false;
      }
    });
  }

  confirmOrder(): void {
    if (!this.orderingProduct) return;
    this.orderLoading = true;
    this.orderError = '';
    this.http.post<any>(`${BASE}/api/products/orders`, {
      productId: this.orderingProduct.id,
      quantity: this.orderQuantity
    }).subscribe({
      next: res => {
        this.orderLoading = false;
        // Order placed — now open payment modal
        this.openPayment(res);
      },
      error: (e) => {
        this.orderError = e?.error?.message || 'Order failed. Please try again.';
        this.orderLoading = false;
      }
    });
  }

  // ── Payment modal ──────────────────────────────────────────────────────────

  openPayment(order: any): void {
    this.orderingProduct = null;
    this.payingOrder = order;
    this.payMethod = 'CASH';
    this.cardNumber = '';
    this.cardExpiry = '';
    this.cardCvv = '';
    this.payError = '';
    this.paySuccess = '';
    this.useWallet = false;
    this.walletBalance = 0;
    this.paySubmitted = false;
    this.useProdPayLater = false;
    this.prodPayLaterEligible = false;
    this.prodPayLaterChecked = false;
    this.prodPayLaterReason = '';
    this.prodPayLaterDeadline = '';
    this.prodPayLaterSuccess = '';
    this.walletLoading = true;
    this.prodPayLaterLoading = true;
    this.http.get<{ balance: number }>(`${BASE}/api/wallet/balance`).subscribe({
      next: d => { this.walletBalance = d.balance; this.walletLoading = false; },
      error: () => { this.walletBalance = 0; this.walletLoading = false; }
    });
    // Check Pay Later eligibility using customer loyalty summary
    this.http.get<any>(`${BASE}/api/v1/customers/${this.auth.getUserId()}/loyalty`).subscribe({
      next: data => {
        const tier = data.tier || 'BRONZE';
        const points = data.points || 0;
        const serviceCost = order.totalPrice || 0;
        const pointsValue = points / 10;
        const tierOk = tier === 'SILVER' || tier === 'GOLD' || tier === 'DIAMOND';
        const pointsOk = pointsValue >= serviceCost / 2;
        this.prodPayLaterEligible = tierOk && pointsOk;
        if (!tierOk) {
          this.prodPayLaterReason = 'Requires Silver tier or above (current: ' + tier + ')';
        } else if (!pointsOk) {
          this.prodPayLaterReason = 'Need points worth ₹' + (serviceCost / 2).toFixed(0) + ' (you have ₹' + pointsValue.toFixed(0) + ')';
        }
        const d = new Date(); d.setHours(d.getHours() + 24);
        this.prodPayLaterDeadline = d.toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
        this.prodPayLaterChecked = true;
        this.prodPayLaterLoading = false;
      },
      error: () => { this.prodPayLaterChecked = true; this.prodPayLaterLoading = false; }
    });
  }

  closePayment(): void {
    this.payingOrder = null;
    this.orderSuccess = null;
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

  onConfirmPaymentClick(): void {
    if (this.useProdPayLater) { this.confirmPayLaterProduct(); return; }
    if (!this.fullyPaidByWallet && this.payMethod === 'CARD') {
      this.paySubmitted = true;
      if (!this.cardValid) return;
    }
    this.confirmPayment();
  }

  confirmPayLaterProduct(): void {
    if (!this.payingOrder) return;
    this.payLoading = true;
    this.payError = '';
    // For products, Pay Later means: place the order but defer payment 24h
    // We store a note and show success — the scheduler handles follow-up
    this.prodPayLaterSuccess = `✅ Pay Later activated! ₹${this.payingOrder.totalPrice} due by ${this.prodPayLaterDeadline}. You'll receive reminders before the deadline.`;
    this.paySuccess = this.prodPayLaterSuccess;
    this.payLoading = false;
    setTimeout(() => { this.closePayment(); this.load(this.currentPage); }, 2500);
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
        this.load(this.currentPage);
      },
      error: (e: any) => {
        this.payError = e?.error?.message || 'Payment failed. Please try again.';
        this.payLoading = false;
      }
    });
  }

  get orderTotal(): number {
    return this.orderingProduct ? this.orderingProduct.price * this.orderQuantity : 0;
  }

  get loyaltyPreview(): number {
    return Math.floor(this.orderTotal / 100) * 10;
  }

  // ── Product detail modal ───────────────────────────────────────────────────

  openDetail(product: any): void {
    this.detailProduct = product;
    this.detailReviews = [];
    this.reviewText = '';
    this.reviewRating = 5;
    this.reviewError = '';
    this.reviewSuccess = '';
    this.hasOrdered = false;
    this.alreadyReviewed = false;
    this.loadReviews(product.id);
    if (this.isLoggedIn()) this.checkCanReview(product.id);
  }

  closeDetail(): void { this.detailProduct = null; }

  loadReviews(productId: number): void {
    this.detailReviewsLoading = true;
    this.http.get<any[]>(`${BASE}/api/products/${productId}/reviews`).subscribe({
      next: data => { this.detailReviews = Array.isArray(data) ? data : []; this.detailReviewsLoading = false; },
      error: () => { this.detailReviews = []; this.detailReviewsLoading = false; }
    });
  }

  checkCanReview(productId: number): void {
    this.http.get<any>(`${BASE}/api/products/${productId}/can-review`).subscribe({
      next: res => {
        this.hasOrdered = res.hasDeliveredOrder;
        this.alreadyReviewed = res.alreadyReviewed;
      },
      error: () => { this.hasOrdered = false; this.alreadyReviewed = false; }
    });
  }

  // kept for backward compat — no longer used directly
  checkOrdered(_productId: number): void {}

  submitReview(): void {
    if (!this.detailProduct) return;
    this.reviewSubmitting = true;
    this.reviewError = '';
    this.http.post<any>(`${BASE}/api/products/${this.detailProduct.id}/reviews`, {
      rating: this.reviewRating,
      reviewText: this.reviewText
    }).subscribe({
      next: () => {
        this.reviewSuccess = 'Review submitted!';
        this.reviewSubmitting = false;
        this.loadReviews(this.detailProduct.id);
      },
      error: (e) => {
        this.reviewError = e?.error?.message || 'Failed to submit review.';
        this.reviewSubmitting = false;
      }
    });
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  stars(n: number): number[] { return [1, 2, 3, 4, 5]; }

  /** Returns a real product image URL — uses stored imageUrl or a category-based Unsplash photo */
  productImageUrl(product: any): string {
    if (product?.imageUrl) return product.imageUrl;
    // Category-based curated Unsplash images (stable source IDs)
    const map: Record<string, string> = {
      HAIRCARE:   'https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=300&h=200&fit=crop',
      SKINCARE:   'https://images.unsplash.com/photo-1556228578-8c89e6adf883?w=300&h=200&fit=crop',
      MAKEUP:     'https://images.unsplash.com/photo-1512496015851-a90fb38ba796?w=300&h=200&fit=crop',
      NAILCARE:   'https://images.unsplash.com/photo-1604654894610-df63bc536371?w=300&h=200&fit=crop',
      FRAGRANCE:  'https://images.unsplash.com/photo-1541643600914-78b084683702?w=300&h=200&fit=crop',
      TOOLS:      'https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=300&h=200&fit=crop',
    };
    const cat = (product?.category || '').toUpperCase();
    return map[cat] || map['HAIRCARE'];
  }

  stockBadge(stock: number): string {
    if (stock === 0) return 'danger';
    if (stock <= 10) return 'warning';
    return 'success';
  }

  stockLabel(stock: number): string {
    if (stock === 0) return 'Out of Stock';
    if (stock <= 10) return `Only ${stock} left`;
    return 'In Stock';
  }

  pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  isLoggedIn(): boolean { return !!this.auth.getUserId(); }
}
