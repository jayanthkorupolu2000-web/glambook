import { HttpClient } from '@angular/common/http';
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';

interface ServiceItem {
  id: number;
  name: string;
  category: string;
  gender: string;
  targetGroup: string;
  price: number;
  durationMins: number;
  discountPct: number;
  isActive: boolean;
}

const BASE = 'http://localhost:8080/api/v1/professionals';
const SERVICES_API = 'http://localhost:8080/api/services/list';

// All possible service names grouped by category
const SERVICE_NAMES_BY_CATEGORY: Record<string, string[]> = {
  Hair:     ['Haircut', 'Hair Trim', 'Blow Dry', 'Straightening', 'Keratin Treatment',
             'Hair Spa', 'Hair Coloring', 'Highlights', 'Balayage', 'Ombre', 'Root Touch-Up'],
  Beard:    ['Beard Trim', 'Clean Shave', 'Beard Styling', 'Mustache Trim', 'Beard Color'],
  Skin:     ['Acne Treatment', 'Anti-Aging', 'Brightening', 'Hydration Therapy', 'Skin Polishing'],
  Nails:    ['Manicure', 'Pedicure', 'Gel Nails', 'Acrylic Nails', 'Nail Art', 'Nail Extensions'],
  Makeup:   ['Party Makeup', 'Engagement Makeup', 'Photoshoot Makeup', 'Natural Makeup', 'Bridal Makeup'],
  Body:     ['Swedish Massage', 'Deep Tissue Massage', 'Aromatherapy', 'Hot Stone Massage', 'Body Scrub'],
  Grooming: ['Full Grooming Package', 'Eyebrow Shaping', 'Ear Cleaning', 'Nose Wax'],
  Packages: ['Bridal Package', 'Party Package', 'Grooming Package', 'Spa Package'],
  Special:  ['Hair Loss Treatment', 'Dandruff Treatment', 'Scalp Treatment', 'Chemical Peel',
             'D-Tan Treatment', 'Waxing', 'Threading'],
  Facial:   ['Basic Facial', 'Gold Facial', 'Fruit Facial', 'D-Tan Facial', 'Cleanup', 'Bleach'],
};

