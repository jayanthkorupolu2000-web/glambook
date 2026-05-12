import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { Router } from '@angular/router';
import { ProfessionalAnalyticsResponse } from '../../../models/professional.model';
import { AuthService } from '../../../services/auth.service';
import { ProfessionalAnalyticsService } from '../../../services/professional-analytics.service';
import { ProfessionalNotificationService } from '../../../services/professional-notification.service';

const BASE = 'http://localhost:8080/api/v1';

export interface QuickAction {
  label: string;
  route: string;
  hover: boolean;
  // SVG path(s) for the icon
  svgPath: string;
  svgPath2?: string;   // optional second path for two-tone icons
  color: string;       // icon stroke/fill colour
  bg: string;          // icon background colour
}

@Component({
  selector: 'app-prof-dashboard',
  templateUrl: './prof-dashboard.component.html',
  styleUrls: ['./prof-dashboard.component.css'],
  encapsulation: ViewEncapsulation.None,
  styles: [`
    .pd-page{min-height:100vh;background:#fdf2f4;padding:0 0 3rem;font-family:'DM Sans','Inter',system-ui,sans-serif;}
    .pd-hero{background:linear-gradient(135deg,#0d1b3e 0%,#1a3a6e 60%,#e8476a 100%);padding:2rem 2rem 3.5rem;position:relative;overflow:hidden;}
    .pd-hero::after{content:'';position:absolute;right:-60px;top:-60px;width:260px;height:260px;border-radius:50%;background:rgba(232,71,106,0.18);pointer-events:none;}
    .pd-hero__inner{max-width:1200px;margin:0 auto;display:flex;align-items:center;justify-content:space-between;gap:1.5rem;flex-wrap:wrap;}
    .pd-hero__eyebrow{font-size:0.72rem;font-weight:700;letter-spacing:0.10em;text-transform:uppercase;color:rgba(255,255,255,0.60);margin-bottom:0.3rem;}
    .pd-hero__title{font-size:1.85rem;font-weight:800;color:#ffffff;letter-spacing:-0.03em;margin-bottom:0.3rem;line-height:1.15;}
    .pd-hero__sub{font-size:0.88rem;color:rgba(255,255,255,0.68);margin:0;}
    .pd-hero__badge{display:flex;flex-direction:column;align-items:center;gap:0.35rem;background:rgba(255,255,255,0.12);border:1px solid rgba(255,255,255,0.22);border-radius:16px;padding:0.85rem 1.25rem;color:#ffffff;font-size:0.82rem;font-weight:700;flex-shrink:0;}
    .pd-hero__badge i{font-size:1.5rem;}
    .pd-loading{display:flex;align-items:center;justify-content:center;gap:0.75rem;padding:3rem;color:#64748b;font-weight:600;}
    .pd-spinner{width:26px;height:26px;border:3px solid #e2e8f0;border-top-color:#e8476a;border-radius:50%;animation:pd-spin 0.7s linear infinite;}
    @keyframes pd-spin{to{transform:rotate(360deg);}}
    .pd-kpi-strip{max-width:1200px;margin:-2rem auto 0;padding:0 1.5rem;display:grid;grid-template-columns:repeat(5,1fr);gap:1rem;position:relative;z-index:2;}
    .pd-kpi{background:#ffffff;border-radius:14px;border:1px solid #f1f5f9;box-shadow:0 4px 16px rgba(13,27,62,0.10);padding:1rem 1.1rem;display:flex;align-items:center;gap:0.85rem;border-left:4px solid transparent;transition:transform 0.18s ease,box-shadow 0.18s ease;}
    .pd-kpi:hover{transform:translateY(-2px);box-shadow:0 8px 24px rgba(13,27,62,0.14);}
    .pd-kpi__icon{width:42px;height:42px;border-radius:12px;display:flex;align-items:center;justify-content:center;font-size:1.2rem;flex-shrink:0;}
    .pd-kpi__val{font-size:1.4rem;font-weight:800;line-height:1.1;margin-bottom:0.1rem;}
    .pd-kpi__lbl{font-size:0.70rem;color:#94a3b8;font-weight:500;}
    .pd-kpi--green{border-left-color:#16a34a;} .pd-kpi--green .pd-kpi__icon{background:rgba(22,163,74,0.10);color:#16a34a;} .pd-kpi--green .pd-kpi__val{color:#16a34a;}
    .pd-kpi--amber{border-left-color:#d97706;} .pd-kpi--amber .pd-kpi__icon{background:rgba(217,119,6,0.10);color:#d97706;} .pd-kpi--amber .pd-kpi__val{color:#d97706;}
    .pd-kpi--blue{border-left-color:#1e6fd9;} .pd-kpi--blue .pd-kpi__icon{background:rgba(30,111,217,0.10);color:#1e6fd9;} .pd-kpi--blue .pd-kpi__val{color:#1e6fd9;}
    .pd-kpi--red{border-left-color:#dc2626;} .pd-kpi--red .pd-kpi__icon{background:rgba(220,38,38,0.10);color:#dc2626;} .pd-kpi--red .pd-kpi__val{color:#dc2626;}
    .pd-kpi--coral{border-left-color:#f97316;} .pd-kpi--coral .pd-kpi__icon{background:rgba(249,115,22,0.10);color:#f97316;} .pd-kpi--coral .pd-kpi__val{color:#f97316;}
    .pd-kpi--slate{border-left-color:#64748b;} .pd-kpi--slate .pd-kpi__icon{background:rgba(100,116,139,0.10);color:#64748b;} .pd-kpi--slate .pd-kpi__val{color:#0d1b3e;}
    .pd-suspension-chip{font-size:0.68rem;font-weight:700;color:#b91c1c;background:rgba(220,38,38,0.10);border:1px solid rgba(220,38,38,0.22);padding:0.15rem 0.5rem;border-radius:999px;margin-top:0.25rem;display:inline-flex;align-items:center;}
    .pd-section-label{max-width:1200px;margin:2rem auto 1rem;padding:0 1.5rem;font-size:0.75rem;font-weight:800;letter-spacing:0.10em;text-transform:uppercase;color:#64748b;}
    .pd-action-grid{max-width:1200px;margin:0 auto;padding:0 1.5rem;display:grid;grid-template-columns:repeat(4,1fr);gap:1rem;}
    .pd-action-tile{background:#ffffff;border-radius:18px;border:1px solid #f1f5f9;box-shadow:0 2px 10px rgba(13,27,62,0.07);padding:1.5rem 1rem;display:flex;flex-direction:column;align-items:center;gap:0.75rem;cursor:pointer;transition:transform 0.18s ease,box-shadow 0.18s ease;}
    .pd-action-tile:hover{transform:translateY(-3px);box-shadow:0 8px 24px rgba(13,27,62,0.12);}
    .pd-action-tile__icon{width:56px;height:56px;border-radius:16px;display:flex;align-items:center;justify-content:center;}
    .pd-action-tile__icon svg{width:26px;height:26px;}
    .pd-action-tile__label{font-size:0.88rem;font-weight:700;color:#0d1b3e;text-align:center;}
    @media(max-width:1024px){.pd-kpi-strip{grid-template-columns:repeat(3,1fr);} .pd-action-grid{grid-template-columns:repeat(3,1fr);}}
    @media(max-width:640px){.pd-hero__title{font-size:1.5rem;} .pd-hero__badge{display:none;} .pd-kpi-strip{grid-template-columns:repeat(2,1fr);} .pd-action-grid{grid-template-columns:repeat(2,1fr);}}
  `]
})
export class ProfDashboardComponent implements OnInit {
  analytics: ProfessionalAnalyticsResponse | null = null;
  unreadCount = 0;
  complaintsCount = 0;
  loading = false;
  profId = 0;
  profName = '';

