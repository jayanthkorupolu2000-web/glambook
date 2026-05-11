import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { CancellationStats, PaymentStats } from '../../../models/complaint.model';
import { AdminService } from '../../../services/admin.service';

const BASE = 'http://localhost:8080';

@Component({
  selector: 'app-admin-reports',
  templateUrl: './admin-reports.component.html',
  styleUrls: ['./admin-reports.component.scss']
})
export class AdminReportsComponent implements OnInit {
  loading = false;

  totalComplaints = 0;
  avgRating = 0;
  cancellations: CancellationStats = { total: 0, sameDay: 0, suspendedCustomers: 0 };
  payments: PaymentStats = { total: 0, paid: 0, refunded: 0, successRatio: 0 };

  complaintsByCity: Record<string, number> = {};
  ratingsDistribution: Record<number, number> = {};

  allCityEntries: { city: string; count: number }[] = [];
  allRatingEntries: { rating: number; count: number }[] = [];

  // Dropdown options
  cities: string[] = [];
  salons: { id: number; name: string; city: string }[] = [];
  filteredSalons: { id: number; name: string; city: string }[] = [];

  selectedCity = '';
  selectedSalonId = '';

  // Donut chart
  readonly donutColors = ['#334155','#244AFD','#1a9e5c','#c77c00','#ad1457','#0277bd'];
  donutSegments: { dash: string; offset: string }[] = [];
  readonly circumference = 2 * Math.PI * 50; // r=50 → 314.16

  constructor(private adminService: AdminService, private http: HttpClient) {}

  ngOnInit(): void {
    this.loading = true;
    this.loadAll();
    this.loadSalons();
  }

  loadSalons(): void {
    this.http.get<any[]>(`${BASE}/api/admin/owners`).subscribe({
      next: data => {
        this.salons = data.map(o => ({ id: o.id, name: o.salonName, city: o.city }));
        this.filteredSalons = [...this.salons];
        this.cities = [...new Set(data.map(o => o.city))].sort();
      },
      error: () => {}
    });
  }

  onCityChange(): void {
    this.selectedSalonId = '';
    this.filteredSalons = this.selectedCity
      ? this.salons.filter(s => s.city === this.selectedCity)
      : [...this.salons];
  }

  loadAll(): void {
    this.adminService.getComplaintsByCity().subscribe(data => {
      this.complaintsByCity = data;
      this.allCityEntries = Object.entries(data).map(([city, count]) => ({ city, count }));
      this.totalComplaints = this.allCityEntries.reduce((a, b) => a + b.count, 0);
      this.buildDonutSegments();
    });

    this.adminService.getRatingsDistribution().subscribe(data => {
      this.ratingsDistribution = data;
      let total = 0, count = 0;
      this.allRatingEntries = [1, 2, 3, 4, 5].map(r => {
        const c = data[r] || 0;
        total += r * c; count += c;
        return { rating: r, count: c };
      });
      this.avgRating = count > 0 ? Math.round((total / count) * 10) / 10 : 0;
      this.loading = false;
    });

    this.adminService.getCancellationStats().subscribe(data => this.cancellations = data);
    this.adminService.getPaymentStats().subscribe(data => this.payments = data);
  }

  get filteredCityEntries(): { city: string; count: number }[] {
    if (!this.selectedCity) return this.allCityEntries;
    return this.allCityEntries.filter(e => e.city === this.selectedCity);
  }

  get filteredTotal(): number {
    const t = this.filteredCityEntries.reduce((a, b) => a + b.count, 0);
    this.buildDonutSegments();
    return t;
  }

  buildDonutSegments(): void {
    const entries = this.filteredCityEntries;
    const total = entries.reduce((a, b) => a + b.count, 0);
    let offset = 0;
    this.donutSegments = entries.map(e => {
      const dash = total > 0 ? (e.count / total) * this.circumference : 0;
      const seg = { dash: `${dash} ${this.circumference}`, offset: `${-offset}` };
      offset += dash;
      return seg;
    });
  }

  ratingColor(rating: number): string {
    const colors: Record<number, string> = {
      1: '#dc2626', 2: '#f97316', 3: '#f59e0b', 4: '#84cc16', 5: '#16a34a'
    };
    return colors[rating] ?? '#94a3b8';
  }

  ratingEntries(): { rating: number; count: number }[] {
    return this.allRatingEntries;
  }
}