@Component({
  selector: 'app-prof-services',
  templateUrl: './prof-services.component.html',
  styleUrls: ['./prof-services.component.css'],
  encapsulation: ViewEncapsulation.None,
  styles: [`
    .ps-page{min-height:100vh;background:#fdf2f4;padding:0 0 3rem;font-family:'DM Sans','Inter',system-ui,sans-serif;}
    .ps-hero{background:linear-gradient(135deg,#0d1b3e 0%,#1a3a6e 60%,#e8476a 100%);padding:2rem 2rem 2rem;position:relative;overflow:hidden;}
    .ps-hero::after{content:'';position:absolute;right:-60px;top:-60px;width:260px;height:260px;border-radius:50%;background:rgba(232,71,106,0.18);pointer-events:none;}
    .ps-hero__inner{max-width:1200px;margin:0 auto;display:flex;align-items:center;justify-content:space-between;gap:1.5rem;}
    .ps-hero__eyebrow{font-size:0.72rem;font-weight:700;letter-spacing:0.10em;text-transform:uppercase;color:rgba(255,255,255,0.60);margin-bottom:0.3rem;}
    .ps-hero__title{font-size:1.85rem;font-weight:800;color:#ffffff;letter-spacing:-0.03em;margin-bottom:0.3rem;line-height:1.15;}
    .ps-hero__sub{font-size:0.88rem;color:rgba(255,255,255,0.68);margin:0;}
    .ps-add-btn{display:inline-flex;align-items:center;padding:0.65rem 1.35rem;border-radius:12px;border:1.5px solid rgba(255,255,255,0.35);background:rgba(255,255,255,0.15);color:#ffffff;font-size:0.88rem;font-weight:700;cursor:pointer;white-space:nowrap;flex-shrink:0;transition:background 0.18s ease;}
    .ps-add-btn:hover{background:rgba(255,255,255,0.25);}
    .ps-add-btn--cancel{background:rgba(220,38,38,0.25);border-color:rgba(252,165,165,0.50);}
    .ps-alerts{max-width:1200px;margin:0.75rem auto 0;padding:0 1.5rem;display:flex;flex-direction:column;gap:0.4rem;}
    .ps-alert{padding:0.85rem 1.2rem;border-radius:10px;font-size:0.90rem;font-weight:700;display:flex;align-items:center;gap:0.6rem;}
    .ps-alert--success{background:#f0fdf4;border:1px solid #bbf7d0;border-left:4px solid #22c55e;color:#15803d;}
    .ps-alert--warning{background:#fffbeb;border:1px solid #fde68a;border-left:4px solid #f59e0b;color:#92400e;}
    .ps-alert--danger{background:#fff1f2;border:1px solid #fecdd3;border-left:4px solid #f43f5e;color:#be123c;}
    .ps-form-card{max-width:1200px;margin:0.75rem auto 0;padding:0 1.5rem;background:#ffffff;border-radius:20px;box-shadow:0 2px 16px rgba(13,27,62,0.08);border:1px solid #f1f5f9;overflow:hidden;}
    .ps-form-card__header{display:flex;align-items:center;padding:1rem 1.5rem;font-size:0.92rem;font-weight:800;color:#e8476a;background:rgba(232,71,106,0.05);border-bottom:1px solid #f1f5f9;}
    .ps-form-card__header .fa-solid{color:#e8476a;}
    .ps-form-card__body{padding:1.5rem;}
    .ps-quick-pick{background:#f8fafc;border:1px solid #f1f5f9;border-radius:12px;padding:1rem;margin-bottom:1.25rem;}
    .ps-quick-pick__label{font-size:0.80rem;font-weight:700;color:#334155;margin-bottom:0.5rem;display:flex;align-items:center;}
    .ps-quick-pick__label .fa-solid{color:#d97706;}
    .ps-quick-pick__hint{font-size:0.75rem;color:#94a3b8;margin-top:0.35rem;display:block;}
    .ps-form-card__body .form-label{font-size:0.80rem;font-weight:700;color:#334155;margin-bottom:0.35rem;display:block;}
    .ps-form-card__body .form-control,.ps-form-card__body .form-select{background:#f8fafc;border:1.5px solid #c8d3e8;border-radius:9px;font-size:0.88rem;color:#1e2340;padding:0.55rem 0.85rem;width:100%;transition:border-color 0.2s,box-shadow 0.2s,background 0.2s;box-shadow:inset 0 1px 3px rgba(45,74,110,0.06);}
    .ps-form-card__body .form-control:hover,.ps-form-card__body .form-select:hover{border-color:#8fa8cc;background:#fff;}
    .ps-form-card__body .form-control:focus,.ps-form-card__body .form-select:focus{border-color:#1a3a6e;background:#fff;box-shadow:0 0 0 3px rgba(26,58,110,0.13);outline:none;}
    .ps-form-card__body .form-control::placeholder{color:#a0aec0;font-style:italic;}
    .ps-form-card__body .form-select{background-image:url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='7'%3E%3Cpath d='M0 0l6 7 6-7z' fill='%231a3a6e'/%3E%3C/svg%3E");background-repeat:no-repeat;background-position:right 0.85rem center;padding-right:2.2rem;appearance:none;-webkit-appearance:none;}
    .ps-form-card__body .form-select:disabled,.ps-form-card__body .form-control:disabled{background:#edf0f7;border-color:#dde3ef;color:#94a3b8;cursor:not-allowed;}
    .ps-quick-pick .form-select{border:1.5px solid #f4b8c8;background:#fff;background-image:url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='7'%3E%3Cpath d='M0 0l6 7 6-7z' fill='%23e8476a'/%3E%3C/svg%3E");background-repeat:no-repeat;background-position:right 0.85rem center;padding-right:2.2rem;appearance:none;-webkit-appearance:none;}
    .ps-quick-pick .form-select:focus{border-color:#e8476a;box-shadow:0 0 0 3px rgba(232,71,106,0.13);outline:none;}
    .ps-submit-btn{display:inline-flex;align-items:center;padding:0.68rem 1.5rem;border-radius:12px;border:none;font-size:0.92rem;font-weight:700;color:#ffffff;cursor:pointer;background:#e8476a;box-shadow:0 4px 14px rgba(232,71,106,0.38);}
    .ps-submit-btn:hover:not(:disabled){background:#be185d;}
    .ps-submit-btn:disabled{opacity:0.45;cursor:not-allowed;}
    .ps-loading{display:flex;align-items:center;justify-content:center;gap:0.75rem;padding:3rem;color:#64748b;font-weight:600;}
    .ps-spinner{width:26px;height:26px;border:3px solid #e2e8f0;border-top-color:#e8476a;border-radius:50%;animation:ps-spin 0.7s linear infinite;}
    @keyframes ps-spin{to{transform:rotate(360deg);}}
    .ps-table-wrap{max-width:1200px;margin:0.75rem auto 0;padding:0 1.5rem;position:relative;z-index:1;background:#ffffff;border-radius:20px;box-shadow:0 4px 20px rgba(13,27,62,0.10);border:1px solid #f1f5f9;overflow:hidden;}
    .ps-table-header{display:flex;align-items:center;gap:0.5rem;padding:1rem 1.5rem;font-size:0.88rem;font-weight:800;color:#e8476a;background:rgba(232,71,106,0.05);border-bottom:1px solid #f1f5f9;}
    .ps-table-header .fa-solid{color:#e8476a;}
    .ps-table-header__count{margin-left:0.35rem;font-size:0.75rem;font-weight:700;padding:0.15rem 0.55rem;border-radius:999px;color:#ffffff;background:#e8476a;}
    .ps-table{width:100%;border-collapse:collapse;font-family:'DM Sans','Inter',system-ui,sans-serif;font-size:0.875rem;}
    .ps-table thead tr{background:#f8fafc;border-bottom:2px solid #e2e8f0;}
    .ps-table thead th{padding:0.75rem 1rem;font-size:0.70rem;font-weight:800;text-transform:uppercase;letter-spacing:0.07em;color:#64748b;white-space:nowrap;}
    .ps-row{border-bottom:1px solid #f1f5f9;transition:background 0.15s ease;}
    .ps-row td{padding:0.85rem 1rem;vertical-align:middle;}
    .ps-row:hover{background:#fdf2f4;}
    .ps-row:last-child{border-bottom:none;}
    .ps-cell--name{font-weight:700;color:#0d1b3e;}
    .ps-cell--meta{color:#64748b;font-size:0.85rem;}
    .ps-cell--price{font-weight:700;color:#334155;}
    .ps-cell--effective{font-weight:800;color:#16a34a;}
    .ps-tg-pill{display:inline-flex;align-items:center;font-size:0.72rem;font-weight:800;padding:0.22rem 0.65rem;border-radius:999px;}
    .ps-status-select{appearance:none;-webkit-appearance:none;padding:0.32rem 1.8rem 0.32rem 0.75rem;border-radius:999px;font-size:0.75rem;font-weight:700;cursor:pointer;outline:none;border:1.5px solid transparent;background-repeat:no-repeat;background-position:right 0.5rem center;background-size:0.65rem;}
    .ps-status-select--available{background-color:rgba(22,163,74,0.10);color:#15803d;border-color:rgba(22,163,74,0.30);background-image:url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M0 0l5 6 5-6z' fill='%2315803d'/%3E%3C/svg%3E");}
    .ps-status-select--unavailable{background-color:rgba(220,38,38,0.08);color:#b91c1c;border-color:rgba(220,38,38,0.25);background-image:url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M0 0l5 6 5-6z' fill='%23b91c1c'/%3E%3C/svg%3E");}
    .ps-status-select:focus{box-shadow:0 0 0 3px rgba(13,148,136,0.18);}
    .ps-filter-select{appearance:none;-webkit-appearance:none;padding:0.3rem 2rem 0.3rem 0.7rem;border-radius:8px;font-size:0.78rem;font-weight:600;cursor:pointer;outline:none;border:1.5px solid #e2e8f0;background:#fff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M0 0l5 6 5-6z' fill='%2364748b'/%3E%3C/svg%3E") no-repeat right 0.55rem center;color:#334155;transition:border-color 0.2s;}
    .ps-filter-select:focus{border-color:#e8476a;box-shadow:0 0 0 3px rgba(232,71,106,0.12);}
    .ps-delete-btn{display:inline-flex;align-items:center;justify-content:center;width:30px;height:30px;border-radius:8px;border:1px solid rgba(220,38,38,0.25);background:rgba(220,38,38,0.08);color:#dc2626;font-size:0.75rem;cursor:pointer;}
    .ps-delete-btn:hover{background:rgba(220,38,38,0.18);}
    .ps-empty{text-align:center;padding:3rem !important;color:#94a3b8;font-weight:600;}
    .ps-empty i{font-size:2rem;display:block;margin-bottom:0.5rem;}
    @media(max-width:768px){.ps-hero__title{font-size:1.5rem;} .ps-table-wrap{margin:0.75rem 0.75rem 0;border-radius:14px;padding:0;}}
    @media(max-width:520px){.ps-hero__inner{flex-direction:column;align-items:flex-start;}}
  `]
})
export class ProfServicesComponent implements OnInit {
  services: ServiceItem[] = [];
  ownerServices: any[] = [];
  form: FormGroup;
  loading = false;
  submitting = false;
  showForm = false;
  profId = 0;
  error = '';
  success = '';
  successType: 'success' | 'warning' | 'danger' = 'success';
  categories: string[] = [];
  durations = [15, 30, 45, 60, 90, 120];
  availabilityFilter: 'ALL' | 'available' | 'unavailable' = 'ALL';

