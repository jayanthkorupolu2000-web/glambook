import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CustomerDashboardDTO } from '../models/customer.model';

const BASE = 'http://localhost:8080/api/v1/customers';

@Injectable({ providedIn: 'root' })
export class CustomerDashboardService {
  constructor(private http: HttpClient) {}

  getDashboard(customerId: number): Observable<CustomerDashboardDTO> {
    return this.http.get<CustomerDashboardDTO>(`${BASE}/${customerId}/dashboard`);
  }
}
