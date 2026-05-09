import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ProfessionalAnalyticsResponse } from '../../../models/professional.model';
import { AuthService } from '../../../services/auth.service';
import { ProfessionalAnalyticsService } from '../../../services/professional-analytics.service';
import { ProfessionalNotificationService } from '../../../services/professional-notification.service';

const BASE = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-prof-dashboard',
  templateUrl: './prof-dashboard.component.html'
})
export class ProfDashboardComponent implements OnInit {
  analytics: ProfessionalAnalyticsResponse | null = null;
  unreadCount = 0;
  complaintsCount = 0;
  loading = false;
  profId = 0;

  constructor(
    private analyticsService: ProfessionalAnalyticsService,
    private notifService: ProfessionalNotificationService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.loading = true;

    this.analyticsService.getAnalytics(this.profId).subscribe({
      next: data => { this.analytics = data; this.loading = false; },
      error: () => this.loading = false
    });

    this.notifService.getUnreadCount(this.profId).subscribe({
      next: data => this.unreadCount = data.count,
      error: () => {}
    });

    // Load complaints count
    fetch(`${BASE}/admin/complaints?status=OPEN`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
    }).then(r => r.json()).then((data: any[]) => {
      if (Array.isArray(data)) {
        this.complaintsCount = data.filter(c => c.professionalId === this.profId).length;
      }
    }).catch(() => {});
  }

  quickActions = [
    { icon: '👤', label: 'My Profile', route: 'profile', hover: false },
    { icon: '📅', label: 'Appointments', route: 'appointments', hover: false },
    { icon: '✂️', label: 'Services', route: 'services', hover: false },
    { icon: '🖼️', label: 'Portfolio', route: 'portfolio', hover: false },
    { icon: '📆', label: 'Availability', route: 'availability', hover: false },
    { icon: '💬', label: 'Messages', route: 'communications', hover: false },
    { icon: '⭐', label: 'Reviews', route: 'reviews', hover: false },
    { icon: '📊', label: 'Analytics', route: 'analytics', hover: false },
    { icon: '🔔', label: 'Notifications', route: 'notifications', hover: false },
    { icon: '🩺', label: 'Consultations', route: 'consultations', hover: false },
    { icon: '👥', label: 'Group Bookings', route: 'group-bookings', hover: false }
  ];

  navigate(path: string): void {
    this.router.navigate(['/dashboard/professional/' + path]);
  }
}
