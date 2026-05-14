import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { SidebarToggleService } from '../../services/sidebar-toggle.service';

@Component({
  selector: 'app-owner-dashboard',
  templateUrl: './owner-dashboard.component.html',
  styleUrls: ['./owner-dashboard.component.scss']
})
export class OwnerDashboardComponent implements OnInit, OnDestroy {
  ownerName = '';
  activeSection = 'dashboard';
  sidebarOpen = true;
  private toggleSub?: Subscription;

  sidebarLinks = [
    { id: 'dashboard',     icon: '🏠', label: 'Dashboard' },
    { id: 'profile',       icon: '👤', label: 'My Profile' },
    { id: 'staff',         icon: '👥', label: 'Staff & Approvals' },
    { id: 'services',      icon: '✂️',  label: 'Services' },
    { id: 'appointments',  icon: '📅', label: 'Appointments' },
    { id: 'promotions',    icon: '🎉', label: 'Promotions' },
    { id: 'loyalty',       icon: '⭐', label: 'Loyalty' },
    { id: 'policies',      icon: '📋', label: 'Policies' },
    { id: 'complaints',    icon: '📣', label: 'Complaints' },
    { id: 'reports',       icon: '📊', label: 'Reports' },
    { id: 'notifications', icon: '🔔', label: 'Notifications' }
  ];

  constructor(
    private router: Router,
    private auth: AuthService,
    private sidebarToggleService: SidebarToggleService
  ) {}

  ngOnInit(): void {
    this.ownerName = this.auth.getUserName() || 'Owner';
    this.syncSection(this.router.url);
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => this.syncSection(e.urlAfterRedirects || e.url));

    // Listen to navbar hamburger
    this.toggleSub = this.sidebarToggleService.toggle$.subscribe(() => this.toggleSidebar());
  }

  ngOnDestroy(): void {
    this.toggleSub?.unsubscribe();
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  private syncSection(url: string): void {
    const seg = url.split('/dashboard/owner/')[1]?.split('?')[0];
    this.activeSection = seg || 'dashboard';
  }

  navigate(section: string): void {
    this.activeSection = section;
    this.router.navigate(['/dashboard/owner/' + section]);
    if (window.innerWidth < 768) {
      this.sidebarOpen = false;
    }
  }
}
