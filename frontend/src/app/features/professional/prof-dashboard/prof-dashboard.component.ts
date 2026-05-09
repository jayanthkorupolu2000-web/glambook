import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ProfessionalAnalyticsResponse } from '../../../models/professional.model';
import { AuthService } from '../../../services/auth.service';
import { ProfessionalAnalyticsService } from '../../../services/professional-analytics.service';
import { ProfessionalNotificationService } from '../../../services/professional-notification.service';

const BASE = 'http://localhost:8080/api/v1';

export interface QuickAction {
  label: string;
  route: string;
  hover: boolean;
  // SVG path(s) for the icon
  svgPath: string;
  svgPath2?: string;   // optional second path for two-tone icons
  color: string;       // icon stroke/fill colour
  bg: string;          // icon background colour
}

@Component({
  selector: 'app-prof-dashboard',
  templateUrl: './prof-dashboard.component.html',
  styleUrls: ['./prof-dashboard.component.css']
})
export class ProfDashboardComponent implements OnInit {
  analytics: ProfessionalAnalyticsResponse | null = null;
  unreadCount = 0;
  complaintsCount = 0;
  loading = false;
  profId = 0;
  profName = '';

  constructor(
    private analyticsService: ProfessionalAnalyticsService,
    private notifService: ProfessionalNotificationService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.profId   = this.auth.getUserId() || 0;
    this.profName = this.auth.getUserName() || 'Professional';
    this.loading  = true;

    this.analyticsService.getAnalytics(this.profId).subscribe({
      next: data => { this.analytics = data; this.loading = false; },
      error: () => this.loading = false
    });

    this.notifService.getUnreadCount(this.profId).subscribe({
      next: data => this.unreadCount = data.count,
      error: () => {}
    });

    fetch(`${BASE}/admin/complaints?status=OPEN`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
    }).then(r => r.json()).then((data: any[]) => {
      if (Array.isArray(data))
        this.complaintsCount = data.filter(c => c.professionalId === this.profId).length;
    }).catch(() => {});
  }

  // ── Quick actions with SVG icons ─────────────────────────────────
  quickActions: QuickAction[] = [
    {
      label: 'My Profile', route: 'profile', hover: false,
      color: '#2d4a6e', bg: '#eef2f8',
      svgPath: 'M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2',
      svgPath2: 'M12 11a4 4 0 100-8 4 4 0 000 8z'
    },
    {
      label: 'Appointments', route: 'appointments', hover: false,
      color: '#1a9e5c', bg: '#e6f9f0',
      svgPath: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z'
    },
    {
      label: 'Services', route: 'services', hover: false,
      color: '#ad1457', bg: '#fce4ec',
      svgPath: 'M14.121 14.121L19 19m-7-7l7-7m-7 7l-2.879 2.879M12 12L9.121 9.121m0 5.758a3 3 0 10-4.243 4.243 3 3 0 004.243-4.243zm0-5.758a3 3 0 10-4.243-4.243 3 3 0 004.243 4.243z'
    },
    {
      label: 'Portfolio', route: 'portfolio', hover: false,
      color: '#6a1b9a', bg: '#f3e5f5',
      svgPath: 'M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z'
    },
    {
      label: 'Availability', route: 'availability', hover: false,
      color: '#0277bd', bg: '#e1f5fe',
      svgPath: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z'
    },
    {
      label: 'Reviews', route: 'reviews', hover: false,
      color: '#c77c00', bg: '#fff8e1',
      svgPath: 'M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z'
    },
    {
      label: 'Analytics', route: 'analytics', hover: false,
      color: '#1565c0', bg: '#e3f2fd',
      svgPath: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z'
    },
    {
      label: 'Consultations', route: 'consultations', hover: false,
      color: '#2e7d32', bg: '#e8f5e9',
      svgPath: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01'
    }
  ];

  navigate(path: string): void {
    this.router.navigate(['/dashboard/professional/' + path]);
  }

  // ── 12-hr clock format ────────────────────────────────────────────
  formatHour(h: number): string {
    if (h === 0)  return '12 AM';
    if (h < 12)   return `${h} AM`;
    if (h === 12) return '12 PM';
    return `${h - 12} PM`;
  }

  stars(n: number): number[] { return Array.from({ length: 5 }, (_, i) => i + 1); }
}
