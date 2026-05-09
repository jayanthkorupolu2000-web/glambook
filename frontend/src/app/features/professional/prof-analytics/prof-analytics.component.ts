import { Component, OnInit } from '@angular/core';
import { ProfessionalAnalyticsResponse } from '../../../models/professional.model';
import { AuthService } from '../../../services/auth.service';
import { ProfessionalAnalyticsService } from '../../../services/professional-analytics.service';

@Component({
  selector: 'app-prof-analytics',
  templateUrl: './prof-analytics.component.html',
  styleUrls: ['./prof-analytics.component.css']
})
export class ProfAnalyticsComponent implements OnInit {
  analytics: ProfessionalAnalyticsResponse | null = null;
  loading = false;
  error = '';
  profId = 0;

  constructor(
    private analyticsService: ProfessionalAnalyticsService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.analyticsService.getAnalytics(this.profId).subscribe({
      next: data => {
        this.analytics = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Analytics error:', err);
        this.error = `Failed to load analytics (${err?.status || 'network error'}).`;
        this.loading = false;
        // Show empty analytics so the page isn't blank
        this.analytics = {
          professionalId: this.profId,
          totalAppointments: 0,
          completedAppointments: 0,
          cancelledAppointments: 0,
          totalEarnings: 0,
          averageRating: 0,
          totalReviews: 0,
          clientRetentionRate: 0,
          popularServices: [],
          peakBookingDay: 'N/A',
          peakBookingHour: 0,
          monthlyEarnings: {},
          reportGeneratedAt: new Date().toISOString()
        } as any;
      }
    });
  }

  monthEntries(): { month: string; amount: number }[] {
    if (!this.analytics?.monthlyEarnings) return [];
    return Object.entries(this.analytics.monthlyEarnings)
      .map(([month, amount]) => ({ month, amount: Number(amount) }));
  }

  maxEarning(): number {
    const entries = this.monthEntries();
    if (!entries.length) return 1;
    return Math.max(...entries.map(e => e.amount), 1);
  }

  barWidth(amount: number): number {
    return Math.round((amount / this.maxEarning()) * 100);
  }

  completionRate(): number {
    if (!this.analytics || !this.analytics.totalAppointments) return 0;
    return Math.round((this.analytics.completedAppointments / this.analytics.totalAppointments) * 100);
  }

  formatHour(h: number): string {
    if (h === 0) return '12 AM';
    if (h < 12) return `${h} AM`;
    if (h === 12) return '12 PM';
    return `${h - 12} PM`;
  }

  stars(n: number): number[] { return Array.from({ length: 5 }, (_, i) => i + 1); }
}
