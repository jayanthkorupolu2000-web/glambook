import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';

const API = 'http://localhost:8080/api/v1';

@Component({
  selector: 'app-beauty-profile',
  templateUrl: './beauty-profile.component.html',
  styleUrls: ['./beauty-profile.component.scss']
})
export class BeautyProfileComponent implements OnInit {
  form!: FormGroup;
  loading = false;
  saving = false;
  success = '';
  error = '';
  editMode = false;

  skinTypes  = ['Normal', 'Oily', 'Dry', 'Combination', 'Sensitive'];
  hairTypes  = ['Straight', 'Wavy', 'Curly', 'Coily'];
  hairTextures = ['Fine', 'Medium', 'Thick'];

  allergyOptions = [
    'Ammonia', 'Parabens', 'Sulfates', 'Fragrances', 'Formaldehyde',
    'PPD (Hair Dye)', 'Latex', 'Nickel', 'Lanolin', 'Alcohol'
  ];

  serviceOptions = [
    'Haircut', 'Hair Color', 'Hair Treatment', 'Blowout',
    'Facial', 'Cleanup', 'Waxing', 'Threading',
    'Manicure', 'Pedicure', 'Makeup', 'Bridal Makeup'
  ];

  selectedAllergies: Set<string> = new Set();
  selectedServices:  Set<string> = new Set();

  constructor(private fb: FormBuilder, private auth: AuthService, private http: HttpClient) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      skinType:    [''],
      hairType:    [''],
      hairTexture: [''],
      notes:       ['']
    });
    this.form.disable();
    this.load();
  }

  load(): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.loading = true;
    this.http.get<any>(`${API}/customers/${id}/beauty-profile`).subscribe({
      next: data => {
        this.form.enable();
        this.form.patchValue({
          skinType:    data.skinType    || '',
          hairType:    data.hairType    || '',
          hairTexture: data.hairTexture || '',
          notes:       data.notes       || ''
        });
        // Parse comma-separated strings back into sets
        this.selectedAllergies = new Set(
          (data.allergies || '').split(',').map((s: string) => s.trim()).filter(Boolean)
        );
        this.selectedServices = new Set(
          (data.preferredServices || '').split(',').map((s: string) => s.trim()).filter(Boolean)
        );
        this.form.disable();
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  enableEdit(): void {
    this.editMode = true;
    this.form.enable();
    this.success = '';
    this.error = '';
  }

  cancel(): void {
    this.editMode = false;
    this.form.disable();
    this.load();
  }

  toggleAllergy(item: string): void {
    if (!this.editMode) return;
    this.selectedAllergies.has(item)
      ? this.selectedAllergies.delete(item)
      : this.selectedAllergies.add(item);
  }

  toggleService(item: string): void {
    if (!this.editMode) return;
    this.selectedServices.has(item)
      ? this.selectedServices.delete(item)
      : this.selectedServices.add(item);
  }

  save(): void {
    const id = this.auth.getUserId();
    if (!id) return;
    this.saving = true;
    this.error = '';
    this.success = '';

    const payload = {
      skinType:          this.form.value.skinType    || null,
      hairType:          this.form.value.hairType    || null,
      hairTexture:       this.form.value.hairTexture || null,
      allergies:         [...this.selectedAllergies].join(', ') || null,
      preferredServices: [...this.selectedServices].join(', ')  || null,
      notes:             this.form.value.notes       || null
    };

    this.http.put<any>(`${API}/customers/${id}/beauty-profile`, payload).subscribe({
      next: () => {
        this.success = '✅ Beauty profile saved successfully!';
        this.editMode = false;
        this.form.disable();
        this.saving = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to save. Please try again.';
        this.saving = false;
      }
    });
  }
}
