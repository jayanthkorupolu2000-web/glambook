import { Component, OnInit } from '@angular/core';
import { CustomerNotificationResponse } from '../../../models/customer.model';
import { AuthService } from '../../../services/auth.service';
import { CustomerNotificationService } from '../../../services/customer-notification.service';

@Component({
  selector: 'app-customer-notifications',
  templateUrl: './customer-notifications.component.html'
})
export class CustomerNotificationsComponent implements OnInit {
  notifications: CustomerNotificationResponse[] = [];
  loading = false;
  customerId = 0;

  constructor(private notifService: CustomerNotificationService, private auth: AuthService) {}

  ngOnInit(): void {
    this.customerId = this.auth.getUserId() || 0;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.notifService.getNotifications(this.customerId).subscribe({
      next: data => { this.notifications = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  markAllRead(): void {
    this.notifService.markAllAsRead(this.customerId).subscribe(() => this.load());
  }

  markRead(id: number): void {
    this.notifService.markAsRead(this.customerId, id).subscribe(() => {
      const n = this.notifications.find(x => x.id === id);
      if (n) n.isRead = true;
    });
  }

  borderColor(type: string): string {
    const m: Record<string, string> = {
      BOOKING_CONFIRMED: '#28a745', BOOKING_CANCELLED: '#dc3545',
      PAYMENT_SUCCESS: '#007bff', PAYMENT_REFUNDED: '#fd7e14',
      REVIEW_RESPONSE: '#6f42c1', LOYALTY_POINTS_EARNED: '#ffc107',
      POLICY_UPDATED: '#6c757d', PROMOTION_AVAILABLE: '#20c997',
      CONSULTATION_CONFIRMED: '#6610f2', COMMUNICATION_RECEIVED: '#17a2b8'
    };
    return m[type] ?? '#244AFD';
  }

  icon(type: string): string {
    const m: Record<string, string> = {
      BOOKING_CONFIRMED: '✅', BOOKING_CANCELLED: '❌',
      PAYMENT_SUCCESS: '💳', PAYMENT_REFUNDED: '💰',
      REVIEW_RESPONSE: '💬', LOYALTY_POINTS_EARNED: '⭐',
      POLICY_UPDATED: '📋', PROMOTION_AVAILABLE: '🎉',
      CONSULTATION_CONFIRMED: '🩺', COMMUNICATION_RECEIVED: '📩'
    };
    return m[type] ?? '🔔';
  }
}
