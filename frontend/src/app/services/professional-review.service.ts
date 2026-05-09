import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ReviewWithResponse } from '../models/professional.model';

const BASE = 'http://localhost:8080/api/v1/professionals';

@Injectable({ providedIn: 'root' })
export class ProfessionalReviewService {
  constructor(private http: HttpClient) {}

  getReviews(profId: number): Observable<ReviewWithResponse[]> {
    return this.http.get<ReviewWithResponse[]>(`${BASE}/${profId}/reviews`);
  }

  respondToReview(profId: number, reviewId: number, response: string): Observable<ReviewWithResponse> {
    return this.http.post<ReviewWithResponse>(`${BASE}/${profId}/reviews/${reviewId}/response`, { response });
  }
}
