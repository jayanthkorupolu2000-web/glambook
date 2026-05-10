import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-owner-dashboard',
  styles: [`
    .layout {
      display: flex;
      min-height: 100vh;
      background: #f2f5fa;
      position: relative;
    }

    /* ── Sidebar ── */
    .owner-sidebar {
      width: 230px;
      min-height: 100vh;
      background: #fff;
      border-right: 1px solid #e2e8f0;
      box-shadow: 2px 0 10px rgba(0,0,0,.07);
      display: flex;
      flex-direction: column;
      flex-shrink: 0;
      transition: width .28s cubic-bezier(.4,0,.2,1),
                  opacity .22s ease,
                  transform .28s cubic-bezier(.4,0,.2,1);
      overflow: hidden;
    }
    .owner-sidebar.closed {
      width: 0;
      opacity: 0;
      transform: translateX(-20px);
      pointer-events: none;
    }

    /* ── Profile block ── */
    .sidebar-profile {
      text-align: center;
      padding: 1.4rem .75rem 1rem;
      border-bottom: 1px solid #f1f5f9;
      flex-shrink: 0;
    }
    .profile-avatar {
      width: 52px; height: 52px;
      border-radius: 50%;
      background: #334155;
      color: #fff;
      font-size: 1.3rem;
      font-weight: 700;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }
    .profile-name {
      font-size: .85rem;
      font-weight: 700;
      color: #1e293b;
      margin-top: .45rem;
      white-space: nowrap;
    }
    .profile-role {
      font-size: .72rem;
      color: #94a3b8;
      white-space: nowrap;
    }

    /* ── Nav ── */
    .sidebar-nav {
      flex: 1;
      padding: .5rem .45rem;
      overflow-y: auto;
      overflow-x: hidden;
    }
    .owner-nav-btn {
      background: transparent;
      border: none;
      color: #475569;
      cursor: pointer;
      transition: background .15s, color .15s;
      width: 100%;
      text-align: left;
      border-radius: 9px;
      padding: .6rem .85rem;
      margin-bottom: 2px;
      display: flex;
      align-items: center;
      gap: .7rem;
      white-space: nowrap;
    }
    .owner-nav-btn:hover {
      background: #f1f5f9;
      color: #334155;
    }
    .owner-nav-active {
      background: #334155 !important;
      color: #fff !important;
    }
    .nav-icon { font-size: 1.1rem; flex-shrink: 0; }
    .nav-label { font-size: .82rem; font-weight: 600; }

    /* ── Main area ── */
    .main-area {
      flex: 1;
      overflow: auto;
      display: flex;
      flex-direction: column;
      min-width: 0;
    }

    /* ── Top bar ── */
    .top-bar {
      display: flex;
      align-items: center;
      gap: .75rem;
      padding: .6rem 1.25rem;
      background: #fff;
      border-bottom: 1px solid #e2e8f0;
      box-shadow: 0 1px 4px rgba(0,0,0,.05);
      flex-shrink: 0;
      position: sticky;
      top: 0;
      z-index: 20;
    }

    /* ── Hamburger button ── */
    .hamburger {
      width: 36px;
      height: 36px;
      border-radius: 8px;
      background: #f1f5f9;
      border: 1px solid #e2e8f0;
      cursor: pointer;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 5px;
      flex-shrink: 0;
      transition: background .15s;
    }
    .hamburger:hover { background: #e2e8f0; }
    .hamburger span {
      display: block;
      width: 18px;
      height: 2px;
      background: #334155;
      border-radius: 2px;
      transition: all .25s ease;
    }
    /* Animate to X when open */
    .hamburger.is-open span:nth-child(1) {
      transform: translateY(7px) rotate(45deg);
    }
    .hamburger.is-open span:nth-child(2) {
      opacity: 0;
      transform: scaleX(0);
    }
    .hamburger.is-open span:nth-child(3) {
      transform: translateY(-7px) rotate(-45deg);
    }

    .top-bar-title {
      font-size: .9rem;
      font-weight: 700;
      color: #334155;
    }
  `],
  template: `
<div class="layout">

  <!-- ── Sidebar ── -->
  <div class="owner-sidebar" [class.closed]="!sidebarOpen">

    <div class="sidebar-profile">
      <div class="profile-avatar">{{ ownerName.charAt(0) }}</div>
      <div class="profile-name">{{ ownerName }}</div>
      <div class="profile-role">Salon Owner</div>
    </div>

    <nav class="sidebar-nav">
      <button *ngFor="let link of sidebarLinks"
              class="owner-nav-btn"
              [class.owner-nav-active]="activeSection === link.id"
              (click)="navigate(link.id)">
        <span class="nav-icon">{{ link.icon }}</span>
        <span class="nav-label">{{ link.label }}</span>
      </button>
    </nav>
  </div>

  <!-- ── Main area ── -->
  <div class="main-area">

    <!-- Top bar with hamburger -->
    <div class="top-bar">
      <button class="hamburger" [class.is-open]="sidebarOpen" (click)="toggleSidebar()"
              [title]="sidebarOpen ? 'Close menu' : 'Open menu'">
        <span></span>
        <span></span>
        <span></span>
      </button>
      <span class="top-bar-title">Salon Management</span>
    </div>

    <router-outlet></router-outlet>
  </div>

</div>
  `
})
export class OwnerDashboardComponent implements OnInit {
  ownerName = '';
  activeSection = 'dashboard';
  sidebarOpen = true;

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

  constructor(private router: Router, private auth: AuthService) {}

  ngOnInit(): void {
    this.ownerName = this.auth.getUserName() || 'Owner';
    this.syncSection(this.router.url);
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => this.syncSection(e.urlAfterRedirects || e.url));
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
  }
}
