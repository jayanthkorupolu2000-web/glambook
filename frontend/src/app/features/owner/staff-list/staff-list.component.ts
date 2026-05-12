import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { Professional } from '../../../models';
import { OwnerIdService } from '../../../services/owner-id.service';

const API_BASE = 'http://localhost:8080';

type StaffMember = Professional & {
  status?: string;
  suspensionReason?: string;
  suspendedUntil?: string | null;
};

@Component({
  selector: 'app-staff-list',
  templateUrl: './staff-list.component.html',
  styleUrls: ['./staff-list.component.scss']
})
export class StaffListComponent implements OnInit {
  staff: StaffMember[] = [];
  loading = false;
  error: string | null = null;
  successMsg = '';
  suspendMsg = '';
  actionLoading: Record<number, boolean> = {};
  ownerId = 0;

  // ── Suspension modal state ──────────────────────────────────────
  suspendTarget: StaffMember | null = null;
  suspendReason = '';
  suspendDurationType: 'permanent' | 'days' = 'permanent';
  suspendDays: number | null = null;
  suspendLoading = false;
  suspendSubmitted = false;

  constructor(private http: HttpClient, private ownerIdService: OwnerIdService) {}

  ngOnInit(): void {
    this.ownerIdService.getOwnerId().subscribe(id => {
      if (!id) return;
      this.ownerId = id;
      this.loadStaff(id);
    });
  }

  loadStaff(id: number): void {
    this.loading = true;
    this.http.get<StaffMember[]>(`${API_BASE}/api/owners/${id}/staff`).subscribe({
      next: data => { this.staff = data; this.loading = false; },
      error: () => { this.error = 'Failed to load staff.'; this.loading = false; }
    });
  }

  approve(profId: number): void {
    this.actionLoading[profId] = true;
    this.http.patch<StaffMember>(
      `${API_BASE}/api/owners/${this.ownerId}/staff/${profId}/approve`, {}
    ).subscribe({
      next: updated => {
        const idx = this.staff.findIndex(s => s.id === profId);
        if (idx !== -1) this.staff[idx] = { ...this.staff[idx], ...updated };
        this.actionLoading[profId] = false;
        this.showSuccess('Professional reactivated successfully.');
      },
      error: () => this.actionLoading[profId] = false
    });
  }

  // ── Open suspension modal ───────────────────────────────────────
  openSuspendModal(member: StaffMember): void {
    this.suspendTarget = member;
    this.suspendReason = '';
    this.suspendDurationType = 'permanent';
    this.suspendDays = null;
    this.suspendSubmitted = false;
    this.suspendLoading = false;
  }

  closeSuspendModal(): void {
    this.suspendTarget = null;
  }

  confirmSuspend(): void {
    this.suspendSubmitted = true;
    if (!this.suspendReason.trim()) return;
    if (this.suspendDurationType === 'days' && (!this.suspendDays || this.suspendDays < 1)) return;

    if (!this.suspendTarget) return;
    this.suspendLoading = true;

    const body: any = { reason: this.suspendReason.trim() };
    if (this.suspendDurationType === 'days' && this.suspendDays) {
      body.durationDays = this.suspendDays;
    }

    this.http.patch<StaffMember>(
      `${API_BASE}/api/owners/${this.ownerId}/staff/${this.suspendTarget.id}/suspend`,
      body
    ).subscribe({
      next: updated => {
        const idx = this.staff.findIndex(s => s.id === this.suspendTarget!.id);
        if (idx !== -1) this.staff[idx] = { ...this.staff[idx], ...updated };
        this.suspendLoading = false;
        this.closeSuspendModal();
        this.suspendMsg = 'Professional suspended. Active appointments cancelled and notifications sent.';
        setTimeout(() => this.suspendMsg = '', 4000);
      },
      error: (err) => {
        this.suspendLoading = false;
        this.error = err?.error?.message || 'Failed to suspend professional.';
      }
    });
  }

  // ── Computed: suspension end date preview ───────────────────────
  get suspendEndDate(): Date | null {
    if (!this.suspendDays || this.suspendDays < 1) return null;
    const d = new Date();
    d.setDate(d.getDate() + this.suspendDays);
    return d;
  }

  clampDays(): void {
    if (this.suspendDays === null || this.suspendDays === undefined) return;
    if (this.suspendDays < 1) this.suspendDays = 1;
    if (this.suspendDays > 365) this.suspendDays = 365;
  }

  // ── Helpers ─────────────────────────────────────────────────────
  private showSuccess(msg: string): void {
    this.successMsg = msg;
    setTimeout(() => this.successMsg = '', 4000);
  }

  statusBadgeClass(status: string | undefined): string {
    const map: Record<string, string> = {
      ACTIVE: 'badge bg-success',
      PENDING: 'badge bg-warning text-dark',
      SUSPENDED: 'badge bg-danger'
    };
    return map[status ?? 'PENDING'] ?? 'badge bg-secondary';
  }

  starsArray(rating: number | undefined): boolean[] {
    const r = Math.round(rating ?? 0);
    return [1, 2, 3, 4, 5].map(i => i <= r);
  }

  countByStatus(status: string): number {
    return this.staff.filter(s => (s.status || 'PENDING') === status).length;
  }

  /** Pink avatar for female-oriented services, blue for male/neutral */
  isFemaleService(specialization: string | undefined): boolean {
    const femaleKeywords = ['makeup', 'bridal', 'facial', 'manicure', 'pedicure', 'waxing', 'threading', 'eyebrow', 'lash', 'nail'];
    return femaleKeywords.some(k => (specialization || '').toLowerCase().includes(k));
  }
}
