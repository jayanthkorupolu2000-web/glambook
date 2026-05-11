import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';

const API = 'http://localhost:8080/api/v1/admin/analytics';

@Component({
  selector: 'app-reports',
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss']
})
export class ReportsComponent implements OnInit {

  summary: any = null;
  salons: any[] = [];
  filteredSalons: any[] = [];
  loading = false;
  error = '';

  // Search / filter
  searchQuery = '';
  sortBy: 'revenue' | 'appointments' | 'rating' = 'revenue';

  // City/Salon dropdowns for reports
  filterCity = '';
  filterSalonId = '';
  reportCities: string[] = [];
  reportFilteredSalons: any[] = [];

  // Expanded salon (for professional breakdown)
  expandedSalonId: number | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.error = '';

    // Load summary KPIs
    this.http.get<any>(`${API}/summary`).subscribe({
      next: d => { this.summary = d; },
      error: () => {}
    });

    // Load per-salon analytics
    this.http.get<any[]>(`${API}/salons`).subscribe({
      next: data => {
        this.salons = Array.isArray(data) ? data : [];
        // Build city list for dropdown
        this.reportCities = [...new Set(this.salons.map(s => s.city).filter(Boolean))].sort();
        this.reportFilteredSalons = [...this.salons];
        this.applyFilter();
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load analytics. Please try again.';
        this.loading = false;
      }
    });
  }

  onReportCityChange(): void {
    this.filterSalonId = '';
    this.reportFilteredSalons = this.filterCity
      ? this.salons.filter(s => s.city === this.filterCity)
      : [...this.salons];
    this.applyFilter();
  }

  applyFilter(): void {
    let list = [...this.salons];

    if (this.filterCity) list = list.filter(s => s.city === this.filterCity);
    if (this.filterSalonId) list = list.filter(s => String(s.ownerId) === String(this.filterSalonId));

    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      list = list.filter(s =>
        s.salonName?.toLowerCase().includes(q) ||
        s.ownerName?.toLowerCase().includes(q) ||
        s.city?.toLowerCase().includes(q)
      );
    }

    list.sort((a, b) => {
      if (this.sortBy === 'revenue')      return (b.totalRevenue || 0) - (a.totalRevenue || 0);
      if (this.sortBy === 'appointments') return (b.totalAppointments || 0) - (a.totalAppointments || 0);
      if (this.sortBy === 'rating')       return (b.averageRating || 0) - (a.averageRating || 0);
      return 0;
    });

    this.filteredSalons = list;
  }

  toggleSalon(id: number): void {
    this.expandedSalonId = this.expandedSalonId === id ? null : id;
  }

  stars(rating: number): number[] {
    return [1, 2, 3, 4, 5];
  }

  completionRate(s: any): number {
    if (!s.totalAppointments) return 0;
    return Math.round((s.completedAppointments / s.totalAppointments) * 100);
  }

  profCompletionRate(p: any): number {
    if (!p.totalAppointments) return 0;
    return Math.round((p.completedAppointments / p.totalAppointments) * 100);
  }

  statusColor(status: string): string {
    return status === 'ACTIVE' ? '#1a9e5c' : '#c0392b';
  }
}
