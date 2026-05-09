import { HttpClient } from '@angular/common/http';
import { Component, HostListener, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';

const API = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-prof-consultations',
  templateUrl: './prof-consultations.component.html'
})
export class ProfConsultationsComponent implements OnInit {
  consultations: any[] = [];
  filtered: any[] = [];
  loading = false;
  filter = '';
  error = '';
  success = '';

  replyText: Record<number, string> = {};
  replyOpen: Record<number, boolean> = {};
  submitting: Record<number, boolean> = {};

  // Lightbox
  lightboxUrl = '';
  lightboxOpen = false;

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
      next: data => {
        this.consultations = Array.isArray(data) ? data : [];
        this.applyFilter();
        this.loading = false;
      },
      error: () => { this.consultations = []; this.filtered = []; this.loading = false; }
    });
  }

  applyFilter(): void {
    this.filtered = this.filter
      ? this.consultations.filter(c => c.status === this.filter)
      : this.consultations;
  }

  setFilter(f: string): void { this.filter = f; this.load(); }

  toggleReply(id: number): void { this.replyOpen[id] = !this.replyOpen[id]; }
  isReplyOpen(id: number): boolean { return !!this.replyOpen[id]; }

  reply(c: any): void {
    const text = this.replyText[c.id];
    if (!text || text.trim().length < 10) { this.error = 'Reply must be at least 10 characters.'; return; }
    const id = this.auth.getUserId();
    if (!id) return;
    this.submitting[c.id] = true;
    this.error = '';
    this.success = '';
    this.http.patch(`${API}/professionals/${id}/consultations/${c.id}/reply`,
      { professionalReply: text.trim() }
    ).subscribe({
      next: (updated: any) => {
        const idx = this.consultations.findIndex(x => x.id === c.id);
        if (idx !== -1) this.consultations[idx] = updated;
        this.replyOpen[c.id] = false;
        this.replyText[c.id] = '';
        this.submitting[c.id] = false;
        this.success = 'Reply sent successfully!';
        this.applyFilter();
        setTimeout(() => { this.success = ''; }, 3000);
      },
      error: (e: any) => {
        this.error = e?.error?.message || 'Failed to send reply.';
        this.submitting[c.id] = false;
      }
    });
  }

  // ── Lightbox ──────────────────────────────────────────────────────
  openLightbox(url: string): void {
    this.lightboxUrl = url;
    this.lightboxOpen = true;
  }

  closeLightbox(): void { this.lightboxOpen = false; }

  @HostListener('document:keydown', ['$event'])
  onKey(e: KeyboardEvent): void {
    if (this.lightboxOpen && e.key === 'Escape') this.closeLightbox();
  }

  // ── Helpers ───────────────────────────────────────────────────────
  statusBadge(s: string): string {
    const m: Record<string, string> = { PENDING: 'warning text-dark', RESPONDED: 'success', CLOSED: 'secondary' };
    return `badge bg-${m[s] ?? 'secondary'}`;
  }

  topicIcon(t: string): string {
    const m: Record<string, string> = { HAIR: '💇', SKIN: '✨', MAKEUP: '💄', GENERAL: '💬' };
    return m[t] ?? '💬';
  }

  initials(name: string): string { return (name || '?').charAt(0).toUpperCase(); }

  resolvePhotoUrl(url: string): string {
    if (!url) return '';
    if (url.startsWith('http')) return url;
    return `http://localhost:8080${url}`;
  }
}
