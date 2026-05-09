import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

const API = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-owner-group-bookings',
  templateUrl: './owner-group-bookings.component.html'
})
export class OwnerGroupBookingsComponent implements OnInit {
  bookings: any[] = [];
  loading = false;
  error = '';
  success = '';
  acting: Record<number, boolean> = {};

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.loading = true;
    this.http.get<any[]>(`${API}/owners/${id}/group-bookings`).subscribe({
      next: data => { this.bookings = Array.isArray(data) ? data : []; this.loading = false; },
      error: () => { this.bookings = []; this.loading = false; }
    });
  }

  confirm(bookingId: number): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.acting[bookingId] = true;
    this.http.patch(`${API}/owners/${id}/group-bookings/${bookingId}/confirm`, {}).subscribe({
      next: () => { this.success = 'Booking confirmed!'; this.acting[bookingId] = false; this.load(); },
      error: (e) => { this.error = e?.error?.message || 'Failed.'; this.acting[bookingId] = false; }
    });
  }

  complete(bookingId: number): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.acting[bookingId] = true;
    this.http.patch(`${API}/owners/${id}/group-bookings/${bookingId}/complete`, {}).subscribe({
      next: () => { this.success = 'Booking completed!'; this.acting[bookingId] = false; this.load(); },
      error: (e) => { this.error = e?.error?.message || 'Failed.'; this.acting[bookingId] = false; }
    });
  }

  statusBadge(s: string): string {
    const m: Record<string, string> = {
      PENDING: 'warning text-dark', CONFIRMED: 'success',
      COMPLETED: 'primary', CANCELLED: 'danger'
    };
    return `badge bg-${m[s] ?? 'secondary'}`;
  }
}
