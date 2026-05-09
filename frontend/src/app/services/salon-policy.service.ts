import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

const BASE = 'http://localhost:8080/api/v1';

export interface SalonPolicyResponse {
  id: number;
  ownerId: number;
  ownerName: string;
  title: string;
  content: string;
  createdAt: string;
}

export interface SalonPolicyRequest {
  title: string;
  content: string;
}

@Injectable({ providedIn: 'root' })
export class SalonPolicyService {
  constructor(private http: HttpClient) {}

  /** Salon Owner: publish a new policy */
  publishPolicy(ownerId: number, dto: SalonPolicyRequest): Observable<SalonPolicyResponse> {
    return this.http.post<SalonPolicyResponse>(`${BASE}/owners/${ownerId}/policies`, dto);
  }

  /** Salon Owner: get all their policies */
  getPoliciesByOwner(ownerId: number): Observable<SalonPolicyResponse[]> {
    return this.http.get<SalonPolicyResponse[]>(`${BASE}/owners/${ownerId}/policies`);
  }

  /** Professional: get all policies for their city */
  getPoliciesByCity(city: string): Observable<SalonPolicyResponse[]> {
    return this.http.get<SalonPolicyResponse[]>(`${BASE}/policies/city/${encodeURIComponent(city)}`);
  }
}
