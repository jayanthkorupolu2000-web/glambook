import { Component } from '@angular/core';

@Component({
  selector: 'app-admin-dashboard',
  template: `
    <div class="container-fluid py-4">
      <h1 class="fw-bold mb-4" style="color:#244AFD;">Admin Dashboard</h1>
      <div class="row g-3">
        <div class="col-md-4 col-lg-3" *ngFor="let card of cards">
          <div class="card border-0 shadow-sm h-100 text-center p-3" style="cursor:pointer;transition:transform .2s"
               (mouseenter)="card.hover=true" (mouseleave)="card.hover=false"
               [style.transform]="card.hover ? 'translateY(-3px)' : ''">
            <div class="fs-1 mb-2">{{ card.icon }}</div>
            <h6 class="fw-bold">{{ card.title }}</h6>
            <p class="text-muted small mb-3">{{ card.desc }}</p>
            <a [routerLink]="card.route" class="btn btn-sm text-white fw-semibold" style="background:#244AFD;">
              Open
            </a>
          </div>
        </div>
      </div>
    </div>
  `
})
export class AdminDashboardComponent {
  cards = [
    { icon: '👥', title: 'User Management', desc: 'View all users', route: '/dashboard/admin/users', hover: false },
    { icon: '🏪', title: 'Salon Owners', desc: 'Manage salon owners', route: '/dashboard/admin/owners', hover: false },
    { icon: '📊', title: 'Reports', desc: 'Appointments & payments', route: '/dashboard/admin/reports', hover: false },
    { icon: '📋', title: 'Complaints', desc: 'Review & resolve complaints', route: '/dashboard/admin/complaints', hover: false },
    { icon: '🔒', title: 'Suspensions', desc: 'Manage user suspensions', route: '/dashboard/admin/user-management', hover: false },
    { icon: '📈', title: 'Analytics', desc: 'Detailed analytics', route: '/dashboard/admin/analytics', hover: false },
    { icon: '📜', title: 'Policies', desc: 'Publish & manage policies', route: '/dashboard/admin/policy', hover: false },
  ];
}
