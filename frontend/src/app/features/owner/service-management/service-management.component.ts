import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

const API_BASE = 'http://localhost:8080';

interface ServiceResponse {
  id: number;
  name: string;
  category: string;
  gender: string;
  price: number;
  durationMins: number;
  isActive?: boolean;
}
interface CategoryGroup { category: string; services: ServiceResponse[]; }
interface GenderGroup { gender: string; categories: CategoryGroup[]; }

@Component({
  selector: 'app-service-management',
  templateUrl: './service-management.component.html'
})
export class ServiceManagementComponent implements OnInit {
  grouped: GenderGroup[] = [];
  loading = false;
  error: string | null = null;
  success = '';
  showForm = false;
  submitting = false;
  form!: FormGroup;

  categories = ['Hair', 'Beard', 'Skin', 'Nails', 'Makeup', 'Body', 'Grooming', 'Packages', 'Special'];
  genders = ['MEN', 'WOMEN', 'KIDS'];
  durations = [15, 30, 45, 60, 90, 120];

  constructor(private http: HttpClient, private fb: FormBuilder) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', Validators.required],
      category: ['', Validators.required],
      gender: ['WOMEN', Validators.required],
      price: [null, [Validators.required, Validators.min(1)]],
      durationMins: [30, Validators.required]
    });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.http.get<Record<string, Record<string, ServiceResponse[]>>>(`${API_BASE}/api/services`).subscribe({
      next: data => {
        this.grouped = Object.entries(data).map(([gender, catMap]) => ({
          gender,
          categories: Object.entries(catMap).map(([category, services]) => ({ category, services }))
        }));
        this.loading = false;
      },
      error: () => { this.error = 'Failed to load services.'; this.loading = false; }
    });
  }

  addService(): void {
    if (this.form.invalid) return;
    this.submitting = true;
    this.error = null;
    this.http.post<ServiceResponse>(`${API_BASE}/api/services`, this.form.value).subscribe({
      next: () => {
        this.success = 'Service added successfully!';
        this.form.reset({ gender: 'WOMEN', durationMins: 30 });
        this.showForm = false;
        this.submitting = false;
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: () => { this.error = 'Failed to add service.'; this.submitting = false; }
    });
  }

  toggleService(id: number, name: string, current: boolean): void {
    this.http.patch(`${API_BASE}/api/services/${id}/toggle`, {}).subscribe({
      next: () => {
        this.success = `"${name}" ${current ? 'deactivated' : 'activated'}!`;
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: () => { this.error = 'Failed to toggle service.'; }
    });
  }

  deleteService(id: number, name: string): void {
    if (!window.confirm(`Delete service "${name}"? This cannot be undone.`)) return;
    this.http.delete(`${API_BASE}/api/services/${id}`).subscribe({
      next: () => { this.load(); this.success = 'Service deleted.'; setTimeout(() => this.success = '', 3000); },
      error: () => { this.error = 'Failed to delete service.'; }
    });
  }

  genderBadgeClass(gender: string): string {
    const map: Record<string, string> = { MEN: 'primary', WOMEN: 'danger', KIDS: 'success' };
    return `badge bg-${map[gender] ?? 'secondary'} fs-6 px-3 py-2`;
  }

  genderLabel(g: string): string {
    const m: Record<string, string> = { MEN: 'Men', WOMEN: 'Women', KIDS: 'Kids' };
    return m[g] || g;
  }
}
