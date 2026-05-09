import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../services/admin.service';

interface ReportsData {
  totalAppointments: number;
  totalPayments: number;
}

@Component({
  selector: 'app-reports',
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss']
})
export class ReportsComponent implements OnInit {
  reportsData: ReportsData | null = null;
  loading = false;
  error: string | null = null;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.loading = true;
    this.error = null;

    this.adminService.getReports().subscribe({
      next: (data) => {
        this.reportsData = data;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading reports:', error);
        this.error = 'Failed to load reports. Please try again.';
        this.loading = false;
      }
    });
  }

  refreshReports(): void {
    this.loadReports();
  }
}