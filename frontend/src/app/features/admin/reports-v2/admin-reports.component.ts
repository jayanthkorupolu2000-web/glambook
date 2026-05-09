import { Component, OnInit } from '@angular/core';
import { CancellationStats, PaymentStats } from '../../../models/complaint.model';
import { AdminService } from '../../../services/admin.service';

@Component({
  selector: 'app-admin-reports',
  templateUrl: './admin-reports.component.html'
})
export class AdminReportsComponent implements OnInit {
  loading = false;

  totalComplaints = 0;
  avgRating = 0;
  cancellations: CancellationStats = { total: 0, sameDay: 0, suspendedCustomers: 0 };
  payments: PaymentStats = { total: 0, paid: 0, refunded: 0, successRatio: 0 };

  complaintsByCity: Record<string, number> = {};
  ratingsDistribution: Record<number, number> = {};

  // Chart data
  cityLabels: string[] = [];
  cityData: number[] = [];
  ratingLabels: string[] = ['1★', '2★', '3★', '4★', '5★'];
  ratingData: number[] = [0, 0, 0, 0, 0];

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loading = true;
    this.loadAll();
  }

  loadAll(): void {
    this.adminService.getComplaintsByCity().subscribe(data => {
      this.complaintsByCity = data;
      this.cityLabels = Object.keys(data);
      this.cityData = Object.values(data);
      this.totalComplaints = this.cityData.reduce((a, b) => a + b, 0);
    });

    this.adminService.getRatingsDistribution().subscribe(data => {
      this.ratingsDistribution = data;
      let total = 0, count = 0;
      for (let i = 1; i <= 5; i++) {
        const c = data[i] || 0;
        this.ratingData[i - 1] = c;
        total += i * c;
        count += c;
      }
      this.avgRating = count > 0 ? Math.round((total / count) * 10) / 10 : 0;
      this.loading = false;
    });

    this.adminService.getCancellationStats().subscribe(data => this.cancellations = data);
    this.adminService.getPaymentStats().subscribe(data => this.payments = data);
  }

  cityEntries(): { city: string; count: number }[] {
    return Object.entries(this.complaintsByCity).map(([city, count]) => ({ city, count }));
  }

  ratingEntries(): { rating: number; count: number }[] {
    return [1, 2, 3, 4, 5].map(r => ({ rating: r, count: this.ratingsDistribution[r] || 0 }));
  }
}
