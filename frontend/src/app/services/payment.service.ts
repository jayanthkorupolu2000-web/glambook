import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Payment, PaymentRequest } from '../models';

const API_BASE = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  constructor(private http: HttpClient) {}

  process(request: PaymentRequest): Observable<Payment> {
    return this.http.post<Payment>(`${API_BASE}/api/payments`, request);
  }

  getByAppointment(appointmentId: number): Observable<Payment> {
    return this.http.get<Payment>(`${API_BASE}/api/payments/appointment/${appointmentId}`);
  }
}
