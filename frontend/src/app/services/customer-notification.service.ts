import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CustomerNotificationResponse } from '../models/customer.model';

const BASE = 'http://localhost:8080/api/v1/customers';

@Injectable({ providedIn: 'root' })
export class CustomerNotificationService {
  constructor(private http: HttpClient) {}

  getNotifications(customerId: number): Observable<CustomerNotificationResponse[]> {
    return this.http.get<CustomerNotificationResponse[]>(`${BASE}/${customerId}/notifications`);
  }

  getUnreadCount(customerId: number): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${BASE}/${customerId}/notifications/unread-count`);
  }

  markAsRead(customerId: number, notifId: number): Observable<void> {
    return this.http.patch<void>(`${BASE}/${customerId}/notifications/${notifId}/read`, {});
  }

  markAllAsRead(customerId: number): Observable<void> {
    return this.http.patch<void>(`${BASE}/${customerId}/notifications/read-all`, {});
  }
}
