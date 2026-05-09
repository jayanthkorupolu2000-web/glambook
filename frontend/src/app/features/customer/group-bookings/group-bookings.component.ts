import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';

const API = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-group-bookings',
  templateUrl: './group-bookings.component.html'
})
export class GroupBookingsComponent implements OnInit {
  bookings: any[] = [];
  professionals: any[] = [];
  loading = false;
  showForm = false;
  submitting = false;
  form!: FormGroup;
  error = '';
  success = '';

  // Map: professionalId → professional object
  selectedProfs: Record<number, any> = {};

  // Auto-calculated discount
  calculatedDiscount = 0;

  constructor(private http: HttpClient, private auth: AuthService, private fb: FormBuilder) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      scheduledAt: ['', Validators.required],
      notes: ['']
    });
    this.load();
    this.loadProfessionals();
  }

  load(): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.loading = true;
    this.http.get<any[]>(`${API}/customers/${id}/group-bookings`).subscribe({
      next: data => { this.bookings = Array.isArray(data) ? data : []; this.loading = false; },
      error: () => { this.bookings = []; this.loading = false; }
    });
  }

  loadProfessionals(): void {
    this.http.get<any[]>('http://localhost:8080/api/professionals/all').subscribe({
      next: data => {
        this.professionals = Array.isArray(data) ? data : [];
        // For each professional, if services array is empty, fetch from dedicated endpoint
        this.professionals.forEach(p => {
          if (!p.services || p.services.length === 0 || !p.services[0].price) {
            this.http.get<any[]>(`http://localhost:8080/api/v1/professionals/${p.id}/services`)
              .subscribe({
                next: svcs => {
                  if (svcs && svcs.length > 0) {
                    p.services = svcs;
                  }
                },
                error: () => {}
              });
          }
        });
      },
      error: () => {}
    });
  }

  toggleProfessional(prof: any): void {
    if (this.selectedProfs[prof.id]) {
      delete this.selectedProfs[prof.id];
    } else {
      this.selectedProfs[prof.id] = prof;
    }
    this.recalculateDiscount();
  }

  isProfSelected(id: number): boolean {
    return !!this.selectedProfs[id];
  }

  get selectedProfList(): any[] {
    return Object.values(this.selectedProfs);
  }

  get selectedCount(): number {
    return Object.keys(this.selectedProfs).length;
  }

  /** Get the primary service price for a professional */
  getProfServicePrice(prof: any): number {
    if (prof.services && prof.services.length > 0) {
      return Number(prof.services[0].price) || 0;
    }
    return 0;
  }

  /** Get the primary service name for a professional */
  getProfServiceName(prof: any): string {
    if (prof.services && prof.services.length > 0) {
      return prof.services[0].name;
    }
    return prof.specialization || 'Service';
  }

  /** Get the primary service ID for a professional */
  getProfServiceId(prof: any): number | null {
    if (prof.services && prof.services.length > 0) {
      return prof.services[0].id;
    }
    return null;
  }

  /** Total cost before discount */
  get totalBeforeDiscount(): number {
    return this.selectedProfList.reduce((sum, p) => sum + this.getProfServicePrice(p), 0);
  }

  /** Discount amount in rupees */
  get discountAmount(): number {
    return Math.round(this.totalBeforeDiscount * this.calculatedDiscount / 100);
  }

  /** Final cost after discount */
  get finalCost(): number {
    return this.totalBeforeDiscount - this.discountAmount;
  }

  /**
   * Discount based on average rating of selected professionals:
   * avg >= 4.5 → 15%  |  avg >= 4.0 → 10%  |  avg >= 3.5 → 7%
   * avg >= 3.0 → 5%   |  below 3.0 → 0%
   * +2% for each professional beyond 2 (group size bonus), capped at 30%
   */
  recalculateDiscount(): void {
    const profs = this.selectedProfList;
    if (profs.length < 2) { this.calculatedDiscount = 0; return; }

    const ratings = profs.map(p => p.rating || 0).filter(r => r > 0);
    const avg = ratings.length ? ratings.reduce((a, b) => a + b, 0) / ratings.length : 0;

    let base = 0;
    if (avg >= 4.5) base = 15;
    else if (avg >= 4.0) base = 10;
    else if (avg >= 3.5) base = 7;
    else if (avg >= 3.0) base = 5;

    const sizeBonus = Math.max(0, profs.length - 2) * 2;
    this.calculatedDiscount = Math.min(base + sizeBonus, 30);
  }

  get discountLabel(): string {
    const profs = this.selectedProfList;
    if (profs.length < 2) return '';
    const ratings = profs.map(p => p.rating || 0).filter(r => r > 0);
    if (!ratings.length) return 'No ratings yet — no discount applied';
    const avg = ratings.reduce((a, b) => a + b, 0) / ratings.length;
    return `Avg rating ${avg.toFixed(1)}★ → ${this.calculatedDiscount}% discount`;
  }

  submit(): void {
    if (this.form.invalid) return;
    if (this.selectedCount < 2) {
      this.error = 'Please select at least 2 professionals.';
      return;
    }
    const id = this.auth.getUserId();
    if (!id) return;
    this.submitting = true;
    this.error = '';

    const profs = this.selectedProfList;
    const payload = {
      customerId: id,
      scheduledAt: this.form.value.scheduledAt,
      discountPct: this.calculatedDiscount,
      notes: this.form.value.notes,
      professionalIds: profs.map(p => p.id),
      serviceId: this.getProfServiceId(profs[0]),
      participantServices: profs.map(p => ({
        professionalId: p.id,
        serviceId: this.getProfServiceId(p)
      }))
    };

    this.http.post<any>(`${API}/customers/${id}/group-bookings`, payload).subscribe({
      next: () => {
        this.success = 'Group booking created! Each professional has been assigned their appointment.';
        this.showForm = false;
        this.form.reset();
        this.selectedProfs = {};
        this.calculatedDiscount = 0;
        this.submitting = false;
        this.load();
      },
      error: (e) => {
        this.error = e?.error?.message || 'Failed to create booking.';
        this.submitting = false;
      }
    });
  }

  statusBadge(s: string): string {
    const m: Record<string, string> = {
      PENDING: 'warning text-dark', CONFIRMED: 'success',
      COMPLETED: 'primary', CANCELLED: 'danger'
    };
    return `badge bg-${m[s] ?? 'secondary'}`;
  }

  cancel(bookingId: number): void {
    const id = this.auth.getUserId();
    if (!id) return;
    if (!confirm('Cancel this group booking?')) return;
    this.http.patch(`${API}/customers/${id}/group-bookings/${bookingId}/cancel`, {}).subscribe({
      next: () => this.load(),
      error: (e) => this.error = e?.error?.message || 'Failed to cancel.'
    });
  }

  stars(n: number): number[] {
    return Array.from({ length: 5 }, (_, i) => i + 1);
  }
}
