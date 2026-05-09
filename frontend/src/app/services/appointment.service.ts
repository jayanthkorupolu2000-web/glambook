import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Appointment, AppointmentRequest } from '../models';

const API_BASE = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  constructor(private http: HttpClient) {}

  book(request: AppointmentRequest): Observable<Appointment> {
    return this.http.post<Appointment>(`${API_BASE}/api/appointments`, request);
  }

  getByCustomer(customerId: number): Observable<Appointment[]> {
    const params = new HttpParams().set('customerId', customerId);
    return this.http.get<Appointment[]>(`${API_BASE}/api/appointments`, { params });
  }

  getBySalon(salonOwnerId: number): Observable<Appointment[]> {
    const params = new HttpParams().set('salonOwnerId', salonOwnerId);
    return this.http.get<Appointment[]>(`${API_BASE}/api/appointments`, { params });
  }

  cancel(appointmentId: number): Observable<void> {
    return this.http.patch<void>(`${API_BASE}/api/appointments/${appointmentId}/cancel`, {});
  }
}
