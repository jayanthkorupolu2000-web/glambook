import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-owner-dashboard',
  styles: [`
    .owner-nav-btn {
      background: transparent;
      border: none;
      color: #333;
      cursor: pointer;
      transition: background 0.15s, color 0.15s;
    }
    .owner-nav-btn:hover {
      background: #f1f5f9;
      color: #334155;
    }
    .owner-nav-active {
      background: #334155 !important;
      color: #fff !important;
    }
  `],
  template: `
<div class="d-flex min-vh-100" style="background:#f8f9fa;">

  <!-- Sidebar -->
  <div class="d-flex flex-column border-end bg-white shadow-sm"
       style="width:220px;min-height:100vh;flex-shrink:0;">

    <div class="text-center py-4 px-3 border-bottom">
      <div class="rounded-circle d-inline-flex align-items-center justify-content-center mb-2"
           style="width:52px;height:52px;background:#334155;color:#fff;font-size:1.3rem;font-weight:700;">
        {{ ownerName.charAt(0) }}
      </div>
      <div class="fw-semibold small">{{ ownerName }}</div>
      <div class="text-muted" style="font-size:.75rem;">Salon Owner</div>
    </div>

    <nav class="flex-fill py-2 px-2">
      <button *ngFor="let link of sidebarLinks"
              class="owner-nav-btn w-100 text-start rounded px-3 py-2 mb-1 d-flex align-items-center gap-2"
              [class.owner-nav-active]="activeSection === link.id"
              (click)="navigate(link.id)">
        <span>{{ link.icon }}</span>
        <span class="small fw-semibold">{{ link.label }}</span>
      </button>
    </nav>
  </div>

  <!-- Main content -->
  <div class="flex-fill overflow-auto">
    <router-outlet></router-outlet>
  </div>

</div>
  `
})
export class OwnerDashboardComponent implements OnInit {
  ownerName = '';
  activeSection = 'staff';

  sidebarLinks = [
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

  constructor(private router: Router, private auth: AuthService) {}

  ngOnInit(): void {
    this.ownerName = this.auth.getUserName() || 'Owner';
    this.syncSection(this.router.url);
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => this.syncSection(e.urlAfterRedirects || e.url));
  }

  private syncSection(url: string): void {
    const seg = url.split('/dashboard/owner/')[1]?.split('?')[0];
    this.activeSection = seg || 'staff';
  }

  navigate(section: string): void {
    this.activeSection = section;
    this.router.navigate(['/dashboard/owner/' + section]);
  }
}
