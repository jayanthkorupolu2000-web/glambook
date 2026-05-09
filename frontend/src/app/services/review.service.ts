import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Review, ReviewRequest } from '../models';

const API_BASE = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  constructor(private http: HttpClient) {}

  submit(request: ReviewRequest): Observable<Review> {
    return this.http.post<Review>(`${API_BASE}/api/reviews`, request);
  }

  getByProfessional(professionalId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${API_BASE}/api/reviews/professional/${professionalId}`);
  }

  existsByAppointment(appointmentId: number): Observable<{ exists: boolean }> {
    return this.http.get<{ exists: boolean }>(
      `${API_BASE}/api/reviews/exists?appointmentId=${appointmentId}`
    );
  }
}
