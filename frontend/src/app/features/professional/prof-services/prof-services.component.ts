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

@Component({
  selector: 'app-prof-services',
  templateUrl: './prof-services.component.html'
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
  categories: string[] = [];
  durations = [15, 30, 45, 60, 90, 120];

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
        this.categories = [...new Set(this.ownerServices.map((s: any) => s.category as string))];
      },
      error: () => {}
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
      this.success = 'Service added!';
      this.form.reset({ targetGroup: 'WOMEN', durationMins: 30, discountPct: 0 });
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
      this.success = `Service ${currentlyActive ? 'deactivated' : 'activated'}!`;
      this.load();
      setTimeout(() => this.success = '', 3000);
    }).catch(() => { this.error = 'Failed to toggle service.'; });
  }

  onStatusChange(service: any, event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    const wantActive = val === 'active';
    const currentlyActive = service.isActive !== false; // null or true = active
    if (wantActive === currentlyActive) return;
    this.toggle(service.id, currentlyActive);
  }

  deleteService(serviceId: number): void {
    if (!window.confirm('Delete this service?')) return;
    fetch(`http://localhost:8080/api/services/${serviceId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
    }).then(() => {
      this.success = 'Service deleted.';
      this.load();
      setTimeout(() => this.success = '', 3000);
    }).catch(() => { this.error = 'Failed to delete service.'; });
  }

  effectivePrice(price: number, discount: number): number {
    return price * (1 - (discount || 0) / 100);
  }

  targetGroupLabel(tg: string): string {
    const m: Record<string, string> = { MEN: 'Men', WOMEN: 'Women', KIDS: 'Kids' };
    return m[tg] || tg || '—';
  }

  targetGroupBadge(tg: string): string {
    const m: Record<string, string> = { MEN: 'primary', WOMEN: 'danger', KIDS: 'success' };
    return `badge bg-${m[tg] ?? 'secondary'}`;
  }
}
