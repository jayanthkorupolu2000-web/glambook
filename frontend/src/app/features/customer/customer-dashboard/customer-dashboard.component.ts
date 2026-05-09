import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { CustomerDashboardDTO } from '../../../models/customer.model';
import { AuthService } from '../../../services/auth.service';
import { CustomerDashboardService } from '../../../services/customer-dashboard.service';
import { CustomerNotificationService } from '../../../services/customer-notification.service';

@Component({
  selector: 'app-customer-dashboard',
  templateUrl: './customer-dashboard.component.html',
  styleUrls: ['./customer-dashboard.component.scss']
})
export class CustomerDashboardHomeComponent implements OnInit {
  dashboard: CustomerDashboardDTO | null = null;
  loading = false;
  customerId = 0;
  activeSection = 'home';
  policyDismissed = false;

  sidebarLinks = [
    { id: 'home', icon: '🏠', label: 'Dashboard' },
    { id: 'search', icon: '🔍', label: 'Book a Service' },
    { id: 'appointments', icon: '📅', label: 'My Appointments' },
    { id: 'beauty-profile', icon: '👤', label: 'Beauty Profile' },
    { id: 'consultations', icon: '💬', label: 'Consultations' },
    { id: 'products', icon: '🛍️', label: 'Products' },
    { id: 'favorites', icon: '❤️', label: 'My Favorites' },
    { id: 'orders', icon: '📦', label: 'My Orders' },
    { id: 'loyalty', icon: '⭐', label: 'Loyalty & Rewards' },
    { id: 'group-bookings', icon: '👥', label: 'Group Bookings' },
    { id: 'notifications', icon: '🔔', label: 'Notifications' }
  ];

  constructor(
    private dashboardService: CustomerDashboardService,
    private notifService: CustomerNotificationService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.customerId = this.auth.getUserId() || 0;
    this.policyDismissed = !!sessionStorage.getItem('policy_dismissed');
    this.load();

    // Keep sidebar highlight in sync with the current URL
    this.syncActiveSection(this.router.url);
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => this.syncActiveSection(e.urlAfterRedirects || e.url));
  }

  private syncActiveSection(url: string): void {
    const after = url.split('/dashboard/customer/')[1];
    this.activeSection = after ? after.split('?')[0] : 'home';
  }

  load(): void {
    this.loading = true;
    this.dashboardService.getDashboard(this.customerId).subscribe({
      next: data => {
        this.dashboard = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Dashboard load error:', err);
        // Show empty dashboard so the page isn't blank
        this.dashboard = {
          customerName: this.auth.getUserName() || 'Customer',
          profilePhotoUrl: '',
          cityName: '',
          memberSince: '',
          upcomingAppointments: [],
          pendingAppointments: [],
          pendingReviews: [],
          totalLoyaltyPoints: 0,
          unreadNotificationCount: 0,
          pendingOrderCount: 0,
          beautyProfileComplete: false,
          latestGlobalPolicy: null
        };
        this.loading = false;
      }
    });
  }

  navigate(section: string): void {
    this.activeSection = section;
    this.router.navigate(['/dashboard/customer/' + section]);
  }

  dismissPolicy(): void {
    sessionStorage.setItem('policy_dismissed', 'true');
    this.policyDismissed = true;
  }

  statusBadge(status: string): string {
    const m: Record<string, string> = {
      PENDING: 'warning text-dark',
      CONFIRMED: 'success',
      COMPLETED: 'primary',
      CANCELLED: 'danger'
    };
    return `badge bg-${m[status] ?? 'secondary'}`;
  }

  statusLabel(status: string): string {
    if (status === 'PENDING') return 'Awaiting Assignment';
    return status;
  }

  stars(n: number): number[] { return Array.from({ length: 5 }, (_, i) => i + 1); }
}
