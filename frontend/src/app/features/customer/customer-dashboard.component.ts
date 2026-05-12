import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { CustomerDashboardDTO } from '../../models/customer.model';
import { AuthService } from '../../services/auth.service';
import { CustomerDashboardService } from '../../services/customer-dashboard.service';

@Component({
  selector: 'app-customer-dashboard',
  templateUrl: './customer-dashboard.component.html',
  styleUrls: ['./customer-dashboard.component.scss']
})
export class CustomerDashboardComponent implements OnInit {
  dashboard: CustomerDashboardDTO | null = null;
  activeSection = 'home';
  sidebarOpen = true;

  sidebarLinks = [
    { id: 'home',          icon: '🏠', label: 'Dashboard' },
    { id: 'search',        icon: '🔍', label: 'Book a Service' },
    { id: 'appointments',  icon: '📅', label: 'My Appointments' },
    { id: 'beauty-profile',icon: '👤', label: 'Beauty Profile' },
    { id: 'consultations', icon: '💬', label: 'Consultations' },
    { id: 'products',      icon: '🛍️', label: 'Products' },
    { id: 'favorites',     icon: '❤️', label: 'My Favorites' },
    { id: 'orders',        icon: '📦', label: 'My Orders' },
    { id: 'loyalty',       icon: '⭐', label: 'Loyalty & Rewards' },
    { id: 'complaints',    icon: '📣', label: 'My Complaints' },
    { id: 'notifications', icon: '🔔', label: 'Notifications' }
  ];

  constructor(
    private auth: AuthService,
    private router: Router,
    private dashboardService: CustomerDashboardService
  ) {}

  ngOnInit(): void {
    const customerId = this.auth.getUserId() || 0;
    this.dashboardService.getDashboard(customerId).subscribe({
      next: data => { this.dashboard = data; },
      error: () => {
        this.dashboard = {
          customerName: this.auth.getUserName() || 'Customer',
          profilePhotoUrl: '', cityName: '', memberSince: '',
          upcomingAppointments: [], pendingAppointments: [],
          pendingReviews: [], totalLoyaltyPoints: 0,
          unreadNotificationCount: 0, pendingOrderCount: 0,
          beautyProfileComplete: false, latestGlobalPolicy: null,
          suspensionReason: null
        };
      }
    });

    this.syncActiveSection(this.router.url);
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => this.syncActiveSection(e.urlAfterRedirects || e.url));
  }

  private syncActiveSection(url: string): void {
    const after = url.split('/dashboard/customer/')[1];
    this.activeSection = after ? after.split('?')[0] : 'home';
  }

  navigate(section: string): void {
    this.activeSection = section;
    this.router.navigate(['/dashboard/customer/' + section]);
    if (window.innerWidth < 768) {
      this.sidebarOpen = false;
    }
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }
}
