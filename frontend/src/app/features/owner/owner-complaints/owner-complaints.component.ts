import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ComplaintResponse } from '../../../models/complaint.model';
import { OwnerIdService } from '../../../services/owner-id.service';

const API = 'http://localhost:8080/api/v1/owners';

@Component({
  selector: 'app-owner-complaints',
  templateUrl: './owner-complaints.component.html'
})
export class OwnerComplaintsComponent implements OnInit {
  complaints: ComplaintResponse[] = [];
  loading = false;
  statusFilter = 'FORWARDED';
  selectedComplaint: ComplaintResponse | null = null;
  actionNotes = '';
  submitting = false;
  ownerId = 0;

  constructor(private http: HttpClient, private ownerIdService: OwnerIdService) {}

  ngOnInit(): void {
    this.ownerIdService.getOwnerId().subscribe(id => {
      this.ownerId = id;
      this.load();
    });
  }

  load(): void {
    this.loading = true;
    this.http.get<ComplaintResponse[]>(`${API}/${this.ownerId}/complaints?status=${this.statusFilter}`).subscribe({
      next: data => { this.complaints = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  openAction(c: ComplaintResponse): void { this.selectedComplaint = c; this.actionNotes = ''; }

  submitAction(): void {
    if (!this.selectedComplaint || !this.actionNotes.trim()) return;
    this.submitting = true;
    this.http.patch<ComplaintResponse>(
      `${API}/${this.ownerId}/complaints/${this.selectedComplaint.id}/action`,
      { ownerActionNotes: this.actionNotes }
    ).subscribe({
      next: () => { this.load(); this.selectedComplaint = null; this.submitting = false; },
      error: () => { alert('Failed to log action.'); this.submitting = false; }
    });
  }

  stars(rating: number): number[] { return Array.from({ length: 5 }, (_, i) => i + 1); }
  statusBadge(s: string): string {
    const m: Record<string, string> = { OPEN: 'warning', FORWARDED: 'info', RESOLVED: 'success' };
    return `badge bg-${m[s] ?? 'secondary'}`;
  }
}
