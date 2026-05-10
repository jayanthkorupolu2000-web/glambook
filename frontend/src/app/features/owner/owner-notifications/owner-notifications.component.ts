import { Component, OnInit } from '@angular/core';
import { OwnerNotificationResponse } from '../../../models/owner.model';
import { OwnerIdService } from '../../../services/owner-id.service';
import { OwnerNotificationService } from '../../../services/owner-notification.service';

@Component({
  selector: 'app-owner-notifications',
  templateUrl: './owner-notifications.component.html'
})
export class OwnerNotificationsComponent implements OnInit {
  notifications: OwnerNotificationResponse[] = [];
  loading = false;
  ownerId = 0;

  constructor(private notifService: OwnerNotificationService, private ownerIdService: OwnerIdService) {}

  ngOnInit(): void {
    this.ownerIdService.getOwnerId().subscribe(id => {
      this.ownerId = id;
      this.load();
    });
  }

  load(): void {
    this.loading = true;
    this.notifService.getNotifications(this.ownerId).subscribe({
      next: data => { this.notifications = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  markAllRead(): void {
    this.notifService.markAllAsRead(this.ownerId).subscribe({
      next: () => this.notifications.forEach(n => n.isRead = true)
    });
  }

  markRead(id: number): void {
    const n = this.notifications.find(x => x.id === id);
    if (!n || n.isRead) return;
    this.notifService.markAsRead(this.ownerId, id).subscribe({
      next: () => { if (n) n.isRead = true; }
    });
  }

  typeIcon(type: string): string {
    const map: Record<string, string> = {
      NEW_BOOKING: '📅', COMPLAINT_FORWARDED: '⚠️',
      PROFESSIONAL_PENDING: '👤', APPOINTMENT_CANCELLED: '❌'
    };
    return map[type] ?? '🔔';
  }
}
