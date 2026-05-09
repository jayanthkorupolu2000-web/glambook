import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { OwnerReportResponse } from '../models/owner.model';

const BASE = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class OwnerReportService {
  constructor(private http: HttpClient) {}

  getReport(ownerId: number): Observable<OwnerReportResponse> {
    return this.http.get<OwnerReportResponse>(`${BASE}/owners/${ownerId}/reports`);
  }
}
