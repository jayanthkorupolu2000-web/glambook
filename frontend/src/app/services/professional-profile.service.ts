import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ProfessionalProfileResponse } from '../models/professional.model';

const BASE = 'http://localhost:8080/api/v1/professionals';

@Injectable({ providedIn: 'root' })
export class ProfessionalProfileService {
  constructor(private http: HttpClient) {}

  updateProfile(id: number, dto: any): Observable<ProfessionalProfileResponse> {
    return this.http.put<ProfessionalProfileResponse>(`${BASE}/${id}/profile`, dto);
  }

  uploadProfilePhoto(id: number, file: File): Observable<{ photoUrl: string }> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<{ photoUrl: string }>(`${BASE}/${id}/profile/photo`, fd);
  }

  getProfile(id: number): Observable<ProfessionalProfileResponse> {
    return this.http.get<ProfessionalProfileResponse>(`${BASE}/${id}/profile`);
  }

  searchProfessionals(params: any): Observable<ProfessionalProfileResponse[]> {
    return this.http.get<ProfessionalProfileResponse[]>(`${BASE}/search`, { params });
  }
}
