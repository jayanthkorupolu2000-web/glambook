import { Component, OnInit } from '@angular/core';
import { ProfessionalAnalyticsResponse } from '../../../models/professional.model';
import { AuthService } from '../../../services/auth.service';
import { ProfessionalAnalyticsService } from '../../../services/professional-analytics.service';

@Component({
  selector: 'app-prof-analytics',
  templateUrl: './prof-analytics.component.html'
})
export class ProfAnalyticsComponent implements OnInit {
  analytics: ProfessionalAnalyticsResponse | null = null;
  loading = false;
  profId = 0;

  monthLabels: string[] = [];
  monthData: number[] = [];

  constructor(private analyticsService: ProfessionalAnalyticsService, private auth: AuthService) {}

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.loading = true;
    this.analyticsService.getAnalytics(this.profId).subscribe({
      next: data => {
        this.analytics = data;
        this.monthLabels = Object.keys(data.monthlyEarnings);
        this.monthData = Object.values(data.monthlyEarnings);
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  monthEntries(): { month: string; amount: number }[] {
    if (!this.analytics) return [];
    return Object.entries(this.analytics.monthlyEarnings).map(([month, amount]) => ({ month, amount }));
  }

  maxEarning(): number {
    return Math.max(...this.monthData, 1);
  }
}