  get filteredServices(): ServiceItem[] {
    if (this.availabilityFilter === 'ALL') return this.services;
    if (this.availabilityFilter === 'available') return this.services.filter(s => s.isActive !== false);
    return this.services.filter(s => s.isActive === false);
  }

  // Service name dropdown options based on selected category
  serviceNameOptions: string[] = [];
  allCategories = Object.keys(SERVICE_NAMES_BY_CATEGORY);

  constructor(private fb: FormBuilder, private auth: AuthService, private http: HttpClient) {
    this.form = this.fb.group({
      ownerServiceId: [''],
      name: ['', Validators.required],
      category: ['', Validators.required],
      targetGroup: ['WOMEN', Validators.required],
      price: [null, [Validators.required, Validators.min(1)]],
      durationMins: [30, Validators.required],
      discountPct: [0, [Validators.min(0), Validators.max(100)]]
    });

    // Update service name options when category changes
    this.form.get('category')!.valueChanges.subscribe(cat => {
      this.serviceNameOptions = SERVICE_NAMES_BY_CATEGORY[cat] ?? [];
      // Reset name if it's not in the new list
      const currentName = this.form.get('name')!.value;
      if (currentName && !this.serviceNameOptions.includes(currentName)) {
        this.form.patchValue({ name: '' }, { emitEvent: false });
      }
    });
  }

