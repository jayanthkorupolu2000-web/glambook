import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

const API = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-prof-consultations',
  templateUrl: './prof-consultations.component.html'
})
export class ProfConsultationsComponent implements OnInit {
  consultations: any[] = [];
  loading = false;
  replyText: Record<number, string> = {};
  submitting: Record<number, boolean> = {};
  error = '';
  success = '';
  filter = '';

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.loading = true;
    const url = this.filter
      ? `${API}/professionals/${id}/consultations?status=${this.filter}`
      : `${API}/professionals/${id}/consultations`;
    this.http.get<any[]>(url).subscribe({
      next: data => { this.consultations = Array.isArray(data) ? data : []; this.loading = false; },
      error: () => { this.consultations = []; this.loading = false; }
    });
  }

  reply(c: any): void {
    const notes = this.replyText[c.id];
    if (!notes || notes.trim().length < 10) {
      this.error = 'Reply must be at least 10 characters.';
      return;
    }
    const id = this.auth.getUserId();
    if (!id) return;
    this.submitting[c.id] = true;
    this.error = '';
    this.http.patch(`${API}/professionals/${id}/consultations/${c.id}/reply`, { notes }).subscribe({
      next: () => {
        this.success = 'Reply sent!';
        this.replyText[c.id] = '';
        this.submitting[c.id] = false;
        this.load();
      },
      error: (e) => {
        this.error = e?.error?.message || 'Failed to send reply.';
        this.submitting[c.id] = false;
      }
    });
  }

  statusBadge(s: string): string {
    const m: Record<string, string> = {
      PENDING: 'warning text-dark', RESPONDED: 'success', CLOSED: 'secondary'
    };
    return `badge bg-${m[s] ?? 'secondary'}`;
  }

  topicIcon(t: string): string {
    const m: Record<string, string> = { HAIR: '💇', SKIN: '✨', MAKEUP: '💄', GENERAL: '💬' };
    return m[t] ?? '💬';
  }
}
