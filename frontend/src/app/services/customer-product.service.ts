import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ProductResponse } from '../models/customer.model';

const BASE = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class CustomerProductService {
  constructor(private http: HttpClient) {}

  browse(category?: string, brand?: string, keyword?: string): Observable<ProductResponse[]> {
    let url = `${BASE}/products?`;
    if (category) url += `category=${category}&`;
    if (brand) url += `brand=${brand}&`;
    if (keyword) url += `keyword=${keyword}`;
    return this.http.get<ProductResponse[]>(url);
  }

  getById(id: number): Observable<ProductResponse> {
    return this.http.get<ProductResponse>(`${BASE}/products/${id}`);
  }
}
