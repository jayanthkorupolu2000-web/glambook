import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ComplaintResponse } from '../../../models/complaint.model';
import { ComplaintService } from '../../../services/complaint.service';

@Component({
  selector: 'app-admin-complaints',
  templateUrl: './admin-complaints.component.html',
  styleUrls: ['./admin-complaints.component.scss']
})
export class AdminComplaintsComponent implements OnInit {
  complaints: ComplaintResponse[] = [];
  filtered: ComplaintResponse[] = [];
  loading = false;
  error = '';

  statusFilter = new FormControl('');
  searchText = new FormControl('');

  // Mediation modal state
  selectedComplaint: ComplaintResponse | null = null;
  resolutionNotes = '';
  mediating = false;

  constructor(private complaintService: ComplaintService) {}

  ngOnInit(): void {
    this.load();
    this.statusFilter.valueChanges.subscribe(() => this.applyFilter());
    this.searchText.valueChanges.subscribe(() => this.applyFilter());
  }

  load(): void {
    this.loading = true;
    this.complaintService.getAllComplaints().subscribe({
      next: data => {
        this.complaints = data;
        this.applyFilter();
        this.loading = false;
      },
      error: () => { this.error = 'Failed to load complaints.'; this.loading = false; }
    });
  }

  applyFilter(): void {
    const status = this.statusFilter.value || '';
    const search = (this.searchText.value || '').toLowerCase();
    this.filtered = this.complaints.filter(c => {
      const matchStatus = !status || c.status === status;
      const matchSearch = !search || c.professionalName.toLowerCase().includes(search);
      return matchStatus && matchSearch;
    });
  }

  forward(id: number): void {
    this.complaintService.forwardComplaint(id).subscribe({
      next: updated => {
        const idx = this.complaints.findIndex(c => c.id === id);
        if (idx !== -1) this.complaints[idx] = updated;
        this.applyFilter();
      },
      error: () => alert('Failed to forward complaint.')
    });
  }

  openMediate(complaint: ComplaintResponse): void {
    this.selectedComplaint = complaint;
    this.resolutionNotes = '';
  }

  submitMediation(): void {
    if (!this.selectedComplaint || !this.resolutionNotes.trim()) return;
    this.mediating = true;
    this.complaintService.mediateComplaint(this.selectedComplaint.id, this.resolutionNotes).subscribe({
      next: updated => {
        const idx = this.complaints.findIndex(c => c.id === updated.id);
        if (idx !== -1) this.complaints[idx] = updated;
        this.applyFilter();
        this.selectedComplaint = null;
        this.mediating = false;
      },
      error: () => { alert('Failed to mediate.'); this.mediating = false; }
    });
  }

  statusBadge(status: string): string {
    const map: Record<string, string> = { OPEN: 'warning', FORWARDED: 'info', RESOLVED: 'success' };
    return `badge bg-${map[status] ?? 'secondary'}`;
  }

  stars(rating: number): number[] {
    return Array.from({ length: 5 }, (_, i) => i + 1);
  }

  countByStatus(status: string): number {
    return this.complaints.filter(c => c.status === status).length;
  }
}
