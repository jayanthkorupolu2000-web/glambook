import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { OwnerNotificationResponse } from '../../../models/owner.model';
import { OwnerIdService } from '../../../services/owner-id.service';
import { OwnerNotificationService } from '../../../services/owner-notification.service';

@Component({
  selector: 'app-owner-notifications',
  templateUrl: './owner-notifications.component.html',
  styleUrls: ['./owner-notifications.component.scss'],
  encapsulation: ViewEncapsulation.None,
  styles: [`
    app-owner-notifications { display: block; }
    .on-page { min-height:100vh; background:#fdf2f4 !important; padding:0 0 3rem; font-family:'DM Sans','Inter',system-ui,sans-serif; }
    .on-hero { background:linear-gradient(135deg,#0d1b3e 0%,#1a3a6e 60%,#e8476a 100%); padding:2rem 2rem 3.5rem; position:relative; overflow:hidden; }
    .on-hero::after { content:''; position:absolute; right:-60px; top:-60px; width:260px; height:260px; border-radius:50%; background:rgba(232,71,106,0.18); pointer-events:none; }
    .on-hero__inner { max-width:1200px; margin:0 auto; display:flex; align-items:flex-start; justify-content:space-between; gap:1.5rem; flex-wrap:wrap; }
    .on-hero__eyebrow { font-size:0.72rem; font-weight:700; letter-spacing:0.10em; text-transform:uppercase; color:rgba(255,255,255,0.60); margin-bottom:0.3rem; }
    .on-hero__title { font-size:1.85rem; font-weight:800; color:#ffffff; letter-spacing:-0.03em; margin-bottom:0.3rem; line-height:1.15; }
    .on-hero__sub { font-size:0.88rem; color:rgba(255,255,255,0.68); margin:0; }
    .on-hero__actions { display:flex; align-items:center; gap:0.75rem; flex-wrap:wrap; padding-top:0.25rem; }
    .on-hero__chip { display:inline-flex; align-items:center; padding:0.35rem 0.85rem; border-radius:999px; font-size:0.78rem; font-weight:700; border:1.5px solid rgba(253,224,71,0.50); background:rgba(255,255,255,0.15); color:#ffffff; }
    .on-mark-all-btn { display:inline-flex; align-items:center; padding:0.55rem 1.1rem; border-radius:12px; border:1.5px solid rgba(255,255,255,0.35); background:rgba(255,255,255,0.12); color:#ffffff; font-size:0.82rem; font-weight:700; cursor:pointer; white-space:nowrap; }
    .on-mark-all-btn:hover { background:rgba(255,255,255,0.22); border-color:rgba(255,255,255,0.60); }
    .on-summary { max-width:1200px; margin:-2rem auto 0; padding:0 1.5rem; display:flex; gap:1rem; flex-wrap:wrap; position:relative; z-index:2; }
    .on-chip { display:flex; align-items:center; gap:0.5rem; background:#ffffff; border-radius:12px; padding:0.75rem 1.1rem; box-shadow:0 4px 16px rgba(13,27,62,0.12); border-left:4px solid transparent; font-size:0.82rem; font-weight:600; color:#0d1b3e; min-width:130px; transition:transform 0.18s ease,box-shadow 0.18s ease; }
    .on-chip:hover { transform:translateY(-2px); box-shadow:0 8px 24px rgba(13,27,62,0.16); }
    .on-chip__label { color:#64748b; }
    .on-chip__count { margin-left:auto; font-size:1.1rem; font-weight:800; color:#0d1b3e; }
    .on-chip--navy { border-left-color:#0d1b3e; } .on-chip--navy .fa-solid { color:#0d1b3e; }
    .on-chip--amber { border-left-color:#d97706; } .on-chip--amber .fa-solid { color:#d97706; }
    .on-chip--green { border-left-color:#16a34a; } .on-chip--green .fa-solid { color:#16a34a; }
    .on-loading { display:flex; align-items:center; justify-content:center; gap:0.75rem; padding:3rem; color:#64748b; font-weight:600; }
    .on-spinner { width:26px; height:26px; border:3px solid #e2e8f0; border-top-color:#e8476a; border-radius:50%; animation:on-spin 0.7s linear infinite; }
    @keyframes on-spin { to { transform:rotate(360deg); } }
    .on-list-wrap { max-width:1200px; margin:1.5rem auto 0; padding:0 1.5rem; background:#ffffff; border-radius:20px; box-shadow:0 2px 16px rgba(13,27,62,0.08); border:1px solid #f1f5f9; overflow:hidden; }
    .on-list-header { display:flex; align-items:center; gap:0.5rem; padding:1rem 1.5rem; font-size:0.88rem; font-weight:800; color:#0d1b3e; background:rgba(13,27,62,0.03); border-bottom:1px solid #f1f5f9; }
    .on-list-header .fa-solid { color:#e8476a; }
    .on-list-header__count { margin-left:0.35rem; font-size:0.75rem; font-weight:700; padding:0.15rem 0.55rem; border-radius:999px; color:#ffffff; background:#0d1b3e; }
    .on-empty { display:flex; flex-direction:column; align-items:center; gap:0.5rem; padding:3.5rem; color:#94a3b8; font-weight:600; }
    .on-empty i { font-size:2.5rem; }
    .on-item { display:flex; align-items:flex-start; gap:1rem; padding:1rem 1.5rem; border-bottom:1px solid #f1f5f9; transition:background 0.15s ease; }
    .on-item:last-child { border-bottom:none; }
    .on-item--unread { background:rgba(217,119,6,0.04); border-left:3px solid #d97706; }
    .on-item--read { background:#ffffff; border-left:3px solid transparent; }
    .on-item:hover { background:#fdf2f4; }
    .on-item__icon { width:38px; height:38px; border-radius:10px; display:flex; align-items:center; justify-content:center; font-size:1rem; flex-shrink:0; margin-top:0.1rem; }
    .on-item__icon--amber { background:rgba(217,119,6,0.12); color:#d97706; border:1px solid rgba(217,119,6,0.25); }
    .on-item__icon--slate { background:#f1f5f9; color:#94a3b8; border:1px solid #e2e8f0; }
    .on-item__body { flex:1; min-width:0; }
    .on-item__message { font-size:0.88rem; color:#334155; margin:0 0 0.3rem; line-height:1.55; }
    .on-item__message--bold { font-weight:700; color:#0d1b3e; }
    .on-item__time { font-size:0.75rem; color:#94a3b8; display:flex; align-items:center; }
    .on-item__status { flex-shrink:0; display:flex; align-items:center; padding-top:0.15rem; }
    .on-read-badge { display:inline-flex; align-items:center; font-size:0.72rem; font-weight:700; color:#16a34a; background:rgba(22,163,74,0.10); border:1px solid rgba(22,163,74,0.25); padding:0.18rem 0.55rem; border-radius:999px; white-space:nowrap; }
    .on-unread-dot { width:10px; height:10px; border-radius:50%; background:#d97706; box-shadow:0 0 0 3px rgba(217,119,6,0.20); }
  `]
})
export class OwnerNotificationsComponent implements OnInit {
  notifications: OwnerNotificationResponse[] = [];
  loading = false;
  ownerId = 0;

