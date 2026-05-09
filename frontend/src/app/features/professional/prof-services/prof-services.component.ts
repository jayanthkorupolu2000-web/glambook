import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
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
  styleUrls: ['./prof-services.component.css']
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
    let val = Number(ctrl.value);
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
