import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AppointmentResponse } from '../../../models/customer.model';
import { AuthService } from '../../../services/auth.service';

const BASE = 'http://localhost:8080';

@Component({
  selector: 'app-prof-appointments',
  templateUrl: './prof-appointments.component.html'
})
export class ProfAppointmentsComponent implements OnInit {
  appointments: AppointmentResponse[] = [];
  loading = false;
  error = '';

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void {
    const profId = this.auth.getUserId();
    if (!profId) return;
    this.loading = true;
    // Use the general appointments endpoint with professionalId filter
    this.http.get<AppointmentResponse[]>(`${BASE}/api/v1/professionals/${profId}/appointments`).subscribe({
      next: data => {
        this.appointments = Array.isArray(data) ? data : [];
        this.loading = false;
      },
      error: (err) => {
        // Fallback: try the general appointments endpoint
        this.http.get<AppointmentResponse[]>(`${BASE}/api/appointments?professionalId=${profId}`).subscribe({
          next: data => { this.appointments = Array.isArray(data) ? data : []; this.loading = false; },
          error: () => { this.error = 'Failed to load appointments.'; this.loading = false; }
        });
      }
    });
  }

  statusBadgeClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'bg-warning text-dark', CONFIRMED: 'bg-success',
      COMPLETED: 'bg-secondary', CANCELLED: 'bg-danger'
    };
    return `badge ${map[status] ?? 'bg-secondary'}`;
  }
}
