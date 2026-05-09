import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { LoyaltyResponse } from '../models/owner.model';

const BASE = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class OwnerLoyaltyService {
  constructor(private http: HttpClient) {}

  getLoyalty(ownerId: number): Observable<LoyaltyResponse[]> {
    return this.http.get<LoyaltyResponse[]>(`${BASE}/owners/${ownerId}/loyalty`);
  }

  updatePoints(ownerId: number, customerId: number, points: number): Observable<LoyaltyResponse> {
    return this.http.patch<LoyaltyResponse>(`${BASE}/owners/${ownerId}/loyalty/${customerId}`, { points });
  }
}
