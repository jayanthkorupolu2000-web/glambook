import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ResourceResponse } from '../models/owner.model';

const BASE = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class OwnerResourceService {
  constructor(private http: HttpClient) {}

  getResources(ownerId: number): Observable<ResourceResponse[]> {
    return this.http.get<ResourceResponse[]>(`${BASE}/owners/${ownerId}/resources`);
  }

  addResource(ownerId: number, dto: any): Observable<ResourceResponse> {
    return this.http.post<ResourceResponse>(`${BASE}/owners/${ownerId}/resources`, dto);
  }

  addSlot(ownerId: number, resourceId: number, dto: any): Observable<ResourceResponse> {
    return this.http.post<ResourceResponse>(`${BASE}/owners/${ownerId}/resources/${resourceId}/availability`, dto);
  }

  toggleAvailability(ownerId: number, resourceId: number, available: boolean): Observable<ResourceResponse> {
    return this.http.patch<ResourceResponse>(`${BASE}/owners/${ownerId}/resources/${resourceId}/toggle?available=${available}`, {});
  }
}
