import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { City, Customer } from '../../../models';
import { AuthService } from '../../../services/auth.service';
import { VALID_CITIES, cityValidator } from '../../../validators/city.validator';

const API_BASE = 'http://localhost:8080';

@Component({
  selector: 'app-customer-profile',
  templateUrl: './customer-profile.component.html'
})
export class CustomerProfileComponent implements OnInit {
  form!: FormGroup;
  cities: readonly City[] = VALID_CITIES;
  editMode = false;
  loading = false;
  saving = false;
  error = '';
  success = '';
  profile: Customer | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: [{ value: '', disabled: true }],
      phone: ['', [Validators.pattern(/^\d{10}$/)]],
      city: ['', [Validators.required, cityValidator()]],
      emergencyContact: ['', [Validators.pattern(/^\d{10}$/)]],
      medicalNotes: ['']
    });

    // Pre-fill from localStorage immediately so fields aren't blank
    const cachedName = this.authService.getUserName();
    const cachedEmail = this.authService.getUserEmail();
    if (cachedName || cachedEmail) {
      this.form.patchValue({ name: cachedName, email: cachedEmail });
      this.profile = { id: this.authService.getUserId() || 0, name: cachedName, email: cachedEmail, phone: '', city: '' as City };
    }

    this.form.disable();
    this.form.get('email')?.disable();

    this.loadProfile();
  }

  loadProfile(): void {
    const customerId = this.authService.getUserId();
    if (!customerId) return;

    this.loading = true;
    this.http.get<Customer>(`${API_BASE}/api/customers/${customerId}`).subscribe({
      next: (data) => {
        this.profile = data;
        this.patchForm(data);
        this.loading = false;
      },
      error: (err) => {
        // API failed — keep the cached data visible
        this.loading = false;
        console.error('Profile load error:', err);
      }
    });
  }

  private patchForm(data: Customer): void {
    this.form.enable();
    this.form.patchValue({
      name: data.name || '',
      email: data.email || '',
      phone: data.phone || '',
      city: data.city || '',
      emergencyContact: data.emergencyContact || '',
      medicalNotes: data.medicalNotes || ''
    });
    if (!this.editMode) {
      this.form.disable();
    }
    this.form.get('email')?.disable();
  }

  get f() { return this.form.controls; }

  enableEdit(): void {
    this.editMode = true;
    this.form.enable();
    this.form.get('email')?.disable();
    this.success = '';
    this.error = '';
  }

  cancelEdit(): void {
    this.editMode = false;
    if (this.profile) this.patchForm(this.profile);
    this.form.disable();
    this.form.get('email')?.disable();
    this.error = '';
  }

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const customerId = this.authService.getUserId();
    if (!customerId) return;

    this.saving = true;
    this.error = '';
    this.success = '';

    const payload = {
      name: this.form.value.name,
      phone: this.form.value.phone,
      city: this.form.value.city,
      emergencyContact: this.form.value.emergencyContact || null,
      medicalNotes: this.form.value.medicalNotes || null
    };

    this.http.put<Customer>(`${API_BASE}/api/customers/${customerId}`, payload).subscribe({
      next: (updated) => {
        this.profile = updated;
        this.patchForm(updated);
        this.editMode = false;
        this.form.disable();
        this.form.get('email')?.disable();
        this.success = 'Profile updated successfully.';
        this.saving = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to update profile.';
        this.saving = false;
      }
    });
  }
}