  constructor(
    private analyticsService: ProfessionalAnalyticsService,
    private notifService: ProfessionalNotificationService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.profId   = this.auth.getUserId() || 0;
    this.profName = this.auth.getUserName() || 'Professional';
    this.loading  = true;

    this.analyticsService.getAnalytics(this.profId).subscribe({
      next: data => { this.analytics = data; this.loading = false; },
      error: () => this.loading = false
    });

    this.notifService.getUnreadCount(this.profId).subscribe({
      next: data => this.unreadCount = data.count,
      error: () => {}
    });

    fetch(`${BASE}/admin/complaints?status=OPEN`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
    }).then(r => r.json()).then((data: any[]) => {
      if (Array.isArray(data))
        this.complaintsCount = data.filter(c => c.professionalId === this.profId).length;
    }).catch(() => {});
  }

  // ── Quick actions with SVG icons ─────────────────────────────────
  quickActions: QuickAction[] = [
    {
      label: 'My Profile', route: 'profile', hover: false,
      color: '#2d4a6e', bg: '#eef2f8',
      svgPath: 'M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2',
      svgPath2: 'M12 11a4 4 0 100-8 4 4 0 000 8z'
    },
    {
      label: 'Appointments', route: 'appointments', hover: false,
      color: '#1a9e5c', bg: '#e6f9f0',
      svgPath: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z'
    },
    {
      label: 'Services', route: 'services', hover: false,
      color: '#ad1457', bg: '#fce4ec',
      svgPath: 'M14.121 14.121L19 19m-7-7l7-7m-7 7l-2.879 2.879M12 12L9.121 9.121m0 5.758a3 3 0 10-4.243 4.243 3 3 0 004.243-4.243zm0-5.758a3 3 0 10-4.243-4.243 3 3 0 004.243 4.243z'
    },
    {
      label: 'Portfolio', route: 'portfolio', hover: false,
      color: '#6a1b9a', bg: '#f3e5f5',
      svgPath: 'M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z'
    },
    {
      label: 'Availability', route: 'availability', hover: false,
      color: '#0277bd', bg: '#e1f5fe',
      svgPath: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z'
    },
    {
      label: 'Reviews', route: 'reviews', hover: false,
      color: '#c77c00', bg: '#fff8e1',
      svgPath: 'M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z'
    },
    {
      label: 'Analytics', route: 'analytics', hover: false,
      color: '#1565c0', bg: '#e3f2fd',
      svgPath: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z'
    },
    {
      label: 'Consultations', route: 'consultations', hover: false,
      color: '#2e7d32', bg: '#e8f5e9',
      svgPath: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01'
    }
  ];

  navigate(path: string): void {
    this.router.navigate(['/dashboard/professional/' + path]);
  }

  // ── 12-hr clock format ────────────────────────────────────────────
  formatHour(h: number): string {
    if (h === 0)  return '12 AM';
    if (h < 12)   return `${h} AM`;
    if (h === 12) return '12 PM';
    return `${h - 12} PM`;
  }

  stars(n: number): number[] { return Array.from({ length: 5 }, (_, i) => i + 1); }
}
