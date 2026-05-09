import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ProfessionalNotificationResponse } from '../models/professional.model';

const BASE = 'http://localhost:8080/api/v1/professionals';

@Injectable({ providedIn: 'root' })
export class ProfessionalNotificationService {
  constructor(private http: HttpClient) {}

  getNotifications(profId: number): Observable<ProfessionalNotificationResponse[]> {
    return this.http.get<ProfessionalNotificationResponse[]>(`${BASE}/${profId}/notifications`);
  }

  getUnreadCount(profId: number): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${BASE}/${profId}/notifications/unread-count`);
  }

  markAsRead(profId: number, notifId: number): Observable<void> {
    return this.http.patch<void>(`${BASE}/${profId}/notifications/${notifId}/read`, {});
  }

  markAllAsRead(profId: number): Observable<void> {
    return this.http.patch<void>(`${BASE}/${profId}/notifications/read-all`, {});
  }
}
