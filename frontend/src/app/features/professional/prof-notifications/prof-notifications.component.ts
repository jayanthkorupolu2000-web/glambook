import { Component, OnInit } from '@angular/core';
import { ProfessionalNotificationResponse } from '../../../models/professional.model';
import { AuthService } from '../../../services/auth.service';
import { ProfessionalNotificationService } from '../../../services/professional-notification.service';

@Component({
  selector: 'app-prof-notifications',
  templateUrl: './prof-notifications.component.html'
})
export class ProfNotificationsComponent implements OnInit {
  notifications: ProfessionalNotificationResponse[] = [];
  loading = false;
  profId = 0;

  constructor(private notifService: ProfessionalNotificationService, private auth: AuthService) {}

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.notifService.getNotifications(this.profId).subscribe({
      next: data => { this.notifications = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  markAllRead(): void {
    this.notifService.markAllAsRead(this.profId).subscribe(() => this.load());
  }

  markRead(id: number): void {
    this.notifService.markAsRead(this.profId, id).subscribe(() => {
      const n = this.notifications.find(x => x.id === id);
      if (n) n.isRead = true;
    });
  }

  borderColor(type: string): string {
    const map: Record<string, string> = {
      APPOINTMENT_CONFIRMED: '#28a745',
      APPOINTMENT_CANCELLED: '#dc3545',
      NEW_REVIEW: '#17a2b8',
      COMPLAINT_RAISED: '#ffc107',
      SUSPENSION_WARNING: '#dc3545',
      POLICY_PUBLISHED: '#244AFD'
    };
    return map[type] ?? '#244AFD';
  }

  icon(type: string): string {
    const map: Record<string, string> = {
      APPOINTMENT_CONFIRMED: '✅',
      APPOINTMENT_CANCELLED: '❌',
      NEW_REVIEW: '⭐',
      COMPLAINT_RAISED: '⚠️',
      SUSPENSION_WARNING: '🚫',
      POLICY_PUBLISHED: '📋'
    };
    return map[type] ?? '🔔';
  }
}
