import { Component, OnInit } from '@angular/core';
import { OwnerReportResponse } from '../../../models/owner.model';
import { OwnerIdService } from '../../../services/owner-id.service';
import { OwnerReportService } from '../../../services/owner-report.service';

@Component({
  selector: 'app-owner-reports',
  templateUrl: './owner-reports.component.html'
})
export class OwnerReportsComponent implements OnInit {
  report: OwnerReportResponse | null = null;
  loading = false;

  constructor(private reportService: OwnerReportService, private ownerIdService: OwnerIdService) {}

  ngOnInit(): void {
    this.ownerIdService.getOwnerId().subscribe(id => {
      this.loading = true;
      this.reportService.getReport(id).subscribe({
        next: data => { this.report = data; this.loading = false; },
        error: () => this.loading = false
      });
    });
  }
}
