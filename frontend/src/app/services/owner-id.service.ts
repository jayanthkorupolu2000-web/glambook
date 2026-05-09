import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

const API = 'http://localhost:8080/api/owners';

@Injectable({ providedIn: 'root' })
export class OwnerIdService {
  private resolvedId: number | null = null;

  constructor(private http: HttpClient) {}

  /**
   * Resolves the real owner ID from the backend JWT.
   * Falls back to localStorage if the API call fails.
   */
  getOwnerId(): Observable<number> {
    // If already resolved in this session, return cached
    if (this.resolvedId) return of(this.resolvedId);

    return this.http.get<any>(`${API}/me`).pipe(
      tap(res => {
        if (res.id) {
          this.resolvedId = res.id;
          localStorage.setItem('auth_user_id', String(res.id));
          if (res.name) localStorage.setItem('auth_user_name', res.name);
          if (res.email) localStorage.setItem('auth_user_email', res.email);
        }
      }),
      map(res => res.id as number),
      catchError(() => {
        const stored = Number(localStorage.getItem('auth_user_id') || '0');
        return of(stored);
      })
    );
  }
}
