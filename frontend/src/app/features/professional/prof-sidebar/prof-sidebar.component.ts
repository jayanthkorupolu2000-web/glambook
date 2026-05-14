import { Component, EventEmitter, HostListener, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

interface SidebarLink {
  label: string;
  route: string;
  emoji: string;
}

@Component({
  selector: 'app-prof-sidebar',
  templateUrl: './prof-sidebar.component.html',
  styleUrls: ['./prof-sidebar.component.scss']
})
export class ProfSidebarComponent implements OnInit {
  @Output() sidebarToggled = new EventEmitter<boolean>();

  isOpen = true;
  userName = '';
  userInitial = '';

  links: SidebarLink[] = [
    { label: 'Dashboard',     route: '/dashboard/professional',              emoji: '🏠' },
    { label: 'Profile',       route: '/dashboard/professional/profile',      emoji: '👤' },
    { label: 'Services',      route: '/dashboard/professional/services',     emoji: '✂️' },
    { label: 'Availability',  route: '/dashboard/professional/availability', emoji: '📅' },
    { label: 'Schedule',      route: '/dashboard/professional/schedule',     emoji: '🗓️' },
    { label: 'Appointments',  route: '/dashboard/professional/appointments', emoji: '📋' },
    { label: 'Reviews',       route: '/dashboard/professional/reviews',      emoji: '⭐' },
    { label: 'Analytics',     route: '/dashboard/professional/analytics',    emoji: '📊' },
    { label: 'Consultations', route: '/dashboard/professional/consultations',emoji: '💬' },
    { label: 'Policies',      route: '/dashboard/professional/policies',     emoji: '📄' },
    { label: 'Notifications', route: '/dashboard/professional/notifications',emoji: '🔔' },
  ];

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.userName = this.authService.getUserName() || 'Professional';
    this.userInitial = this.userName.charAt(0).toUpperCase();
    // Close by default on small screens
    if (window.innerWidth < 992) {
      this.isOpen = false;
      this.sidebarToggled.emit(false);
    }
  }

  toggle(): void {
    this.isOpen = !this.isOpen;
    this.sidebarToggled.emit(this.isOpen);
  }

  close(): void {
    this.isOpen = false;
    this.sidebarToggled.emit(false);
  }

  onLinkClick(): void {
    // Close sidebar on mobile after navigation
    if (window.innerWidth < 992) {
      this.close();
    }
  }

  @HostListener('window:resize')
  onResize(): void {
    if (window.innerWidth >= 992 && !this.isOpen) {
      this.isOpen = true;
      this.sidebarToggled.emit(true);
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth']);
  }
}
