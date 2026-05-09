import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SalonPolicyResponse } from '../models/owner.model';

const BASE = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class OwnerSalonPolicyService {
  constructor(private http: HttpClient) {}

  getPolicies(ownerId: number): Observable<SalonPolicyResponse[]> {
    return this.http.get<SalonPolicyResponse[]>(`${BASE}/owners/${ownerId}/policies`);
  }

  getLatest(ownerId: number): Observable<SalonPolicyResponse> {
    return this.http.get<SalonPolicyResponse>(`${BASE}/owners/${ownerId}/policies/latest`);
  }

  publishPolicy(ownerId: number, dto: any): Observable<SalonPolicyResponse> {
    return this.http.post<SalonPolicyResponse>(`${BASE}/owners/${ownerId}/policies`, dto);
  }
}
