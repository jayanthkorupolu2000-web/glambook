import { Component, OnInit } from '@angular/core';
import { ProfessionalNotificationResponse } from '../../../models/professional.model';
import { AuthService } from '../../../services/auth.service';
import { ProfessionalNotificationService } from '../../../services/professional-notification.service';

@Component({
  selector: 'app-prof-notifications',
  templateUrl: './prof-notifications.component.html',
  styleUrls: ['./prof-notifications.component.css']
})
export class ProfNotificationsComponent implements OnInit {
  notifications: ProfessionalNotificationResponse[] = [];
  loading = false;
  profId = 0;
  showAll = false;   // false = show unread only; true = show all

  constructor(
    private notifService: ProfessionalNotificationService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.notifService.getNotifications(this.profId).subscribe({
      next: data => {
        this.notifications = data;
        this.loading = false;
        // Auto-mark all unread as read silently when the page is opened
        const hasUnread = data.some(n => !n.isRead);
        if (hasUnread) {
          this.notifService.markAllAsRead(this.profId).subscribe({
            next: () => {
              // Update local state — mark all as read in memory
              this.notifications.forEach(n => n.isRead = true);
            },
            error: () => {} // silent — don't disrupt the UI
          });
        }
      },
      error: () => this.loading = false
    });
  }

  get displayed(): ProfessionalNotificationResponse[] {
    if (this.showAll) return this.notifications;
    // When showAll is false, show unread first then recently-read (all, but unread highlighted)
    return this.notifications;
  }

  get unreadCount(): number {
    return this.notifications.filter(n => !n.isRead).length;
  }

  markAllRead(): void {
    this.notifService.markAllAsRead(this.profId).subscribe({
      next: () => {
        this.notifications.forEach(n => n.isRead = true);
      }
    });
  }

  markRead(id: number): void {
    this.notifService.markAsRead(this.profId, id).subscribe({
      next: () => {
        const n = this.notifications.find(x => x.id === id);
        if (n) n.isRead = true;
      }
    });
  }

  typeColor(type: string): string {
    const map: Record<string, string> = {
      APPOINTMENT_CONFIRMED: '#1a9e5c',
      APPOINTMENT_CANCELLED: '#c0392b',
      NEW_REVIEW:            '#f5a623',
      COMPLAINT_RAISED:      '#c77c00',
      SUSPENSION_WARNING:    '#c0392b',
      POLICY_PUBLISHED:      '#2d4a6e'
    };
    return map[type] ?? '#2d4a6e';
  }

  typeBg(type: string): string {
    const map: Record<string, string> = {
      APPOINTMENT_CONFIRMED: '#e6f9f0',
      APPOINTMENT_CANCELLED: '#fdecea',
      NEW_REVIEW:            '#fff8e1',
      COMPLAINT_RAISED:      '#fff8e1',
      SUSPENSION_WARNING:    '#fdecea',
      POLICY_PUBLISHED:      '#eef2f8'
    };
    return map[type] ?? '#eef2f8';
  }

  typeIcon(type: string): string {
    const map: Record<string, string> = {
      APPOINTMENT_CONFIRMED: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
      APPOINTMENT_CANCELLED: 'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z',
      NEW_REVIEW:            'M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z',
      COMPLAINT_RAISED:      'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z',
      SUSPENSION_WARNING:    'M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636',
      POLICY_PUBLISHED:      'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z'
    };
    return map[type] ?? 'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9';
  }
}
