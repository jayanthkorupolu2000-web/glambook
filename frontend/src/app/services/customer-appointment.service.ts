import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppointmentResponse } from '../models/customer.model';

const BASE = 'http://localhost:8080/api/appointments';

@Injectable({ providedIn: 'root' })
export class CustomerAppointmentService {
  constructor(private http: HttpClient) {}

  /** Fetches all appointments for the currently logged-in customer (uses JWT) */
  getHistory(customerId: number): Observable<AppointmentResponse[]> {
    // Pass customerId as query param — backend validates against JWT
    return this.http.get<AppointmentResponse[]>(`${BASE}?customerId=${customerId}`);
  }

  getUpcoming(customerId: number): Observable<AppointmentResponse[]> {
    return this.http.get<AppointmentResponse[]>(`${BASE}?customerId=${customerId}`);
  }

  cancel(customerId: number, appointmentId: number): Observable<AppointmentResponse> {
    return this.http.patch<AppointmentResponse>(`${BASE}/${appointmentId}/cancel`, {});
  }

  book(customerId: number, dto: any): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(`${BASE}`, dto);
  }
}
