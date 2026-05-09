import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ComplaintRequest, ComplaintResponse } from '../models/complaint.model';

const BASE = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class ComplaintService {
  constructor(private http: HttpClient) {}

  submitComplaint(dto: ComplaintRequest): Observable<ComplaintResponse> {
    return this.http.post<ComplaintResponse>(`${BASE}/complaints`, dto);
  }

  getAllComplaints(status?: string): Observable<ComplaintResponse[]> {
    const url = status ? `${BASE}/admin/complaints?status=${status}` : `${BASE}/admin/complaints`;
    return this.http.get<ComplaintResponse[]>(url);
  }

  forwardComplaint(id: number): Observable<ComplaintResponse> {
    return this.http.patch<ComplaintResponse>(`${BASE}/admin/complaints/${id}/forward`, {});
  }

  mediateComplaint(id: number, notes: string): Observable<ComplaintResponse> {
    return this.http.patch<ComplaintResponse>(`${BASE}/admin/complaints/${id}/mediate`, { resolutionNotes: notes });
  }
}
