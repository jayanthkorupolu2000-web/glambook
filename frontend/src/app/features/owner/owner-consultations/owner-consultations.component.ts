import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

const API = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-owner-consultations',
  templateUrl: './owner-consultations.component.html'
})
export class OwnerConsultationsComponent implements OnInit {
  consultations: any[] = [];
  loading = false;

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.loading = true;
    this.http.get<any[]>(`${API}/owners/${id}/consultations`).subscribe({
      next: data => { this.consultations = Array.isArray(data) ? data : []; this.loading = false; },
      error: () => { this.consultations = []; this.loading = false; }
    });
  }

  statusBadge(s: string): string {
    const m: Record<string, string> = { PENDING: 'warning text-dark', RESPONDED: 'success', CLOSED: 'secondary' };
    return `badge bg-${m[s] ?? 'secondary'}`;
  }
}
