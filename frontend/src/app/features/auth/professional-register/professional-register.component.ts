import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { VALID_CITIES, cityValidator } from '../../../validators/city.validator';
import { emailDomainValidator } from '../../../validators/email-domain.validator';
import { fullNameValidator } from '../../../validators/full-name.validator';
import { passwordStrengthValidator } from '../../../validators/password-strength.validator';

const API = 'http://localhost:8080';

@Component({
  selector: 'app-professional-register',
  templateUrl: './professional-register.component.html'
})
export class ProfessionalRegisterComponent implements OnInit {
  form: FormGroup;
  cities = VALID_CITIES;
  services: any[] = [];
  errorMessage = '';
  loading = false;
  loadingServices = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private http: HttpClient
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, fullNameValidator()]],
      email: ['', [Validators.required, Validators.email, emailDomainValidator()]],
      password: ['', [Validators.required, Validators.minLength(8), passwordStrengthValidator()]],
      city: ['', [Validators.required, cityValidator()]],
      serviceId: ['', Validators.required],
      specialization: [''] // auto-filled from selected service
    });
  }

  ngOnInit(): void {
    this.loadServices();
  }

  loadServices(): void {
    this.loadingServices = true;
    this.http.get<any[]>(`${API}/api/services/list`).subscribe({
      next: data => {
        this.services = Array.isArray(data) ? data : [];
        this.loadingServices = false;
      },
      error: () => { this.loadingServices = false; }
    });
  }

  onServiceChange(event: Event): void {
    const id = Number((event.target as HTMLSelectElement).value);
    const svc = this.services.find(s => s.id === id);
    if (svc) {
      this.form.patchValue({ specialization: svc.name });
    }
  }

  get f() { return this.form.controls; }

  get selectedService(): any {
    const id = Number(this.form.value.serviceId);
    return this.services.find(s => s.id === id) || null;
  }

  getCategories(): string[] {
    return [...new Set(this.services.map(s => s.category))];
  }

  getServicesByCategory(cat: string): any[] {
    return this.services.filter(s => s.category === cat);
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.errorMessage = '';

    const svc = this.selectedService;
    const payload = {
      name: this.form.value.name,
      email: this.form.value.email,
      password: this.form.value.password,
      city: this.form.value.city,
      specialization: svc ? svc.name : this.form.value.specialization,
      serviceId: this.form.value.serviceId ? Number(this.form.value.serviceId) : null
    };

    this.auth.registerProfessional(payload as any).subscribe({
      next: () => this.router.navigate(['/dashboard/professional']),
      error: err => {
        this.errorMessage = err?.error?.message || 'Registration failed. Please try again.';
        this.loading = false;
      }
    });
  }
}
