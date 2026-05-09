import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import {
    AuthResponse,
    CustomerRegisterRequest,
    LoginRequest,
    ProfessionalRegisterRequest,
    Role
} from '../models';

const API_BASE = 'http://localhost:8080';
const TOKEN_KEY = 'auth_token';
const ROLE_KEY = 'auth_role';
const USER_ID_KEY = 'auth_user_id';
const USER_NAME_KEY = 'auth_user_name';
const USER_EMAIL_KEY = 'auth_user_email';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private http: HttpClient) {}

  loginCustomer(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/api/auth/customer/login`, credentials)
      .pipe(tap(res => this.storeAuth(res)));
  }

  loginOwner(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/api/auth/owner/login`, credentials)
      .pipe(tap(res => this.storeAuth(res)));
  }

  loginProfessional(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/api/auth/professional/login`, credentials)
      .pipe(tap(res => this.storeAuth(res)));
  }

  loginAdmin(credentials: { username: string; password: string }): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/api/auth/admin/login`, credentials)
      .pipe(tap(res => this.storeAuth(res)));
  }

  registerCustomer(data: CustomerRegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/api/auth/customer/register`, data)
      .pipe(tap(res => this.storeAuth(res)));
  }

  registerProfessional(data: ProfessionalRegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/api/auth/professional/register`, data)
      .pipe(tap(res => this.storeAuth(res)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(USER_ID_KEY);
    localStorage.removeItem(USER_NAME_KEY);
    localStorage.removeItem(USER_EMAIL_KEY);
  }

  getRole(): Role | null {
    return localStorage.getItem(ROLE_KEY) as Role | null;
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getUserId(): number | null {
    const id = localStorage.getItem(USER_ID_KEY);
    return id ? Number(id) : null;
  }

  getUserName(): string {
    return localStorage.getItem(USER_NAME_KEY) || '';
  }

  getUserEmail(): string {
    return localStorage.getItem(USER_EMAIL_KEY) || '';
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  private storeAuth(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(ROLE_KEY, res.role);
    localStorage.setItem(USER_ID_KEY, String(res.userId));
    if (res.name) localStorage.setItem(USER_NAME_KEY, res.name);
    // Extract email from JWT payload
    try {
      const payload = JSON.parse(atob(res.token.split('.')[1]));
      if (payload.sub) localStorage.setItem(USER_EMAIL_KEY, payload.sub);
    } catch {}
  }
}