  constructor(private notifService: OwnerNotificationService, private ownerIdService: OwnerIdService) {}

  ngOnInit(): void {
    this.ownerIdService.getOwnerId().subscribe(id => {
      this.ownerId = id;
      this.load();
    });
  }

  load(): void {
    this.loading = true;
    this.notifService.getNotifications(this.ownerId).subscribe({
      next: data => { this.notifications = data; this.loading = false; },
      error: () => this.loading = false
    });
  }

  markAllRead(): void {
    this.notifService.markAllAsRead(this.ownerId).subscribe({
      next: () => this.notifications.forEach(n => n.isRead = true)
    });
  }

  markRead(id: number): void {
    const n = this.notifications.find(x => x.id === id);
    if (!n || n.isRead) return;
    this.notifService.markAsRead(this.ownerId, id).subscribe({
      next: () => { if (n) n.isRead = true; }
    });
  }

  typeIcon(type: string): string {
    const map: Record<string, string> = {
      NEW_BOOKING: '📅', COMPLAINT_FORWARDED: '⚠️',
      PROFESSIONAL_PENDING: '👤', APPOINTMENT_CANCELLED: '❌'
    };
    return map[type] ?? '🔔';
  }

  get unreadCount(): number {
    return this.notifications.filter(n => !n.isRead).length;
  }
}
