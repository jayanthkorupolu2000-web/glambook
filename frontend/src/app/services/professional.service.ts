import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PagedResponse, Professional, Service } from '../models';

const API_BASE = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class ProfessionalService {
  constructor(private http: HttpClient) {}

  getByCity(city: string, page = 0, size = 10): Observable<PagedResponse<Professional>> {
    const params = new HttpParams()
      .set('city', city)
      .set('page', page)
      .set('size', size);
    return this.http.get<PagedResponse<Professional>>(`${API_BASE}/api/professionals`, { params });
  }

  getById(id: number): Observable<Professional> {
    return this.http.get<Professional>(`${API_BASE}/api/professionals/${id}`);
  }

  getServices(professionalId: number): Observable<Service[]> {
    return this.http.get<Service[]>(`${API_BASE}/api/professionals/${professionalId}/services`);
  }
}
