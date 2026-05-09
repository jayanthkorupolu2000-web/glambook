import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { Professional } from '../../../models';
import { OwnerIdService } from '../../../services/owner-id.service';

const API_BASE = 'http://localhost:8080';

@Component({
  selector: 'app-staff-list',
  templateUrl: './staff-list.component.html'
})
export class StaffListComponent implements OnInit {
  staff: (Professional & { status?: string })[] = [];
  loading = false;
  error: string | null = null;
  actionLoading: Record<number, boolean> = {};
  ownerId = 0;

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
    this.http.get<(Professional & { status?: string })[]>(`${API_BASE}/api/owners/${id}/staff`).subscribe({
      next: data => { this.staff = data; this.loading = false; },
      error: () => { this.error = 'Failed to load staff.'; this.loading = false; }
    });
  }

  approve(profId: number): void {
    this.actionLoading[profId] = true;
    this.http.patch<Professional & { status?: string }>(
      `${API_BASE}/api/owners/${this.ownerId}/staff/${profId}/approve`, {}
    ).subscribe({
      next: updated => {
        const idx = this.staff.findIndex(s => s.id === profId);
        if (idx !== -1) this.staff[idx] = { ...this.staff[idx], ...updated };
        this.actionLoading[profId] = false;
      },
      error: () => this.actionLoading[profId] = false
    });
  }

  reject(profId: number): void {
    this.actionLoading[profId] = true;
    this.http.patch<Professional & { status?: string }>(
      `${API_BASE}/api/owners/${this.ownerId}/staff/${profId}/reject`, {}
    ).subscribe({
      next: updated => {
        const idx = this.staff.findIndex(s => s.id === profId);
        if (idx !== -1) this.staff[idx] = { ...this.staff[idx], ...updated };
        this.actionLoading[profId] = false;
      },
      error: () => this.actionLoading[profId] = false
    });
  }

  statusBadgeClass(status: string | undefined): string {
    const map: Record<string, string> = {
      ACTIVE: 'bg-success', PENDING: 'bg-warning text-dark', SUSPENDED: 'bg-danger'
    };
    return `badge ${map[status ?? 'PENDING'] ?? 'bg-secondary'}`;
  }

  starsArray(rating: number | undefined): boolean[] {
    const r = Math.round(rating ?? 0);
    return [1, 2, 3, 4, 5].map(i => i <= r);
  }
}
