import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { OwnerReportResponse } from '../../../models/owner.model';
import { AuthService } from '../../../services/auth.service';
import { OwnerIdService } from '../../../services/owner-id.service';
import { OwnerReportService } from '../../../services/owner-report.service';

export interface OwnerQuickAction {
  label: string;
  route: string;
  svgPath: string;
  svgPath2?: string;
  color: string;
  bg: string;
}

const API_BASE = 'http://localhost:8080';

@Component({
  selector: 'app-owner-home',
  templateUrl: './owner-home.component.html',
  styleUrls: ['./owner-home.component.scss']
})
export class OwnerHomeComponent implements OnInit {
  ownerName = '';
  report: OwnerReportResponse | null = null;
  recentAppointments: any[] = [];
  loading = true;
  ownerId = 0;
  unreadNotifications = 0;
  today = new Date();

  quickActions: OwnerQuickAction[] = [
    {
      label: 'Staff & Approvals', route: 'staff',
      color: '#2d4a6e', bg: '#eef2f8',
      svgPath: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2',
      svgPath2: 'M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75M9 7a4 4 0 100-8 4 4 0 000 8z'
    },
    {
      label: 'Services', route: 'services',
      color: '#ad1457', bg: '#fce4ec',
      svgPath: 'M14.121 14.121L19 19m-7-7l7-7m-7 7l-2.879 2.879M12 12L9.121 9.121m0 5.758a3 3 0 10-4.243 4.243 3 3 0 004.243-4.243zm0-5.758a3 3 0 10-4.243-4.243 3 3 0 004.243 4.243z'
    },
    {
      label: 'Appointments', route: 'appointments',
      color: '#1a9e5c', bg: '#e6f9f0',
      svgPath: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z'
    },
    {
      label: 'Promotions', route: 'promotions',
      color: '#c77c00', bg: '#fff8e1',
      svgPath: 'M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A2 2 0 013 12V7a4 4 0 014-4z'
    },
    {
      label: 'Loyalty', route: 'loyalty',
      color: '#e65c00', bg: '#fff3e0',
      svgPath: 'M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z'
    },
    {
      label: 'Complaints', route: 'complaints',
      color: '#c0392b', bg: '#fdecea',
      svgPath: 'M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z',
      svgPath2: 'M12 9v4M12 17h.01'
    },
    {
      label: 'Reports', route: 'reports',
      color: '#1565c0', bg: '#e3f2fd',
      svgPath: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z'
    },
    {
      label: 'Policies', route: 'policies',
      color: '#2e7d32', bg: '#e8f5e9',
      svgPath: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01'
    },
    {
      label: 'Notifications', route: 'notifications',
      color: '#6a1b9a', bg: '#f3e5f5',
      svgPath: 'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9'
    },
    {
      label: 'My Profile', route: 'profile',
      color: '#0277bd', bg: '#e1f5fe',
      svgPath: 'M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2',
      svgPath2: 'M12 11a4 4 0 100-8 4 4 0 000 8z'
    }
  ];

  constructor(
    private auth: AuthService,
    private ownerIdService: OwnerIdService,
    private reportService: OwnerReportService,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.ownerName = this.auth.getUserName() || 'Owner';
    this.ownerIdService.getOwnerId().subscribe(id => {
      this.ownerId = id;
      this.loadReport(id);
      this.loadRecentAppointments(id);
      this.loadUnreadNotifications(id);
    });
  }

  loadReport(id: number): void {
    this.reportService.getReport(id).subscribe({
      next: data => { this.report = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  loadRecentAppointments(id: number): void {
    this.http.get<any[]>(`${API_BASE}/api/owners/${id}/appointments`).subscribe({
      next: data => {
        this.recentAppointments = (Array.isArray(data) ? data : [])
          .sort((a, b) => new Date(b.appointmentDate || b.createdAt || 0).getTime()
                        - new Date(a.appointmentDate || a.createdAt || 0).getTime())
          .slice(0, 5);
      },
      error: () => {}
    });
  }

  loadUnreadNotifications(id: number): void {
    this.http.get<any[]>(`${API_BASE}/api/v1/owners/${id}/notifications`).subscribe({
      next: data => {
        this.unreadNotifications = Array.isArray(data)
          ? data.filter(n => !n.isRead).length : 0;
      },
      error: () => {}
    });
  }

  navigate(route: string): void {
    this.router.navigate(['/dashboard/owner/' + route]);
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'status-pending',
      CONFIRMED: 'status-confirmed',
      COMPLETED: 'status-completed',
      CANCELLED: 'status-cancelled'
    };
    return map[status] ?? 'status-pending';
  }

  get completionRate(): number {
    if (!this.report || !this.report.totalAppointments) return 0;
    return Math.round((this.report.completedAppointments / this.report.totalAppointments) * 100);
  }
}
