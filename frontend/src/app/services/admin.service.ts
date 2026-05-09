import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Customer, Professional, SalonOwner } from '../models';
import { CancellationStats, PaymentStats, PolicyRequest, PolicyResponse, UserStatusResponse } from '../models/complaint.model';

export interface AdminUsersResponse {
  customers: Customer[];
  owners: SalonOwner[];
  professionals: Professional[];
  totalCount: number;
}

export interface SalonOwnerEditRequest {
  name: string;
  phone: string;
}

export interface SalonOwnerEditResponse {
  id: number;
  name: string;
  phone: string;
  email: string;
  city: string;
  role: string;
  additionalInfo: string;
}

export interface SalonOwnerManagementEditRequest {
  ownerName: string;
  salonName: string;
  phone: string;
}

export interface SalonOwnerManagementResponse {
  id: number;
  ownerName: string;
  salonName: string;
  city: string;
  email: string;
  phone: string;
}

const V1 = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly baseUrl = 'http://localhost:8080/api/admin';

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<AdminUsersResponse> {
    return this.http.get<AdminUsersResponse>(`${this.baseUrl}/users`);
  }

  getAllOwners(): Observable<SalonOwner[]> {
    return this.http.get<SalonOwner[]>(`${this.baseUrl}/owners`);
  }

  getReports(): Observable<{ totalAppointments: number; totalPayments: number }> {
    return this.http.get<{ totalAppointments: number; totalPayments: number }>(`${this.baseUrl}/reports`);
  }

  editSalonOwner(id: number, dto: SalonOwnerEditRequest): Observable<SalonOwnerEditResponse> {
    return this.http.patch<SalonOwnerEditResponse>(`${V1}/admin/users/${id}/edit`, dto);
  }

  editSalonOwnerDetails(id: number, dto: SalonOwnerManagementEditRequest): Observable<SalonOwnerManagementResponse> {
    return this.http.patch<SalonOwnerManagementResponse>(`${V1}/admin/salon-owners/${id}/edit`, dto);
  }

  // User suspension
  updateUserStatus(id: number, userType: string, status: string): Observable<UserStatusResponse> {
    return this.http.patch<UserStatusResponse>(
      `${V1}/admin/users/${id}/status?userType=${userType}`,
      { status }
    );
  }

  // Policies
  publishPolicy(adminId: number, dto: PolicyRequest): Observable<PolicyResponse> {
    return this.http.post<PolicyResponse>(`${V1}/admin/policy?adminId=${adminId}`, dto);
  }

  getLatestPolicy(): Observable<PolicyResponse> {
    return this.http.get<PolicyResponse>(`${V1}/policy/latest`);
  }

  getAllPolicies(): Observable<PolicyResponse[]> {
    return this.http.get<PolicyResponse[]>(`${V1}/admin/policy`);
  }

  // Analytics
  getComplaintsByCity(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${V1}/admin/analytics/complaints-by-city`);
  }

  getRatingsDistribution(): Observable<Record<number, number>> {
    return this.http.get<Record<number, number>>(`${V1}/admin/analytics/ratings-distribution`);
  }

  getCancellationStats(): Observable<CancellationStats> {
    return this.http.get<CancellationStats>(`${V1}/admin/analytics/cancellations`);
  }

  getPaymentStats(): Observable<PaymentStats> {
    return this.http.get<PaymentStats>(`${V1}/admin/analytics/payments`);
  }
}