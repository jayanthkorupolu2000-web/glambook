import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Appointment } from '../../../models';
import { AppointmentService } from '../../../services/appointment.service';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-appointment-history',
  templateUrl: './appointment-history.component.html'
})
export class AppointmentHistoryComponent implements OnInit {
  appointments: Appointment[] = [];
  loading = false;
  error = '';
  cancellingId: number | null = null;
  activeTab: 'upcoming' | 'past' = 'upcoming';

  constructor(
    private appointmentService: AppointmentService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const customerId = this.authService.getUserId();
    if (!customerId) return;

    this.loading = true;
    this.error = '';
    this.appointmentService.getByCustomer(customerId).subscribe({
      next: (data) => {
        this.appointments = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load appointments.';
        this.loading = false;
      }
    });
  }

  get upcoming(): Appointment[] {
    const now = new Date();
    return this.appointments.filter(a =>
      new Date(a.dateTime) >= now && a.status !== 'CANCELLED' && a.status !== 'COMPLETED'
    );
  }

  get past(): Appointment[] {
    const now = new Date();
    return this.appointments.filter(a =>
      new Date(a.dateTime) < now || a.status === 'COMPLETED' || a.status === 'CANCELLED'
    );
  }

  cancel(id: number): void {
    if (!confirm('Are you sure you want to cancel this appointment?')) return;
    this.cancellingId = id;
    this.appointmentService.cancel(id).subscribe({
      next: () => {
        this.cancellingId = null;
        this.load();
      },
      error: () => {
        this.error = 'Failed to cancel appointment.';
        this.cancellingId = null;
      }
    });
  }

  rebook(appointment: Appointment): void {
    this.router.navigate(['/customer/book', appointment.professional.id]);
  }

  statusBadgeClass(status: string): string {
    switch (status) {
      case 'CONFIRMED': return 'bg-success';
      case 'PENDING': return 'bg-warning text-dark';
      case 'CANCELLED': return 'bg-danger';
      case 'COMPLETED': return 'bg-secondary';
      default: return 'bg-light text-dark';
    }
  }
}