  ngOnInit(): void {
    this.profId = this.auth.getUserId() || 0;
    this.load();
    this.loadOwnerServices();
  }

  load(): void {
    this.loading = true;
    fetch(`${BASE}/${this.profId}/services?activeOnly=false`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
    }).then(r => r.json()).then(data => {
      const raw = Array.isArray(data) ? data : [];
      this.services = raw.map((s: any) => ({ ...s, targetGroup: s.targetGroup || s.gender || '' }));
      this.loading = false;
    }).catch(() => { this.loading = false; });
  }

  loadOwnerServices(): void {
    this.http.get<any[]>(SERVICES_API).subscribe({
      next: data => {
        this.ownerServices = Array.isArray(data) ? data : [];
        // Merge owner categories with our predefined ones
        const ownerCats = [...new Set(this.ownerServices.map((s: any) => s.category as string))];
        this.categories = [...new Set([...this.allCategories, ...ownerCats])];
      },
      error: () => { this.categories = this.allCategories; }
    });
  }

  onOwnerServiceSelect(event: Event): void {
    const id = Number((event.target as HTMLSelectElement).value);
    const svc = this.ownerServices.find(s => s.id === id);
    if (svc) {
      this.form.patchValue({
        name: svc.name,
        category: svc.category,
        targetGroup: svc.gender || 'WOMEN',
        price: svc.price,
        durationMins: svc.durationMins || 30
      });
    }
  }

  getServicesByCategory(cat: string): any[] {
    return this.ownerServices.filter(s => s.category === cat);
  }

  // ── Discount clamp ───────────────────────────────────────────────────────
  clampDiscount(): void {
    const ctrl = this.form.get('discountPct')!;
    let val = parseFloat(Number(ctrl.value).toFixed(2));
    if (isNaN(val) || val < 0) val = 0;
    if (val > 100) val = 100;
    ctrl.setValue(val, { emitEvent: false });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.submitting = true;
    this.error = '';
    const v = this.form.value;
    const body = {
      name: v.name, category: v.category, targetGroup: v.targetGroup,
      price: v.price, durationMins: v.durationMins,
      discountPct: v.discountPct || 0, professionalId: this.profId
    };
    fetch(`${BASE}/${this.profId}/services`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${localStorage.getItem('auth_token')}` },
      body: JSON.stringify(body)
    }).then(() => {
      this.showAlert('Service added successfully!', 'success');
      this.form.reset({ targetGroup: 'WOMEN', durationMins: 30, discountPct: 0 });
      this.serviceNameOptions = [];
      this.showForm = false;
      this.load();
      this.submitting = false;
    }).catch(() => { this.error = 'Failed to add service.'; this.submitting = false; });
  }

  toggle(serviceId: number, currentlyActive: boolean): void {
    fetch(`${BASE}/${this.profId}/services/${serviceId}/toggle`, {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
    }).then(() => {
      // Deactivation → warning orange; activation → success green
      if (currentlyActive) {
        this.showAlert('Service deactivated.', 'warning');
      } else {
        this.showAlert('Service activated!', 'success');
      }
      this.load();
    }).catch(() => { this.error = 'Failed to toggle service.'; });
  }

  onStatusChange(service: any, event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    const wantActive = val === 'active';
    const currentlyActive = service.isActive !== false;
    if (wantActive === currentlyActive) return;
    this.toggle(service.id, currentlyActive);
  }

  deleteService(serviceId: number): void {
    if (!window.confirm('Delete this service? This cannot be undone.')) return;
    // Use the professional-scoped delete endpoint
    fetch(`${BASE}/${this.profId}/services/${serviceId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
    }).then(res => {
      if (res.ok || res.status === 204) {
        this.showAlert('Service deleted.', 'danger');
        this.load();
      } else {
        this.error = `Failed to delete service (${res.status}).`;
      }
    }).catch(() => { this.error = 'Failed to delete service.'; });
  }

  private showAlert(msg: string, type: 'success' | 'warning' | 'danger'): void {
    this.success = msg;
    this.successType = type;
    setTimeout(() => this.success = '', 3500);
  }

  effectivePrice(price: number, discount: number): number {
    return price * (1 - (discount || 0) / 100);
  }

  targetGroupLabel(tg: string): string {
    const m: Record<string, string> = { MEN: 'Men', WOMEN: 'Women', KIDS: 'Kids' };
    return m[tg] || tg || '—';
  }

  targetGroupColor(tg: string): string {
    const m: Record<string, string> = { MEN: '#1565c0', WOMEN: '#ad1457', KIDS: '#2e7d32' };
    return m[tg] ?? '#555';
  }

  targetGroupBg(tg: string): string {
    const m: Record<string, string> = { MEN: '#e3f2fd', WOMEN: '#fce4ec', KIDS: '#e8f5e9' };
    return m[tg] ?? '#f0f0f0';
  }
}
