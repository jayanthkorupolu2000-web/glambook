import { Component, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { Role } from '../../../models';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class NavbarComponent implements OnInit, OnDestroy {
  currentRole: Role | null = null;
  isLoggedIn = false;
  navigationLinks: { label: string; route: string }[] = [];
  roleDisplayName = '';
  private sub?: Subscription;

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.updateAuthState();
    this.sub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => this.updateAuthState());
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  private updateAuthState(): void {
    this.currentRole = this.authService.getRole();
    this.isLoggedIn = this.authService.isLoggedIn();
    this.navigationLinks = this.buildNavigationLinks();
    this.roleDisplayName = this.buildRoleDisplayName();
  }

  private buildNavigationLinks(): { label: string; route: string }[] {
    if (!this.isLoggedIn || !this.currentRole) return [];
    switch (this.currentRole) {
      case 'CUSTOMER': return [
        { label: 'Dashboard', route: '/dashboard/customer' },
        { label: 'Book Service', route: '/dashboard/customer/search' },
        { label: 'Appointments', route: '/dashboard/customer/appointments' },
        { label: 'Profile', route: '/dashboard/customer/profile' },
        { label: '🔔', route: '/dashboard/customer/notifications' }
      ];
      case 'SALON_OWNER': return [
        { label: 'Dashboard', route: '/dashboard/owner' },
        { label: 'Staff', route: '/dashboard/owner/staff' },
        { label: 'Services', route: '/dashboard/owner/services' },
        { label: 'Appointments', route: '/dashboard/owner/appointments' },
        { label: 'Promotions', route: '/dashboard/owner/promotions' },
        { label: 'Loyalty', route: '/dashboard/owner/loyalty' },
        { label: 'Policies', route: '/dashboard/owner/policies' },
        { label: 'Complaints', route: '/dashboard/owner/complaints' },
        { label: 'Reports', route: '/dashboard/owner/reports' },
        { label: '🔔', route: '/dashboard/owner/notifications' }
      ];
      case 'PROFESSIONAL': return [
        { label: '🏠 Dashboard', route: '/dashboard/professional' },
        { label: '👤 Profile', route: '/dashboard/professional/profile' },
        { label: '✂️ Services', route: '/dashboard/professional/services' },
        { label: '📅 Availability', route: '/dashboard/professional/availability' },
        { label: '🗓️ Schedule', route: '/dashboard/professional/schedule' },
        { label: '📋 Policies', route: '/dashboard/professional/policies' },
        { label: '⭐ Reviews', route: '/dashboard/professional/reviews' },
        { label: '📊 Analytics', route: '/dashboard/professional/analytics' },
        { label: '🔔 Notifications', route: '/dashboard/professional/notifications' }
      ];
      case 'ADMIN': return [
        { label: 'Dashboard', route: '/dashboard/admin' },
        { label: 'Users', route: '/dashboard/admin/users' },
        { label: 'Owners', route: '/dashboard/admin/owners' },
        { label: 'Reports', route: '/dashboard/admin/reports' },
        { label: 'Complaints', route: '/dashboard/admin/complaints' },
        { label: 'Suspensions', route: '/dashboard/admin/user-management' },
        { label: 'Analytics', route: '/dashboard/admin/analytics' },
        { label: 'Policies', route: '/dashboard/admin/policy' }
      ];
      default: return [];
    }
  }

  private buildRoleDisplayName(): string {
    const map: Record<string, string> = {
      CUSTOMER: 'Customer', SALON_OWNER: 'Salon Owner',
      PROFESSIONAL: 'Professional', ADMIN: 'Admin'
    };
    return this.currentRole ? (map[this.currentRole] ?? '') : '';
  }

  logout(): void {
    this.authService.logout();
    this.updateAuthState();
    this.router.navigate(['/auth']);
  }

  goHome(): void {
    const roleRoutes: Record<string, string> = {
      CUSTOMER: '/dashboard/customer',
      SALON_OWNER: '/dashboard/owner',
      PROFESSIONAL: '/dashboard/professional',
      ADMIN: '/dashboard/admin'
    };
    const route = this.currentRole ? (roleRoutes[this.currentRole] ?? '/auth') : '/auth';
    this.router.navigate([route]);
  }
}