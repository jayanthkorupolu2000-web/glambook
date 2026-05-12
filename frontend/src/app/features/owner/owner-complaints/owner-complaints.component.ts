import { HttpClient } from '@angular/common/http';
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { ComplaintResponse } from '../../../models/complaint.model';
import { OwnerIdService } from '../../../services/owner-id.service';

const API = 'http://localhost:8080/api/v1/owners';

@Component({
  selector: 'app-owner-complaints',
  templateUrl: './owner-complaints.component.html',
  styleUrls: ['./owner-complaints.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class OwnerComplaintsComponent implements OnInit {
  complaints: ComplaintResponse[] = [];
  loading = false;
  statusFilter = 'FORWARDED';
  ownerId = 0;
  success = '';
  error = '';

  // Action modal
  selectedComplaint: ComplaintResponse | null = null;
  actionNotes = '';
  submitting = false;

  // Suspend modal
  suspendTarget: ComplaintResponse | null = null;
  suspendReason = '';
  suspendDurationType: 'permanent' | 'days' = 'permanent';
  suspendDays: number | null = null;
  suspendSubmitted = false;
  suspendLoading = false;

  constructor(private http: HttpClient, private ownerIdService: OwnerIdService) {}

  ngOnInit(): void {
    this.ownerIdService.getOwnerId().subscribe(id => {
      this.ownerId = id;
      this.load();
    });
  }

  load(): void {
    this.loading = true;
    this.http.get<ComplaintResponse[]>(
      `${API}/${this.ownerId}/complaints?status=${this.statusFilter}`
    ).subscribe({
      next: data => { this.complaints = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  // ── Add Remarks ─────────────────────────────────────────────────
  openAction(c: ComplaintResponse): void {
    this.selectedComplaint = c;
    this.actionNotes = c.ownerActionNotes || '';
  }

  closeAction(): void { this.selectedComplaint = null; }

  submitAction(): void {
    if (!this.selectedComplaint || this.actionNotes.trim().length < 10) {
      this.error = 'Remarks must be at least 10 characters.';
      return;
    }
    this.submitting = true;
    this.error = '';
    this.http.patch<ComplaintResponse>(
      `${API}/${this.ownerId}/complaints/${this.selectedComplaint.id}/action`,
      { ownerActionNotes: this.actionNotes.trim() }
    ).subscribe({
      next: updated => {
        const idx = this.complaints.findIndex(c => c.id === updated.id);
        if (idx !== -1) this.complaints[idx] = { ...this.complaints[idx], ...updated };
        this.selectedComplaint = null;
        this.submitting = false;
        this.showSuccess('Remarks saved successfully.');
      },
      error: err => {
        this.error = err?.error?.message || 'Failed to save remarks.';
        this.submitting = false;
      }
    });
  }

  // ── Suspend professional ────────────────────────────────────────
  openSuspend(c: ComplaintResponse): void {
    this.suspendTarget = c;
    this.suspendReason = '';
    this.suspendDurationType = 'permanent';
    this.suspendDays = null;
    this.suspendSubmitted = false;
    this.suspendLoading = false;
  }

  closeSuspend(): void { this.suspendTarget = null; }

  clampDays(): void {
    if (this.suspendDays !== null && this.suspendDays < 1) this.suspendDays = 1;
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

    this.http.patch(
      `${API}/${this.ownerId}/complaints/${this.suspendTarget.id}/suspend-professional`,
      body
    ).subscribe({
      next: () => {
        this.suspendLoading = false;
        this.closeSuspend();
        this.load();
        this.showSuccess('Professional suspended. Appointments cancelled and notifications sent.');
      },
      error: err => {
        this.error = err?.error?.message || 'Failed to suspend professional.';
        this.suspendLoading = false;
      }
    });
  }

  get suspendEndDate(): Date | null {
    if (!this.suspendDays || this.suspendDays < 1) return null;
    const d = new Date();
    d.setDate(d.getDate() + this.suspendDays);
    return d;
  }

  private showSuccess(msg: string): void {
    this.success = msg;
    setTimeout(() => this.success = '', 4000);
  }

  stars(n: number): number[] { return [1, 2, 3, 4, 5]; }

  statusColor(s: string): string {
    const m: Record<string, string> = { OPEN: '#c77c00', FORWARDED: '#1565c0', RESOLVED: '#16a34a' };
    return m[s] ?? '#64748b';
  }

  statusBg(s: string): string {
    const m: Record<string, string> = { OPEN: '#fff8e1', FORWARDED: '#e3f2fd', RESOLVED: '#dcfce7' };
    return m[s] ?? '#f1f5f9';
  }

  countByStatus(status: string): number {
    return this.complaints.filter(c => c.status === status).length;
  }
}
