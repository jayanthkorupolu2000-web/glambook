import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { OwnerNotificationResponse } from '../models/owner.model';

const BASE = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class OwnerNotificationService {
  constructor(private http: HttpClient) {}

  getNotifications(ownerId: number): Observable<OwnerNotificationResponse[]> {
    return this.http.get<OwnerNotificationResponse[]>(`${BASE}/owners/${ownerId}/notifications`);
  }

  getUnreadCount(ownerId: number): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${BASE}/owners/${ownerId}/notifications/unread-count`);
  }

  markAsRead(ownerId: number, notifId: number): Observable<void> {
    return this.http.patch<void>(`${BASE}/owners/${ownerId}/notifications/${notifId}/read`, {});
  }

  markAllAsRead(ownerId: number): Observable<void> {
    return this.http.patch<void>(`${BASE}/owners/${ownerId}/notifications/read-all`, {});
  }
}
