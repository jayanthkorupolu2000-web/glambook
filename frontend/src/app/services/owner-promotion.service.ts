import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PromotionResponse } from '../models/owner.model';

const BASE = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class OwnerPromotionService {
  constructor(private http: HttpClient) {}

  getPromotions(ownerId: number, activeOnly = false): Observable<PromotionResponse[]> {
    return this.http.get<PromotionResponse[]>(`${BASE}/owners/${ownerId}/promotions?activeOnly=${activeOnly}`);
  }

  createPromotion(ownerId: number, dto: any): Observable<PromotionResponse> {
    return this.http.post<PromotionResponse>(`${BASE}/owners/${ownerId}/promotions`, dto);
  }
}
