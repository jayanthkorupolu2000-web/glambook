import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

const API    = 'http://localhost:8080/api/v1/customers';
const WALLET = 'http://localhost:8080/api/wallet';

interface LoyaltySummary {
  points: number;
  tier: string;
  totalEarned: number;
  totalRedeemed: number;
  tierBenefits: string[];
  earlyAccessServices: string[];
  walletBalance: number;
  lastCreditedAmount?: number;
}

interface LoyaltyTransaction {
  id: number;
  type: 'EARN' | 'REDEEM';
  points: number;
  description: string;
  createdAt: string;
}

interface WalletTransaction {
  id: number;
  type: 'credit' | 'debit';
  amount: number;
  source: string;
  description: string;
  createdAt: string;
}

@Component({
  selector: 'app-customer-loyalty',
  templateUrl: './customer-loyalty.component.html'
})
export class CustomerLoyaltyComponent implements OnInit {
  summary: LoyaltySummary | null = null;
  transactions: LoyaltyTransaction[] = [];
  walletTransactions: WalletTransaction[] = [];
  walletBalance = 0;
  loading = false;
  redeeming = false;
  redeemPoints = 100;
  redeemError = '';
  redeemSuccess = '';
  activeTab: 'overview' | 'history' | 'benefits' | 'early-access' = 'overview';

  tiers = [
    { name: 'BRONZE',   label: 'Bronze',   min: 0,    max: 499,      color: '#cd7f32', icon: '🥉' },
    { name: 'SILVER',   label: 'Silver',   min: 500,  max: 1499,     color: '#9e9e9e', icon: '🥈' },
    { name: 'GOLD',     label: 'Gold',     min: 1500, max: 2999,     color: '#f5a623', icon: '🥇' },
    { name: 'PLATINUM', label: 'Platinum', min: 3000, max: Infinity, color: '#244AFD', icon: '💎' }
  ];

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.loading = true;
    this.http.get<LoyaltySummary>(`${API}/${id}/loyalty`).subscribe({
      next: data => {
        this.summary = data;
        this.walletBalance = data.walletBalance ?? 0;
        this.loading = false;
        this.loadTransactions(id);
        this.loadWalletTransactions();
      },
      error: () => {
        this.summary = { points: 0, tier: 'BRONZE', totalEarned: 0, totalRedeemed: 0,
                         tierBenefits: [], earlyAccessServices: [], walletBalance: 0 };
        this.loading = false;
      }
    });
  }

  loadTransactions(id: number): void {
    this.http.get<LoyaltyTransaction[]>(`${API}/${id}/loyalty/transactions`).subscribe({
      next: data => { this.transactions = Array.isArray(data) ? data : []; },
      error: () => { this.transactions = []; }
    });
  }

  loadWalletTransactions(): void {
    this.http.get<WalletTransaction[]>(`${WALLET}/transactions`).subscribe({
      next: data => { this.walletTransactions = Array.isArray(data) ? data : []; },
      error: () => { this.walletTransactions = []; }
    });
  }

  refreshWalletBalance(): void {
    this.http.get<{ balance: number; currency: string }>(`${WALLET}/balance`).subscribe({
      next: data => { this.walletBalance = data.balance; },
      error: () => {}
    });
  }

  get currentTier(): any {
    return this.tiers.find(t => t.name === this.summary?.tier) || this.tiers[0];
  }

  get nextTier(): any {
    const idx = this.tiers.indexOf(this.currentTier);
    return idx < this.tiers.length - 1 ? this.tiers[idx + 1] : null;
  }

  get progressPct(): number {
    const t = this.currentTier;
    if (!this.summary) return 0;
    if (t.max === Infinity) return 100;
    const earned = this.summary.totalEarned;
    return Math.min(100, Math.round(((earned - t.min) / (t.max - t.min)) * 100));
  }

  get discountValue(): number {
    return Math.floor(this.redeemPoints / 100) * 10;
  }

  get tierIndex(): number {
    return this.tiers.findIndex(t => t.name === this.summary?.tier);
  }

  isTierCompleted(tierIdx: number): boolean {
    return tierIdx < this.tierIndex;
  }

  setTab(tab: string): void {
    this.activeTab = tab as any;
    if (tab === 'history') this.loadWalletTransactions();
  }

  redeem(): void {
    this.redeemError = '';
    this.redeemSuccess = '';
    if (!this.redeemPoints || this.redeemPoints < 100) { this.redeemError = 'Minimum 100 points required.'; return; }
    if (this.redeemPoints % 100 !== 0) { this.redeemError = 'Points must be in multiples of 100.'; return; }
    if (this.summary && this.redeemPoints > this.summary.points) { this.redeemError = 'Not enough points.'; return; }
    const id = this.auth.getUserId();
    if (!id) return;
    this.redeeming = true;
    this.http.post<LoyaltySummary>(`${API}/${id}/loyalty/redeem`, { points: this.redeemPoints }).subscribe({
      next: updated => {
        const credited = updated.lastCreditedAmount ?? this.discountValue;
        this.summary = updated;
        this.walletBalance = updated.walletBalance ?? this.walletBalance;
        this.redeemSuccess = `✅ ₹${credited} added to your wallet!`;
        this.redeemPoints = 100;
        this.redeeming = false;
        this.loadTransactions(id);
        this.loadWalletTransactions();
      },
      error: (e) => { this.redeemError = e?.error?.message || 'Redemption failed.'; this.redeeming = false; }
    });
  }
}
