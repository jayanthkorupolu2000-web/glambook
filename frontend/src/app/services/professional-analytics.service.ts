import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ProfessionalAnalyticsResponse } from '../models/professional.model';

const BASE = 'http://localhost:8080/api/v1/professionals';

@Injectable({ providedIn: 'root' })
export class ProfessionalAnalyticsService {
  constructor(private http: HttpClient) {}

  getAnalytics(profId: number): Observable<ProfessionalAnalyticsResponse> {
    return this.http.get<ProfessionalAnalyticsResponse>(`${BASE}/${profId}/analytics`);
  }
}
