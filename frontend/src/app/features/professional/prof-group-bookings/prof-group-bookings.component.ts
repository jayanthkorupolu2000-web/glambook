import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

const API = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-prof-group-bookings',
  templateUrl: './prof-group-bookings.component.html'
})
export class ProfGroupBookingsComponent implements OnInit {
  bookings: any[] = [];
  loading = false;

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.loading = true;
    this.http.get<any[]>(`${API}/professionals/${id}/group-bookings`).subscribe({
      next: data => { this.bookings = Array.isArray(data) ? data : []; this.loading = false; },
      error: () => { this.bookings = []; this.loading = false; }
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
