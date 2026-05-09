import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { Professional } from '../../../models';
import { AppointmentResponse } from '../../../models/customer.model';
import { OwnerIdService } from '../../../services/owner-id.service';

const API_BASE = 'http://localhost:8080';

@Component({
  selector: 'app-appointment-overview',
  templateUrl: './appointment-overview.component.html'
})
export class AppointmentOverviewComponent implements OnInit {
  appointments: AppointmentResponse[] = [];
  staff: Professional[] = [];
  loading = false;
  error: string | null = null;
  ownerId = 0;

  // Assignment modal state
  assigningAppt: AppointmentResponse | null = null;
  selectedProfId: number | null = null;
  assignLoading = false;
  assignError = '';
  assignSuccess = '';

  constructor(private http: HttpClient, private ownerIdService: OwnerIdService) {}

  ngOnInit(): void {
    this.ownerIdService.getOwnerId().subscribe(id => {
      if (!id) return;
      this.ownerId = id;
      this.loadAppointments(id);
      this.loadStaff(id);
    });
  }

  loadAppointments(id: number): void {
    this.loading = true;
    this.http.get<AppointmentResponse[]>(`${API_BASE}/api/owners/${id}/appointments`).subscribe({
      next: data => { this.appointments = data; this.loading = false; },
      error: () => { this.error = 'Failed to load appointments.'; this.loading = false; }
    });
  }

  loadStaff(id: number): void {
    this.http.get<Professional[]>(`${API_BASE}/api/owners/${id}/staff`).subscribe({
      next: data => this.staff = data,
      error: () => {}
    });
  }

  openAssign(appt: AppointmentResponse): void {
    this.assigningAppt = appt;
    this.selectedProfId = appt.professionalId || null;
    this.assignError = '';
    this.assignSuccess = '';
  }

  closeAssign(): void {
    this.assigningAppt = null;
    this.selectedProfId = null;
  }

  confirmAssign(): void {
    if (!this.assigningAppt || !this.selectedProfId) return;
    this.assignLoading = true;
    this.assignError = '';

    this.http.patch<AppointmentResponse>(
      `${API_BASE}/api/owners/appointments/${this.assigningAppt.id}/assign`,
      { professionalId: this.selectedProfId }
    ).subscribe({
      next: updated => {
        const idx = this.appointments.findIndex(a => a.id === updated.id);
        if (idx !== -1) this.appointments[idx] = updated;
        this.assignLoading = false;
        this.assignSuccess = 'Professional assigned and appointment confirmed!';
        setTimeout(() => this.closeAssign(), 1500);
      },
      error: err => {
        this.assignError = err?.error?.message || 'Assignment failed. Please try again.';
        this.assignLoading = false;
      }
    });
  }

  cancelAssignment(appt: AppointmentResponse): void {
    if (!confirm(`Cancel assignment for ${appt.customerName}'s appointment? It will revert to PENDING.`)) return;

    this.http.patch<AppointmentResponse>(
      `${API_BASE}/api/owners/appointments/${appt.id}/unassign`, {}
    ).subscribe({
      next: updated => {
        const idx = this.appointments.findIndex(a => a.id === updated.id);
        if (idx !== -1) this.appointments[idx] = updated;
      },
      error: err => alert(err?.error?.message || 'Failed to cancel assignment.')
    });
  }

  statusBadgeClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'bg-warning text-dark',
      CONFIRMED: 'bg-success',
      COMPLETED: 'bg-secondary',
      CANCELLED: 'bg-danger'
    };
    return `badge ${map[status] ?? 'bg-secondary'}`;
  }

  get pendingCount(): number {
    return this.appointments.filter(a => a.status === 'PENDING').length;
  }
}
