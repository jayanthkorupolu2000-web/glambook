import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

const BASE = 'http://localhost:8080/api/v1/professionals';

/** Mirrors the backend ScheduleSlotResponse DTO */
export interface ScheduleSlot {
  id: number;
  date: string;
  startTime: string;
  endTime: string;
  slotType: 'WORKING' | 'LUNCH_BREAK' | 'BREAK' | 'BLOCKED';
  slotStatus: 'AVAILABLE' | 'UNAVAILABLE' | 'COMPLETED';
  booked: boolean;
  past: boolean;
}

@Injectable({ providedIn: 'root' })
export class ScheduleService {
  constructor(private http: HttpClient) {}

  /**
   * Returns today's schedule for a professional.
   * Past slots (endTime <= now) are excluded by the backend.
   */
  getTodaySchedule(professionalId: number): Observable<ScheduleSlot[]> {
    return this.http.get<ScheduleSlot[]>(`${BASE}/${professionalId}/schedule/today`);
  }

  /**
   * Returns all slots for a given date with their computed slotStatus.
   * Use this in the booking component to show AVAILABLE / UNAVAILABLE / COMPLETED.
   */
  getSlotsByDate(professionalId: number, date: string): Observable<ScheduleSlot[]> {
    return this.http.get<ScheduleSlot[]>(`${BASE}/${professionalId}/schedule/${date}/slots`);
  }
}
